package com.example;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "C:/Дополнительно/chromedriver-win64/chromedriver.exe");

        String downloadPath = System.getProperty("user.dir") + "/result";

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadPath);
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("safebrowsing.enabled", false);
        prefs.put("profile.default_content_setting_values.automatic_downloads", 1);

        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-running-insecure-content");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--safebrowsing-disable-download-protection");
        options.addArguments("--allow-insecure-localhost");
        options.addArguments("--disable-popup-blocking");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});

        WebDriver driver = new ChromeDriver(options);
        try {
            List<String> lines = Files.readAllLines(Paths.get("data/data.txt"));
            String artist = lines.get(0);
            String title = lines.get(1);
            List<String> tracks = lines.subList(2, lines.size());

            driver.get("http://www.papercdcase.com");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            // Artist
            WebElement artistField = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("artist")));
            artistField.sendKeys(artist);

            // Title
            driver.findElement(By.name("title")).sendKeys(title);

            // Треки
            for (int i = 0; i < tracks.size() && i < 16; i++) {
                String fieldName = "track" + (i + 1);
                WebElement trackField = wait.until(ExpectedConditions.presenceOfElementLocated(By.name(fieldName)));
                trackField.sendKeys(tracks.get(i));
            }

            // Радиокнопки
            driver.findElement(By.cssSelector("input[type='radio'][value='jewel']")).click();
            driver.findElement(By.cssSelector("input[type='radio'][value='a4']")).click();

            // Кнопка Create
            WebElement createButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", createButton);

            // Ожидание загрузки файла (с обработкой InterruptedException)
            Path pdfPath = Paths.get(downloadPath, "papercdcase.pdf");
            boolean downloaded = false;
            for (int i = 0; i < 30; i++) {
                if (Files.exists(pdfPath)) {
                    downloaded = true;
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (downloaded) {
                Path targetPdf = Paths.get(downloadPath, "cd.pdf");
                Files.move(pdfPath, targetPdf, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.out.println("✅ PDF сохранён: " + targetPdf.toAbsolutePath());
            } else {
                System.out.println("⚠️ Файл не скачался автоматически.");
                System.out.println("Проверьте папку " + downloadPath + " на наличие papercdcase.pdf");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}