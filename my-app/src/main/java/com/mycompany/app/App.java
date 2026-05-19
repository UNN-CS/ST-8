package com.mycompany.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class App {
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver",
                "C:\\Program Files\\chromedriver-win64\\chromedriver.exe");

        try {
            Map<String, String> albumData = readAlbumData("../data/data.txt");
            ArrayList<String> tracks = readTracks("../data/data.txt");

            String downloadDir = new File("../result").getCanonicalPath();
            Files.createDirectories(Paths.get(downloadDir));

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--ignore-certificate-errors");
            options.addArguments("--allow-running-insecure-content");

            Map<String, Object> prefs = new HashMap<>();
            prefs.put("download.default_directory", downloadDir);
            prefs.put("download.prompt_for_download", false);
            prefs.put("plugins.always_open_pdf_externally", true);
            options.setExperimentalOption("prefs", prefs);

            WebDriver driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));

            try {
                driver.get("http://www.papercdcase.com");

                WebElement artist = driver.findElement(By.xpath("//input[@name='artist']"));
                WebElement title = driver.findElement(By.xpath("//input[@name='title']"));

                artist.clear();
                artist.sendKeys(albumData.getOrDefault("Artist", ""));

                title.clear();
                title.sendKeys(albumData.getOrDefault("Title", ""));

                for (int i = 0; i < tracks.size() && i < 16; i++) {
                    WebElement trackField = driver.findElement(
                            By.xpath("//input[@name='track" + (i + 1) + "']"));
                    trackField.clear();
                    trackField.sendKeys(tracks.get(i));
                }

                WebElement jewelCase = driver.findElement(
                        By.xpath("//input[@type='radio' and @name='template' and @value='jewel']"));
                if (!jewelCase.isSelected()) {
                    jewelCase.click();
                }

                WebElement a4Paper = driver.findElement(
                        By.xpath("//input[@type='radio' and @name='size' and @value='a4']"));
                if (!a4Paper.isSelected()) {
                    a4Paper.click();
                }

                WebElement submitButton = driver.findElement(
                        By.xpath("//input[@name='submit' and @type='image']"));
                submitButton.click();

                Path pdfPath = waitForPdf(Paths.get(downloadDir), Duration.ofSeconds(30));
                if (pdfPath != null) {
                    Path target = Paths.get(downloadDir, "cd.pdf");
                    Files.deleteIfExists(target);
                    Files.move(pdfPath, target);
                    System.out.println("PDF saved to: " + target.toAbsolutePath());
                } else {
                    System.out.println("PDF was not downloaded.");
                }
            } finally {
                driver.quit();
            }
        } catch (Exception e) {
            System.out.println("Error");
            System.out.println(e.toString());
        }
    }

    private static Map<String, String> readAlbumData(String filePath) throws Exception {
        Map<String, String> data = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Artist:")) {
                    data.put("Artist", line.substring("Artist:".length()).trim());
                } else if (line.startsWith("Title:")) {
                    data.put("Title", line.substring("Title:".length()).trim());
                }
            }
        }
        return data;
    }

    private static ArrayList<String> readTracks(String filePath) throws Exception {
        ArrayList<String> tracks = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Track:")) {
                    tracks.add(line.substring("Track:".length()).trim());
                }
            }
        }
        return tracks;
    }

    private static Path waitForPdf(Path dir, Duration timeout) throws Exception {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            try (var stream = Files.list(dir)) {
                Path found = stream
                        .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".pdf"))
                        .findFirst()
                        .orElse(null);
                if (found != null) {
                    return found;
                }
            }
            Thread.sleep(1000);
        }
        return null;
    }
}