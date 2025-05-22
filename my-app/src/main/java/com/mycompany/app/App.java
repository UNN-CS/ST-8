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

        Path outputDir = Paths.get("..", "result").toAbsolutePath().normalize();
        try {
            Files.createDirectories(outputDir);

            ChromeOptions chromeSettings = new ChromeOptions();
            Map<String, Object> chromePrefs = new HashMap<>();
            chromePrefs.put("download.default_directory", outputDir.toString());
            chromePrefs.put("download.prompt_for_download", false);
            chromePrefs.put("plugins.always_open_pdf_externally", true);
            chromeSettings.setExperimentalOption("prefs", chromePrefs);

            WebDriver browser = new ChromeDriver(chromeSettings);
            try {
                browser.manage().window().maximize();
                browser.get("https://www.papercdcase.com/index.php");

                Map<String, Object> discInfo = fetchDiscInfo();

                WebElement artistInput = browser.findElement(
                        By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input"));
                artistInput.sendKeys((String) discInfo.get("Artist"));

                WebElement albumInput = browser.findElement(
                        By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input"));
                albumInput.sendKeys((String) discInfo.get("Title"));

                @SuppressWarnings("unchecked")
                List<String> songList = (List<String>) discInfo.get("Tracks");

                // Fill left column tracks
                for (int idx = 0; idx < Math.min(8, songList.size()); idx++) {
                    WebElement trackInput = browser.findElement(
                            By.xpath(
                                    "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr["
                                            + (idx + 1) + "]/td[2]/input"));
                    trackInput.sendKeys(songList.get(idx));
                }
                // Fill right column tracks
                for (int idx = 8; idx < Math.min(16, songList.size()); idx++) {
                    WebElement trackInput = browser.findElement(
                            By.xpath(
                                    "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr["
                                            + (idx - 7) + "]/td[2]/input"));
                    trackInput.sendKeys(songList.get(idx));
                }

                // Select options
                WebElement caseTypeOption = browser.findElement(
                        By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]"));
                if (!caseTypeOption.isSelected())
                    caseTypeOption.click();

                WebElement paperOption = browser.findElement(
                        By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]"));
                if (!paperOption.isSelected())
                    paperOption.click();

                // Submit form
                WebElement submitBtn = browser.findElement(
                        By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input"));
                submitBtn.click();

                Thread.sleep(5000);

                processPdfFile(outputDir);

            } catch (Exception ex) {
                System.err.println("An error occurred during browser automation:");
                ex.printStackTrace();
            } finally {
                browser.quit();
            }
        } catch (IOException ioEx) {
            System.err.println("Failed to create result directory: " + ioEx.getMessage());
        }
    }

    private static Map<String, Object> fetchDiscInfo() throws IOException {
        Map<String, Object> infoMap = new HashMap<>();
        List<String> trackNames = new ArrayList<>();
        boolean isTrackSection = false;

        try (BufferedReader bufReader = new BufferedReader(new FileReader("../data/data.txt"))) {
            String currLine;
            while ((currLine = bufReader.readLine()) != null) {
                currLine = currLine.trim();
                if (currLine.isEmpty())
                    continue;

                if (currLine.startsWith("Artist:")) {
                    infoMap.put("Artist", currLine.substring("Artist:".length()).trim());
                    isTrackSection = false;
                } else if (currLine.startsWith("Title:")) {
                    infoMap.put("Title", currLine.substring("Title:".length()).trim());
                    isTrackSection = false;
                } else if (currLine.equalsIgnoreCase("Tracks:")) {
                    isTrackSection = true;
                } else if (isTrackSection) {
                    trackNames.add(currLine);
                }
            }
        }

        if (!infoMap.containsKey("Artist") || !infoMap.containsKey("Title") || trackNames.isEmpty()) {
            throw new IOException("data.txt file contains incomplete or invalid data");
        }

        infoMap.put("Tracks", trackNames);
        return infoMap;
    }

    private static void processPdfFile(Path outputDir) {
        try {
            Path[] foundPdfs = Files.list(outputDir)
                    .filter(f -> !Files.isDirectory(f) && f.toString().endsWith(".pdf"))
                    .toArray(Path[]::new);

            if (foundPdfs.length == 0) {
                System.out.println("No PDF files found in directory: " + outputDir);
                Files.list(outputDir).forEach(p -> System.out.println(" - " + p.getFileName()));
                return;
            }

            // Find the newest PDF file
            Path newestPdf = foundPdfs[0];
            long newestTime = Files.getLastModifiedTime(newestPdf).toMillis();
            for (int i = 1; i < foundPdfs.length; i++) {
                long fileTime = Files.getLastModifiedTime(foundPdfs[i]).toMillis();
                if (fileTime > newestTime) {
                    newestTime = fileTime;
                    newestPdf = foundPdfs[i];
                }
            }

            Path renamedPdf = outputDir.resolve("cd.pdf");

            // Remove old cd.pdf if exists, then copy and remove the original
            if (Files.exists(renamedPdf)) {
                Files.delete(renamedPdf);
                System.out.println("Old cd.pdf was deleted");
            }
            Files.copy(newestPdf, renamedPdf);
            System.out.println("PDF file successfully copied as cd.pdf");

            Files.deleteIfExists(newestPdf);
        } catch (Exception err) {
            System.out.println("Error while handling PDF files:");
            err.printStackTrace();
        }
    }
}
