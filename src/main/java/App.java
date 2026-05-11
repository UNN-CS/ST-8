import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

public class App {
    private static final List<String> BASE_URLS = List.of(
            "http://www.papercdcase.com",
            "https://www.papercdcase.com"
    );
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(90);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(300);
    private static final int MAX_TRACKS = 16;
    private static final int MAX_RUN_ATTEMPTS = 3;

    public static void main(String[] args) throws IOException, InterruptedException {
        Path projectRoot = Paths.get("").toAbsolutePath();
        Path dataFile = projectRoot.resolve("data").resolve("data.txt");
        Path resultDir = projectRoot.resolve("result");
        Path finalPdf = resultDir.resolve("cd.pdf");

        CoverData coverData = readCoverData(dataFile);
        Files.createDirectories(resultDir);
        Files.deleteIfExists(finalPdf);
        deleteOldPapercdcasePdfs(resultDir);

        Exception lastError = null;
        for (int attempt = 1; attempt <= MAX_RUN_ATTEMPTS; attempt++) {
            WebDriver driver = new ChromeDriver(createChromeOptions(resultDir));
            try {
                driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(120));
                WebDriverWait wait = new WebDriverWait(driver, WAIT_TIMEOUT, POLL_INTERVAL);
                openCasePage(driver, wait);

                fillInput(wait, "artist", coverData.artist());
                fillInput(wait, "title", coverData.title());

                for (int i = 0; i < coverData.tracks().size() && i < MAX_TRACKS; i++) {
                    fillInput(wait, "track" + (i + 1), coverData.tracks().get(i));
                }

                trySelectJewelCaseAndA4(wait);
                tryEnableForceSaveAs(wait);

                WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//input[@type='submit' or @value='Create CD Case' or @name='submit']")));
                clickSubmit(driver, submitButton);

                Path downloadedPdf = waitForDownloadedPdf(resultDir);
                Files.move(downloadedPdf, finalPdf);

                System.out.println("PDF saved to: " + finalPdf.toAbsolutePath());
                return;
            } catch (Exception exception) {
                lastError = exception;
            } finally {
                driver.quit();
            }
        }

        throw new IllegalStateException("Failed to generate cd.pdf from papercdcase", lastError);
    }

    private static CoverData readCoverData(Path dataFile) throws IOException {
        List<String> rawLines = Files.readAllLines(dataFile);
        List<String> lines = rawLines.stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();

        if (lines.size() < 3) {
            throw new IllegalArgumentException("data.txt must contain artist, title and at least one track");
        }

        String artist = lines.get(0);
        String title = lines.get(1);
        List<String> tracks = lines.subList(2, Math.min(lines.size(), MAX_TRACKS + 2));
        return new CoverData(artist, title, tracks);
    }

    private static void fillInput(WebDriverWait wait, String fieldName, String value) {
        WebElement input = wait.until(ExpectedConditions.presenceOfElementLocated(By.name(fieldName)));
        input.clear();
        input.sendKeys(value);
    }

    private static void trySelectJewelCaseAndA4(WebDriverWait wait) {
        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//input[@type='radio' and @name='template' and @value='jewel']"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//input[@type='radio' and @name='size' and @value='a4']"))).click();
    }

    private static void tryEnableForceSaveAs(WebDriverWait wait) {
        try {
            WebElement checkbox = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//input[@type='checkbox' and (contains(@name,'save') or contains(@name,'force') or contains(@value,'papercdcase'))]")
            ));
            if (!checkbox.isSelected()) {
                wait.until(ExpectedConditions.elementToBeClickable(checkbox)).click();
            }
        } catch (Exception ignored) {
            // Force Save-as may be absent, continue.
        }
    }

    private static void clickSubmit(WebDriver driver, WebElement submitButton) {
        try {
            submitButton.click();
        } catch (Exception clickException) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitButton);
        }
    }

    private static void deleteOldPapercdcasePdfs(Path resultDir) throws IOException {
        try (var stream = Files.list(resultDir)) {
            for (Path path : stream.toList()) {
                String name = path.getFileName().toString().toLowerCase();
                if (name.startsWith("papercdcase") && name.endsWith(".pdf")) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static ChromeOptions createChromeOptions(Path resultDir) {
        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", resultDir.toAbsolutePath().toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("safebrowsing.enabled", true);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);
        return options;
    }

    private static void openCasePage(WebDriver driver, WebDriverWait wait) {
        for (String baseUrl : BASE_URLS) {
            driver.get(baseUrl);
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.name("artist")));
                return;
            } catch (org.openqa.selenium.TimeoutException timeoutException) {
                // Try next URL (http/https).
            }
        }
        throw new IllegalStateException("Could not open papercdcase page");
    }

    private static Path waitForDownloadedPdf(Path resultDir) throws IOException, InterruptedException {
        long deadline = System.currentTimeMillis() + WAIT_TIMEOUT.toMillis();
        while (System.currentTimeMillis() < deadline) {
            Path latestPdf = findLatestPapercdcasePdf(resultDir);
            if (latestPdf != null) {
                return latestPdf;
            }
            Thread.sleep(POLL_INTERVAL.toMillis());
        }
        throw new IllegalStateException("PDF was not downloaded within timeout");
    }

    private static Path findLatestPapercdcasePdf(Path resultDir) throws IOException {
        try (var stream = Files.list(resultDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().startsWith("papercdcase"))
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".pdf"))
                    .filter(path -> !path.getFileName().toString().toLowerCase().endsWith(".crdownload"))
                    .max(Comparator.comparingLong(path -> path.toFile().lastModified()))
                    .orElse(null);
        }
    }

    private record CoverData(String artist, String title, List<String> tracks) {
    }
}
