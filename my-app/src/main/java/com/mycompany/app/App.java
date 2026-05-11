package com.mycompany.app;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class App {

    private static final String TARGET_URL = "http://www.papercdcase.com/";
    private static final String PDF_ENDPOINT = "http://www.papercdcase.com/papercdcase.cgi/papercdcase.pdf";
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(90);

    public static void main(String[] args) throws Exception {
        Path repoRoot = Paths.get("").toAbsolutePath().getParent();
        Path dataFile = repoRoot.resolve("data").resolve("data.txt");
        Path resultDir = repoRoot.resolve("result");
        Path outputPdf = resultDir.resolve("cd.pdf");

        CoverData coverData = CoverDataLoader.fromFile(dataFile);
        Files.createDirectories(resultDir);
        deleteLeftoverPdfFiles(resultDir);

        try {
            generatePdfWithSelenium(coverData, resultDir, outputPdf);
        } catch (Exception seleniumError) {
            System.err.println("Selenium flow failed, falling back to direct PDF download: "
                    + seleniumError.getMessage());
            downloadPdfDirectly(coverData, outputPdf);
        }

        System.out.println("PDF saved to: " + outputPdf);
    }

    private static void generatePdfWithSelenium(CoverData coverData, Path resultDir, Path outputPdf) throws Exception {
        ChromeOptions options = buildChromeOptions(resultDir);
        WebDriver driver = new ChromeDriver(options);

        try {
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(20));
            driver.get(TARGET_URL);

            WebDriverWait wait = new WebDriverWait(driver, WAIT_TIMEOUT);
            fillTextField(wait, "artist", coverData.artist());
            fillTextField(wait, "title", coverData.title());
            fillTracks(wait, coverData.tracks());
            setRadio(wait, driver, "template", "jewel");
            setRadio(wait, driver, "size", "a4");
            setCheckbox(wait, driver, "force_saveas");

            WebElement submitButton = findSubmitButton(wait);
            clickElement(driver, submitButton);

            Path downloadedPdf = waitForPdf(resultDir, DOWNLOAD_TIMEOUT);
            Files.move(downloadedPdf, outputPdf);
        } finally {
            driver.quit();
        }
    }

    private static ChromeOptions buildChromeOptions(Path downloadDir) {
        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);

        Path installedChrome = Paths.get("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe");
        if (Files.exists(installedChrome)) {
            options.setBinary(installedChrome.toString());
        }

        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--ignore-certificate-errors");
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadDir.toAbsolutePath().toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("plugins.always_open_pdf_externally", true);
        prefs.put("safebrowsing.enabled", true);
        options.setExperimentalOption("prefs", prefs);

        return options;
    }

    private static void fillTextField(WebDriverWait wait, String fieldName, String value) {
        WebElement field = wait.until(ExpectedConditions.elementToBeClickable(By.name(fieldName)));
        field.clear();
        field.sendKeys(value);
    }

    private static void fillTracks(WebDriverWait wait, List<String> tracks) {
        for (int index = 0; index < tracks.size(); index++) {
            WebElement input = wait.until(ExpectedConditions.elementToBeClickable(By.name("track" + (index + 1))));
            input.clear();
            input.sendKeys(tracks.get(index));
        }
    }

    private static void setRadio(WebDriverWait wait, WebDriver driver, String fieldName, String value) {
        String xpath = String.format(Locale.ROOT, "//input[@type='radio' and @name='%s' and @value='%s']",
                fieldName, value);
        WebElement radio = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
        clickElement(driver, radio);
    }

    private static void setCheckbox(WebDriverWait wait, WebDriver driver, String fieldName) {
        WebElement checkbox = wait.until(ExpectedConditions.presenceOfElementLocated(By.name(fieldName)));
        if (!checkbox.isSelected()) {
            clickElement(driver, checkbox);
        }
    }

    private static WebElement findSubmitButton(WebDriverWait wait) {
        return wait.until(ExpectedConditions.elementToBeClickable(By.name("submit")));
    }

    private static void clickElement(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    private static Path waitForPdf(Path resultDir, Duration timeout) throws IOException, InterruptedException {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(resultDir, "*.pdf")) {
                for (Path file : stream) {
                    if (!file.getFileName().toString().equalsIgnoreCase("cd.pdf")) {
                        return file;
                    }
                }
            }

            Thread.sleep(1_000);
        }

        throw new IllegalStateException("Timed out waiting for the generated PDF file.");
    }

    private static void deleteLeftoverPdfFiles(Path resultDir) throws IOException {
        try (DirectoryStream<Path> pdfs = Files.newDirectoryStream(resultDir, "*.pdf")) {
            for (Path pdf : pdfs) {
                Files.deleteIfExists(pdf);
            }
        }

        try (DirectoryStream<Path> partials = Files.newDirectoryStream(resultDir, "*.crdownload")) {
            for (Path partial : partials) {
                Files.deleteIfExists(partial);
            }
        }
    }

    private static void downloadPdfDirectly(CoverData coverData, Path outputPdf) throws IOException, InterruptedException {
        Files.deleteIfExists(outputPdf);

        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("artist", coverData.artist());
        queryParams.put("title", coverData.title());

        for (int index = 0; index < coverData.tracks().size(); index++) {
            queryParams.put("track" + (index + 1), coverData.tracks().get(index));
        }

        queryParams.put("template", "jewel");
        queryParams.put("size", "a4");
        queryParams.put("lang", "west");
        queryParams.put("force_saveas", "yes");

        String query = queryParams.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElseThrow();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PDF_ENDPOINT + "?" + query))
                .timeout(DOWNLOAD_TIMEOUT)
                .GET()
                .build();

        HttpResponse<Path> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofFile(outputPdf));

        if (response.statusCode() != 200) {
            throw new IOException("PDF download failed with HTTP status " + response.statusCode());
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
