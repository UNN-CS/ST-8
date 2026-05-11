import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class App {
    private static final String BASE_URL = "http://www.papercdcase.com";
    private static final Path DATA_FILE = Path.of("data", "data.txt");
    private static final Path RESULT_DIR = Path.of("result").toAbsolutePath();
    private static final Path RESULT_FILE = RESULT_DIR.resolve("cd.pdf");
    private static final Path TEMP_RESULT_FILE = RESULT_DIR.resolve("cd-new.pdf");
    private static final Path LOCAL_DRIVER = Path.of("drivers", "chromedriver-win64", "chromedriver.exe");

    public static void main(String[] args) throws IOException {
        CdData data = readData(DATA_FILE);
        Files.createDirectories(RESULT_DIR);
        Files.deleteIfExists(TEMP_RESULT_FILE);

        if (Files.exists(LOCAL_DRIVER)) {
            System.setProperty("webdriver.chrome.driver", LOCAL_DRIVER.toAbsolutePath().toString());
        }

        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--disable-popup-blocking");

        if (Boolean.getBoolean("headless")) {
            options.addArguments("--headless=new");
        }

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", RESULT_DIR.toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);

        WebDriver webDriver = new ChromeDriver(options);
        try {
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(30));
            webDriver.get(BASE_URL);

            wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("artist"))).sendKeys(data.artist());
            webDriver.findElement(By.name("title")).sendKeys(data.title());

            for (int i = 0; i < data.tracks().size(); i++) {
                webDriver.findElement(By.name("track" + (i + 1))).sendKeys(data.tracks().get(i));
            }

            webDriver.findElement(By.cssSelector("input[name='template'][value='jewel']")).click();
            webDriver.findElement(By.cssSelector("input[name='size'][value='a4']")).click();

            WebElement btn = webDriver.findElement(By.name("submit"));
            btn.submit();

            waitForPdf();
            System.out.println("PDF saved to " + RESULT_FILE);
        } finally {
            webDriver.quit();
        }
    }

    private static CdData readData(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());

        if (lines.size() < 3) {
            throw new IllegalArgumentException("data/data.txt must contain artist, album title and at least one track.");
        }

        List<String> tracks = lines.subList(2, Math.min(lines.size(), 18));
        if (tracks.size() > 16) {
            tracks = tracks.subList(0, 16);
        }
        return new CdData(lines.get(0), lines.get(1), tracks);
    }

    private static void waitForPdf() {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(60).toMillis();
        Path downloaded = RESULT_DIR.resolve("papercdcase.pdf");

        while (System.currentTimeMillis() < deadline) {
            if (Files.exists(downloaded) && !Files.exists(RESULT_DIR.resolve("papercdcase.pdf.crdownload"))) {
                try {
                    Files.move(downloaded, TEMP_RESULT_FILE, StandardCopyOption.REPLACE_EXISTING);
                    replaceResultFile();
                    return;
                } catch (IOException e) {
                    throw new IllegalStateException(
                            "Could not save PDF to result/cd.pdf. Close the old PDF file if it is open and run again.",
                            e);
                }
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for PDF download.", e);
            }
        }

        throw new IllegalStateException("PDF was not downloaded to " + RESULT_DIR + " within 60 seconds.");
    }

    private static void replaceResultFile() throws IOException {
        try {
            Files.move(TEMP_RESULT_FILE, RESULT_FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IOException("result/cd.pdf is busy. Close it in the PDF viewer or browser.", e);
        }
    }

    private record CdData(String artist, String title, List<String> tracks) {
    }
}
