import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class App {
    private static final String BASE_URL = "http://www.papercdcase.com/";
    private static final Path PROJECT_ROOT = Paths.get("").toAbsolutePath().normalize();
    private static final Path DATA_FILE = PROJECT_ROOT.resolve("data/data.txt");
    private static final Path RESULT_DIR = PROJECT_ROOT.resolve("result");
    private static final Path RESULT_PDF = RESULT_DIR.resolve("cd.pdf");

    public static void main(String[] args) throws Exception {
        List<String> lines = Files.readAllLines(DATA_FILE);
        if (lines.size() < 2) {
            throw new IllegalArgumentException("data.txt must contain artist, title, and optional tracks");
        }

        String artist = lines.get(0).trim();
        String title = lines.get(1).trim();
        List<String> tracks = lines.subList(2, lines.size()).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .limit(16)
                .collect(Collectors.toList());

        Files.createDirectories(RESULT_DIR);
        clearOldDownloads();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--allow-insecure-localhost");
        options.setExperimentalOption("prefs", downloadPreferences());

        WebDriver webDriver = new ChromeDriver(options);
        try {
            webDriver.get(BASE_URL);
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(20));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.name("artist")));

            webDriver.findElement(By.name("artist")).sendKeys(artist);
            webDriver.findElement(By.name("title")).sendKeys(title);

            for (int i = 0; i < tracks.size(); i++) {
                webDriver.findElement(By.name("track" + (i + 1))).sendKeys(tracks.get(i));
            }

            webDriver.findElement(By.xpath("//input[@name='template' and @value='jewel']")).click();
            webDriver.findElement(By.xpath("//input[@name='size' and @value='a4']")).click();
            webDriver.findElement(By.name("force_saveas")).click();

            WebElement btn = webDriver.findElement(By.xpath("//input[@name='submit']"));
            btn.submit();

            Path downloadedPdf = waitForDownload(Duration.ofSeconds(60));
            Files.move(downloadedPdf, RESULT_PDF, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Saved: " + RESULT_PDF);
        } finally {
            webDriver.quit();
        }
    }

    private static Map<String, Object> downloadPreferences() {
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", RESULT_DIR.toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("safebrowsing.enabled", true);
        prefs.put("plugins.always_open_pdf_externally", true);
        return prefs;
    }

    private static void clearOldDownloads() throws IOException {
        if (!Files.isDirectory(RESULT_DIR)) {
            return;
        }
        try (Stream<Path> files = Files.list(RESULT_DIR)) {
            files.filter(path -> path.toString().endsWith(".pdf"))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    private static Path waitForDownload(Duration timeout) throws InterruptedException, IOException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try (Stream<Path> files = Files.list(RESULT_DIR)) {
                List<Path> pdfs = files
                        .filter(path -> {
                            String name = path.getFileName().toString().toLowerCase();
                            return name.endsWith(".pdf") && !name.endsWith(".crdownload");
                        })
                        .collect(Collectors.toList());

                if (!pdfs.isEmpty()) {
                    Path pdf = pdfs.get(0);
                    if (isDownloadComplete(pdf)) {
                        return pdf;
                    }
                }
            }
            Thread.sleep(500);
        }
        throw new IOException("PDF download did not complete in time");
    }

    private static boolean isDownloadComplete(Path pdf) throws InterruptedException, IOException {
        long size1 = Files.size(pdf);
        Thread.sleep(300);
        long size2 = Files.size(pdf);
        return size1 > 0 && size1 == size2;
    }
}
