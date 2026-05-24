package com.example.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {

    private static final String SITE_URL      = "http://www.papercdcase.com";
    private static final String DATA_FILE     = "data/data.txt";
    private static final String RESULT_DIR    = "result";
    private static final String PDF_ORIGINAL  = "papercdcase.pdf";
    private static final String PDF_RESULT    = "cd.pdf";
    private static final int    WAIT_SECONDS  = 15;
    private static final long   DL_TIMEOUT_MS = 30_000;

    public static void main(String[] args) throws Exception {
        File resultDir = new File(RESULT_DIR);
        resultDir.mkdirs();
        String downloadPath = resultDir.getAbsolutePath();

        WebDriver driver = buildDriver(downloadPath);
        try {
            List<String> lines = Files.readAllLines(Paths.get(DATA_FILE));
            if (lines.size() < 3) {
                System.out.println("data.txt: line 1=artist, line 2=title, then tracks.");
                return;
            }

            driver.get(SITE_URL);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_SECONDS));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("artist")));

            fillArtistAndTitle(driver, lines);
            fillTracks(driver, lines);
            selectOptions(driver);
            submitForm(driver);

            waitAndRenameDownload(downloadPath);

        } finally {
            driver.quit();
        }
    }

    private static WebDriver buildDriver(String downloadPath) {
        resolveDriverPath();

        Map<String, Object> prefs = new HashMap<String, Object>();
        prefs.put("download.default_directory", downloadPath);
        prefs.put("plugins.always_open_pdf_externally", true);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
        options.setExperimentalOption("prefs", prefs);

        if (isHeadless()) {
            options.addArguments("--headless=new", "--disable-gpu", "--window-size=1280,900");
        }

        return new ChromeDriver(options);
    }

    private static void resolveDriverPath() {
        String prop = System.getProperty("webdriver.chrome.driver");
        if (prop != null && !prop.trim().isEmpty()) return;
        for (String var : new String[]{"WEBDRIVER_CHROME_DRIVER", "CHROMEWEBDRIVER"}) {
            String val = System.getenv(var);
            if (val != null && !val.trim().isEmpty()) {
                System.setProperty("webdriver.chrome.driver", val.trim());
                return;
            }
        }
    }

    private static boolean isHeadless() {
        return "true".equalsIgnoreCase(System.getenv("HEADLESS"))
                || System.getenv("GITHUB_ACTIONS") != null;
    }

    private static void fillArtistAndTitle(WebDriver driver, List<String> lines) {
        WebElement artist = driver.findElement(By.name("artist"));
        artist.clear();
        artist.sendKeys(lines.get(0).trim());

        WebElement title = driver.findElement(By.name("title"));
        title.clear();
        title.sendKeys(lines.get(1).trim());
    }

    private static void fillTracks(WebDriver driver, List<String> lines) {
        int trackIdx = 2;
        for (int i = 1; i <= 16 && trackIdx < lines.size(); i++, trackIdx++) {
            try {
                WebElement field = driver.findElement(By.name("track" + i));
                field.clear();
                field.sendKeys(lines.get(trackIdx).trim());
            } catch (Exception e) {
                System.out.println("track" + i + " not found, skipping.");
            }
        }
    }

    private static void selectOptions(WebDriver driver) {
        WebElement jewel = driver.findElement(
                By.xpath("//input[@type='radio'][@name='template'][@value='jewel']"));
        if (!jewel.isSelected()) jewel.click();

        WebElement a4 = driver.findElement(
                By.xpath("//input[@type='radio'][@name='size'][@value='a4']"));
        if (!a4.isSelected()) a4.click();
    }

    private static void submitForm(WebDriver driver) {
        WebElement btn = driver.findElement(By.xpath("//input[@name='submit']"));
        btn.click();
        System.out.println("Form submitted, waiting for PDF...");
    }

    private static void waitAndRenameDownload(String downloadPath) throws Exception {
        File downloaded = new File(downloadPath, PDF_ORIGINAL);
        long deadline = System.currentTimeMillis() + DL_TIMEOUT_MS;

        while (System.currentTimeMillis() < deadline) {
            if (downloaded.exists() && downloaded.length() > 0) break;
            Thread.sleep(500);
        }

        if (downloaded.exists() && downloaded.length() > 0) {
            File target = new File(downloadPath, PDF_RESULT);
            Files.move(downloaded.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Done: " + target.getAbsolutePath());
        } else {
            System.out.println("Error: PDF did not download within " + (DL_TIMEOUT_MS / 1000) + "s.");
        }
    }
}