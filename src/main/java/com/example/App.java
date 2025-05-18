package com.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-popup-blocking");

        Map<String, Object> prefs = new HashMap<>();
        String downloadPath = System.getProperty("user.dir") + "/result";
        prefs.put("download.default_directory", downloadPath);
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("safebrowsing.enabled", true);
        prefs.put("plugins.always_open_pdf_externally", true);
        prefs.put("browser.helperApps.neverAsk.saveToDisk", "application/pdf");
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        try {
            Path downloadDir = Paths.get(System.getProperty("user.dir"), "result");
            Files.createDirectories(downloadDir);
            logger.info("Директория для скачивания создана: {}", downloadDir);

            driver.get("http://www.papercdcase.com/index.php");
            logger.info("Страница открыта");

            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(2000);

            List<String> lines = Files.readAllLines(Paths.get("data/data.txt"));
            String artist = lines.get(0);
            String title = lines.get(1);
            List<String> trackLines = lines.subList(2, lines.size());

            WebElement artistField = wait
                    .until(ExpectedConditions.elementToBeClickable(By.cssSelector("input[name='artist']")));
            artistField.clear();
            artistField.sendKeys(artist);

            WebElement titleField = wait
                    .until(ExpectedConditions.elementToBeClickable(By.cssSelector("input[name='title']")));
            titleField.clear();
            titleField.sendKeys(title);

            for (int i = 0; i < Math.min(trackLines.size(), 16); i++) {
                String trackName = "track" + (i + 1);
                WebElement trackField = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("input[name='" + trackName + "']")));
                trackField.clear();
                trackField.sendKeys(trackLines.get(i));
            }

            WebElement a4Radio = wait
                    .until(ExpectedConditions.elementToBeClickable(By.cssSelector("input[value='a4']")));
            a4Radio.click();

            WebElement jewelCaseRadio = wait
                    .until(ExpectedConditions.elementToBeClickable(By.cssSelector("input[value='jewel']")));
            jewelCaseRadio.click();

            String formUrl = driver.getCurrentUrl();
            logger.info("URL формы: {}", formUrl);

            WebElement submitButton = wait
                    .until(ExpectedConditions.elementToBeClickable(By.cssSelector("input[name='submit']")));
            submitButton.click();
            logger.info("Форма отправлена");

            Thread.sleep(5000);

            String baseUrl = "http://www.papercdcase.com/papercdcase.cgi/papercdcase.pdf";
            StringBuilder urlBuilder = new StringBuilder(baseUrl);
            urlBuilder.append("?sql_serial=&sql_genre=");
            urlBuilder.append("&artist=").append(URLEncoder.encode(artist, "UTF-8"));
            urlBuilder.append("&title=").append(URLEncoder.encode(title, "UTF-8"));
            for (int i = 0; i < 16; i++) {
                String track = (i < trackLines.size()) ? trackLines.get(i) : "";
                urlBuilder.append("&track").append(i + 1).append("=")
                        .append(URLEncoder.encode(track, "UTF-8"));
            }
            urlBuilder.append("&template=jewel");
            urlBuilder.append("&size=a4");
            urlBuilder.append("&lang=west");

            String pdfUrl = urlBuilder.toString();
            System.out.println("PDF URL: " + pdfUrl);

            try (InputStream in = new URL(pdfUrl).openStream()) {
                Path targetFile = downloadDir.resolve("cd.pdf");
                Files.copy(in, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                logger.info("PDF файл успешно скачан: {}", targetFile);
            }

        } catch (Exception e) {
            logger.error("Произошла ошибка: {}", e.getMessage(), e);
        } finally {
            driver.quit();
            logger.info("Браузер закрыт");
        }
    }
}