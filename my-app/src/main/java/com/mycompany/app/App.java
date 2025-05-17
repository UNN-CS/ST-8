package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class App {
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "D:\\chromedriver-win64\\chromedriver.exe");

        String downloadFolder = Paths.get("..", "result").toAbsolutePath().normalize().toString();
        Path downloadPath = Paths.get(downloadFolder);

        try {
            Files.createDirectories(downloadPath);
        } catch (IOException e) {
            System.err.println("Ошибка при создании папки: " + e.getMessage());
            return;
        }

        ChromeOptions chromeOptions = new ChromeOptions();
        Map<String, Object> chromePrefs = new HashMap<>();
        chromePrefs.put("download.default_directory", downloadFolder);
        chromePrefs.put("download.prompt_for_download", false);
        chromePrefs.put("plugins.always_open_pdf_externally", true);
        chromeOptions.setExperimentalOption("prefs", chromePrefs);

        WebDriver webDriver = new ChromeDriver(chromeOptions);

        try {
            Map<String, Object> cdInfo = loadCDInfo();

            webDriver.manage().window().maximize();
            webDriver.get("http://www.papercdcase.com/index.php");

            webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input"))
                    .sendKeys((String) cdInfo.get("ArtistName"));
            webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input"))
                    .sendKeys((String) cdInfo.get("AlbumTitle"));

            @SuppressWarnings("unchecked")
            List<String> trackList = (List<String>) cdInfo.get("TrackList");

            for (int i = 0; i < Math.min(8, trackList.size()); i++) {
                webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[" + (i + 1) + "]/td[2]/input"))
                        .sendKeys(trackList.get(i));
            }

            for (int i = 8; i < Math.min(16, trackList.size()); i++) {
                webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[" + (i - 7) + "]/td[2]/input"))
                        .sendKeys(trackList.get(i));
            }

            WebElement caseOption = webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]"));
            if (!caseOption.isSelected()) caseOption.click();

            WebElement paperOption = webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]"));
            if (!paperOption.isSelected()) paperOption.click();

            webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input")).click();

            Thread.sleep(5000);

            saveLatestPdf(downloadPath);

        } catch (Exception e) {
            System.err.println("Ошибка выполнения скрипта:");
            e.printStackTrace();
        } finally {
            webDriver.quit();
        }
    }

    private static Map<String, Object> loadCDInfo() throws IOException {
        Map<String, Object> cdData = new HashMap<>();
        List<String> tracks = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader("../data/data.txt"))) {
            String line;
            boolean readingTracks = false;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("Artist:")) {
                    cdData.put("ArtistName", line.substring("Artist:".length()).trim());
                    readingTracks = false;
                } else if (line.startsWith("Title:")) {
                    cdData.put("AlbumTitle", line.substring("Title:".length()).trim());
                    readingTracks = false;
                } else if (line.equalsIgnoreCase("Tracks:")) {
                    readingTracks = true;
                } else if (readingTracks) {
                    String trackName = line.replaceAll("^\\d+\\.\\s*", "");
                    tracks.add(trackName);
                }
            }
        }

        if (!cdData.containsKey("ArtistName") || !cdData.containsKey("AlbumTitle") || tracks.isEmpty()) {
            throw new IOException("data.txt содержит неполные или неверные данные");
        }

        cdData.put("TrackList", tracks);
        return cdData;
    }

    private static void saveLatestPdf(Path folderPath) throws IOException {
        Path[] pdfFiles = Files.list(folderPath)
                .filter(p -> !Files.isDirectory(p) && p.toString().endsWith(".pdf"))
                .toArray(Path[]::new);

        if (pdfFiles.length == 0) {
            System.out.println("PDF файл не найден в папке: " + folderPath);
            return;
        }

        Path latestPdf = pdfFiles[0];
        long latestModifiedTime = Files.getLastModifiedTime(latestPdf).toMillis();

        for (Path pdf : pdfFiles) {
            long currentModifiedTime = Files.getLastModifiedTime(pdf).toMillis();
            if (currentModifiedTime > latestModifiedTime) {
                latestPdf = pdf;
                latestModifiedTime = currentModifiedTime;
            }
        }

        Path targetPdf = folderPath.resolve("cd.pdf");
        Files.deleteIfExists(targetPdf);
        Files.move(latestPdf, targetPdf);
        System.out.println("PDF файл сохранён как cd.pdf");
    }
}
