package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

public class App {

    public static void main(String[] args) throws Exception {
        System.setProperty("webdriver.chrome.driver",
                "/home/user/Загрузки/chromedriver-linux64/chromedriver");

        Path dataFile = Paths.get("data", "data.txt");
        if (!Files.exists(dataFile)) {
            System.err.println("ОШИБКА: Файл не найден: " + dataFile.toAbsolutePath());
            System.exit(1);
        }

        String artist = "";
        String title = "";
        List<String> tracks = new ArrayList<>();

        for (String line : Files.readAllLines(dataFile)) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.toLowerCase().startsWith("artist:")) {
                artist = line.substring(line.indexOf(":") + 1).trim();
            } else if (line.toLowerCase().startsWith("title:")) {
                title = line.substring(line.indexOf(":") + 1).trim();
            } else if (line.toLowerCase().matches("track\\s*\\d+\\s*:.*")) {
                String trackName = line.substring(line.indexOf(":") + 1).trim();
                tracks.add(trackName);
            }
        }

        System.out.println("Исполнитель: " + artist);
        System.out.println("Альбом: " + title);
        System.out.println("Количество треков: " + tracks.size());

        Path resultDir = Paths.get("result");
        if (!Files.exists(resultDir)) {
            Files.createDirectories(resultDir);
        }
        String downloadDir = resultDir.toAbsolutePath().toString();

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadDir);
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("plugins.always_open_pdf_externally", true);
        prefs.put("safebrowsing.enabled", false);
        prefs.put("safebrowsing.disable_download_protection", true);

        ChromeOptions options = new ChromeOptions();
        options.setBinary("/home/user/Загрузки/chrome-linux64/chrome");
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--disable-popup-blocking");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        try {
            driver.get("https://www.papercdcase.com/index.php");

            WebElement fieldArtist = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@name='artist']"))
            );
            fieldArtist.clear();
            fieldArtist.sendKeys(artist);

            WebElement fieldTitle = driver.findElement(By.xpath("//input[@name='title']"));
            fieldTitle.clear();
            fieldTitle.sendKeys(title);

            int maxTracks = Math.min(tracks.size(), 16);
            for (int i = 0; i < maxTracks; i++) {
                String xpath = "//input[@name='track" + (i + 1) + "']";
                try {
                    WebElement trackField = driver.findElement(By.xpath(xpath));
                    trackField.clear();
                    trackField.sendKeys(tracks.get(i));
                } catch (Exception e) {
                    System.out.println("Поле track" + (i + 1) + " не найдено");
                    break;
                }
            }

            WebElement radioJewel = driver.findElement(By.xpath("//input[@name='template' and @value='jewel']"));
            if (!radioJewel.isSelected()) {
                radioJewel.click();
            }

            WebElement radioA4 = driver.findElement(By.xpath("//input[@name='size' and @value='a4']"));
            if (!radioA4.isSelected()) {
                radioA4.click();
            }

            WebElement submitButton = wait.until(
                    ExpectedConditions.elementToBeClickable(By.name("submit"))
            );
            submitButton.click();

            File pdfFile = waitForPdf(downloadDir, 20);
            if (pdfFile != null) {
                File target = new File(downloadDir, "cd.pdf");
                if (target.exists()) target.delete();
                if (!pdfFile.renameTo(target)) {
                    Files.move(pdfFile.toPath(), target.toPath());
                }
                System.out.println("Файл сохранён: " + target.getAbsolutePath());
            } else {
                System.err.println("PDF не получен");
            }
        } finally {
            Thread.sleep(2000);
            driver.quit();
        }
    }

    private static File waitForPdf(String folderPath, int timeoutSeconds) throws InterruptedException {
        long start = System.currentTimeMillis();
        long timeoutMs = timeoutSeconds * 1000L;
        while (System.currentTimeMillis() - start < timeoutMs) {
            File[] files = new File(folderPath).listFiles(
                    (dir, name) -> name.toLowerCase().endsWith(".pdf") && !name.equals("cd.pdf")
            );
            if (files != null && files.length > 0) {
                return files[0];
            }
            Thread.sleep(500);
        }
        return null;
    }
}
