import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class App {
    private static final String BASE_URL = "http://www.papercdcase.com/";
    private static final Path DATA_FILE = Path.of("data", "data.txt");
    private static final Path RESULT_DIR = Path.of("result");
    private static final Path RESULT_FILE = RESULT_DIR.resolve("cd.pdf");

    public static void main(String[] args) throws Exception {
        CdData data = CdData.read(DATA_FILE);
        Files.createDirectories(RESULT_DIR);
        Files.deleteIfExists(RESULT_FILE);

        System.setProperty("webdriver.chrome.driver", 
            "C:\\Users\\romap\\Downloads\\chromedriver-win64\\chromedriver.exe");

        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--remote-allow-origins=*");

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", RESULT_DIR.toAbsolutePath().toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        prefs.put("safebrowsing.enabled", true);
        options.setExperimentalOption("prefs", prefs);

        WebDriver webDriver = new ChromeDriver(options);
        try {
            fillFormAndSubmit(webDriver, data);
            waitForPdfDownload();
            System.out.println("ГОТОВО! PDF сохранён в " + RESULT_FILE.toAbsolutePath());
        } finally {
            webDriver.quit();
        }
    }

    private static void fillFormAndSubmit(WebDriver webDriver, CdData data) {
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(30));
        webDriver.get(BASE_URL);

        WebElement artist = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("artist")));
        WebElement title = webDriver.findElement(By.name("title"));

        artist.clear();
        artist.sendKeys(data.artist());
        title.clear();
        title.sendKeys(data.title());

        for (int i = 0; i < data.tracks().size(); i++) {
            WebElement track = webDriver.findElement(By.name("track" + (i + 1)));
            track.clear();
            track.sendKeys(data.tracks().get(i));
        }

        WebElement jewelCase = webDriver.findElement(By.cssSelector("input[name='template'][value='jewel']"));
        WebElement a4 = webDriver.findElement(By.cssSelector("input[name='size'][value='a4']"));
        if (!jewelCase.isSelected()) jewelCase.click();
        if (!a4.isSelected()) a4.click();

        WebElement btn = webDriver.findElement(By.name("submit"));
        btn.submit();
    }

    private static void waitForPdfDownload() throws IOException, InterruptedException {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(60).toMillis();
        while (System.currentTimeMillis() < deadline) {
            Path downloaded = findDownloadedPdf();
            if (downloaded != null) {
                Files.move(downloaded, RESULT_FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return;
            }
            Thread.sleep(500);
        }
        throw new IOException("PDF не скачался в " + RESULT_DIR.toAbsolutePath());
    }

    private static Path findDownloadedPdf() throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(RESULT_DIR)) {
            for (Path file : stream) {
                String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
                if (name.endsWith(".pdf") && !name.endsWith(".crdownload")) {
                    return file;
                }
            }
        }
        return null;
    }

    private record CdData(String artist, String title, List<String> tracks) {
        private static CdData read(Path path) throws IOException {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            String artist = "";
            String title = "";
            List<String> tracks = new ArrayList<>();
            boolean readingTracks = false;

            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty()) continue;

                String lower = line.toLowerCase(Locale.ROOT);
                if (lower.startsWith("artist:")) {
                    artist = line.substring("artist:".length()).trim();
                    readingTracks = false;
                } else if (lower.startsWith("title:")) {
                    title = line.substring("title:".length()).trim();
                    readingTracks = false;
                } else if (lower.startsWith("tracks:")) {
                    readingTracks = true;
                } else if (readingTracks) {
                    tracks.add(line.replaceFirst("^\\d+[.)]\\s*", ""));
                }
            }

            if (artist.isBlank() || title.isBlank() || tracks.isEmpty()) {
                throw new IOException("data/data.txt must contain Artist, Title and at least one Track");
            }
            if (tracks.size() > 18) {
                throw new IOException("The task allows no more than 18 tracks");
            }
            return new CdData(artist, title, tracks);
        }
    }
}
