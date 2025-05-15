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
        System.setProperty("webdriver.chrome.driver", "E:\\Downloads\\chromedriver-win64\\chromedriver.exe");

        String downloadFilepath = Paths.get("..", "result").toAbsolutePath().normalize().toString();
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

            driver.manage().window().maximize();
            driver.get("https://www.papercdcase.com/index.php");

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
            if (!caseTypeRadio.isSelected()) caseTypeRadio.click();

            WebElement paperRadio = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]"));
            if (!paperRadio.isSelected()) paperRadio.click();

            WebElement submitButton = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input"));
            submitButton.click();

            Thread.sleep(5000);

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

        try (BufferedReader reader = new BufferedReader(new FileReader("../data/data.txt"))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (lineNumber == 0) {
                    data.put("Artist", line);
                } else if (lineNumber == 1) {
                    data.put("Title", line);
                } else {
                    tracks.add(line);
                }
                lineNumber++;
            }
        }

        if (!data.containsKey("Artist") || !data.containsKey("Title") || tracks.isEmpty()) {
            throw new IOException("The data.txt file contains incomplete or invalid data");
        }

        data.put("Tracks", tracks);
        return data;
    }

    private static void handleDownloadedPdf(Path resultDir) {
        try {
            Path[] pdfFiles = Files.list(resultDir)
                    .filter(file -> !Files.isDirectory(file) && file.toString().endsWith(".pdf"))
                    .toArray(Path[]::new);

            if (pdfFiles.length == 0) {
                System.out.println("No PDF files found in directory: " + resultDir);
                Files.list(resultDir).forEach(p -> System.out.println(" - " + p.getFileName()));
                return;
            }

            Path latestFile = pdfFiles[0];
            long latestTime = Files.getLastModifiedTime(latestFile).toMillis();

            for (int i = 1; i < pdfFiles.length; i++) {
                long fileTime = Files.getLastModifiedTime(pdfFiles[i]).toMillis();
                if (fileTime > latestTime) {
                    latestTime = fileTime;
                    latestFile = pdfFiles[i];
                }
            }

            Path newFilePath = resultDir.resolve("cd.pdf");

            try {
                Files.deleteIfExists(newFilePath);
                System.out.println("Existing cd.pdf deleted");
            } catch (IOException e) {
                System.err.println("Failed to delete old cd.pdf: " + e.getMessage());
            }

            Files.copy(latestFile, newFilePath);
            System.out.println("PDF file successfully copied as cd.pdf");

            Files.deleteIfExists(latestFile);
        } catch (Exception e) {
            System.out.println("Error handling PDF files:");
            e.printStackTrace();
        }
    }
}
