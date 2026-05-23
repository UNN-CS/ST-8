package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {

    public static void main(String[] args) throws Exception {
        Path dataFile = Paths.get("data", "data.txt");

        if (!Files.exists(dataFile)) {
            System.err.println("ОШИБКА: Файл не найден: " + dataFile.toAbsolutePath());
            System.exit(1);
        }

        String artist = "";
        String title  = "";
        List<String> tracks = new ArrayList<>();

        for (String line : Files.readAllLines(dataFile)) {
            line = line.trim();

            if (line.startsWith("Artist:")) {
                artist = line.substring("Artist:".length()).trim();

            } else if (line.startsWith("Title:")) {
                title = line.substring("Title:".length()).trim();

            } else if (line.matches("Track \\d+:.*")) {
                int colonIdx = line.indexOf(':');
                if (colonIdx != -1) {
                    String trackName = line.substring(colonIdx + 1).trim();
                    tracks.add(trackName);
                }
            }
        }

        System.out.println("Artist : " + artist);
        System.out.println("Title  : " + title);
        System.out.println("Tracks : " + tracks.size());

        String downloadDir = Paths.get("result").toAbsolutePath().toString();
        Files.createDirectories(Paths.get(downloadDir));
        System.out.println("PDF будет сохранён в: " + downloadDir);

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory",         downloadDir);
        prefs.put("download.prompt_for_download",       false);
        prefs.put("download.directory_upgrade",         true);
        prefs.put("plugins.always_open_pdf_externally", true);
        prefs.put("safebrowsing.enabled",               false);
        prefs.put("safebrowsing.disable_download_protection", true);

        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--safebrowsing-disable-download-protection");
        options.addArguments("--disable-safe-browsing");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        try {
            driver.get("https://www.papercdcase.com/index.php");

            wait.until(ExpectedConditions.presenceOfElementLocated(By.name("artist")));
            System.out.println("Страница загружена.");

            WebElement artistField = driver.findElement(By.name("artist"));
            artistField.clear();
            artistField.sendKeys(artist);

            WebElement titleField = driver.findElement(By.name("title"));
            titleField.clear();
            titleField.sendKeys(title);

            for (int i = 0; i < tracks.size() && i < 16; i++) {
                String fieldName = "track" + (i + 1);
                try {
                    WebElement trackField = driver.findElement(By.name(fieldName));
                    trackField.clear();
                    trackField.sendKeys(tracks.get(i));
                    System.out.println("Track " + (i + 1) + ": " + tracks.get(i));
                } catch (Exception e) {
                    System.err.println("Не удалось найти поле: " + fieldName + " — " + e.getMessage());
                }
            }

            WebElement jewelRadio = driver.findElement(
                    By.xpath("//input[@value='jewel']"));
            if (!jewelRadio.isSelected()) {
                jewelRadio.click();
                System.out.println("Выбран тип: Jewel case");
            }

            WebElement a4Radio = driver.findElement(
                    By.xpath("//input[@value='a4']"));
            if (!a4Radio.isSelected()) {
                a4Radio.click();
                System.out.println("Выбран формат бумаги: A4");
            }

            Thread.sleep(500);

            WebElement btn = driver.findElement(
                    By.xpath("//input[@type='submit'] | //input[@type='image']"));
            System.out.println("Нажатие кнопки создания обложки...");
            btn.submit();

            System.out.println("Ожидание загрузки PDF...");
            waitForDownload(downloadDir, 20_000);

            renameToCdPdf(downloadDir);

            System.out.println("Файл сохранён.");

        } finally {
            Thread.sleep(2000);
            driver.quit();
        }
    }

    private static void waitForDownload(String dir, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            File[] files = new File(dir).listFiles(f ->
                    f.isFile()
                    && !f.getName().endsWith(".crdownload")
                    && !f.getName().endsWith(".tmp")
                    && !f.getName().startsWith(".com.google.Chrome")
            );
            if (files != null && files.length > 0) {
                System.out.println("Файл скачан: " + files[0].getName());
                return;
            }
            Thread.sleep(500);
        }
        System.err.println("PDF не появился за отведённое время.");
    }

    private static void renameToCdPdf(String dir) throws IOException {
        File[] pdfs = new File(dir).listFiles(f ->
                f.isFile() && f.getName().toLowerCase().endsWith(".pdf"));

        if (pdfs == null || pdfs.length == 0) {
            System.err.println("ОШИБКА: PDF-файл не найден в папке " + dir);
            return;
        }

        File target = new File(dir, "cd.pdf");
        if (target.exists()) {
            target.delete();
        }

        boolean renamed = pdfs[0].renameTo(target);
        if (renamed) {
            System.out.println("Файл переименован в cd.pdf");
        } else {
            System.err.println("Не удалось переименовать файл. Копирование вручную...");
            Files.copy(pdfs[0].toPath(), target.toPath());
            pdfs[0].delete();
        }
    }
}
