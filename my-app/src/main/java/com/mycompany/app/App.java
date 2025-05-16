package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "D:\\chromedriver-win64\\chromedriver.exe");

        String downloadFilepath = Paths.get("result").toAbsolutePath().normalize().toString();
        Path resultDir = Paths.get(downloadFilepath);

        try {
            Files.createDirectories(resultDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadFilepath);
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);
        try {
            Map<String, Object> cdData = readCDData();

            driver.get("http://www.papercdcase.com/index.php");

            WebElement artistField = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input"));
            artistField.sendKeys((String) cdData.get("Artist"));

            WebElement titleField = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input"));
            titleField.sendKeys((String) cdData.get("Title"));

            @SuppressWarnings("unchecked")
            List<String> tracks = (List<String>) cdData.get("Tracks");

            for (int i = 0; i < Math.min(8, tracks.size()); i++) {
                WebElement trackField = driver.findElement(
                        By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[" + (i + 1) + "]/td[2]/input"));
                trackField.sendKeys(tracks.get(i));
            }

            for (int i = 8; i < Math.min(16, tracks.size()); i++) {
                WebElement trackField = driver.findElement(
                        By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[" + (i - 7) + "]/td[2]/input"));
                trackField.sendKeys(tracks.get(i));
            }

            WebElement caseTypeRadio = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]"));
            caseTypeRadio.click();

            WebElement paperRadio = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]"));
            paperRadio.click();

            WebElement submitButton = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input"));
            submitButton.click();

            Thread.sleep(3000);

            handleDownloadedPdf(resultDir);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    private static Map<String, Object> readCDData() throws IOException {
        Map<String, Object> data = new HashMap<>();
        List<String> tracks = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader("data/data.txt"))) {
            String line;
            boolean readingTracks = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("Artist:")) {
                    data.put("Artist", line.substring("Artist:".length()).trim());
                    readingTracks = false;
                } else if (line.startsWith("Title:")) {
                    data.put("Title", line.substring("Title:".length()).trim());
                    readingTracks = false;
                } else if (line.equalsIgnoreCase("Tracks:")) {
                    readingTracks = true;
                } else if (readingTracks) {
                    tracks.add(line);
                }
            }
        }

        data.put("Tracks", tracks);
        return data;
    }

    private static void handleDownloadedPdf(Path resultDir) throws IOException {
        Path[] pdfFiles = Files.list(resultDir)
                .filter(file -> file.toString().endsWith(".pdf"))
                .toArray(Path[]::new);

        if (pdfFiles.length > 0) {
            Path latestFile = pdfFiles[0];
            Path newFilePath = resultDir.resolve("cd.pdf");
            Files.move(latestFile, newFilePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}