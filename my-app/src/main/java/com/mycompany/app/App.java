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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

public class App {
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "C:\\Program Files\\chromedriver-win64\\chromedriver.exe");

        String downloadFilepath = "../result";
        Path resultDir = Paths.get(downloadFilepath).toAbsolutePath().normalize();
        if (!Files.exists(resultDir)) {
            System.err.println("Директория для результатов не найдена: " + resultDir);
            System.err.println("Пожалуйста, убедитесь, что директория '" + downloadFilepath + "' существует относительно места запуска программы.");
            return;
        } else {
            System.out.println("Используется директория для результатов: " + resultDir);
        }
        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", resultDir.toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);
        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get("https://www.papercdcase.com/index.php");
            Thread.sleep(2000);

            Map<String, Object> cdData = readCDData("../data/data.txt");

            WebElement artistField = driver.findElement(
                    By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input"));
            artistField.sendKeys((String) cdData.get("Artist"));

            WebElement titleField = driver.findElement(
                    By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input"));
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

            WebElement caseTypeRadio = driver.findElement(
                    By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]"));
            if (!caseTypeRadio.isSelected()) {
                caseTypeRadio.click();
            }

            WebElement paperRadio = driver.findElement(
                    By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]"));
            if (!paperRadio.isSelected()) {
                paperRadio.click();
            }

            WebElement submitButton = driver.findElement(
                    By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input"));
            submitButton.click();

            Thread.sleep(7000);

            try {
                Path[] pdfFiles = Files.list(resultDir)
                        .filter(file -> !Files.isDirectory(file) && file.toString().toLowerCase().endsWith(".pdf"))
                        .sorted(Comparator.comparingLong(f -> {
                            try {
                                return Files.getLastModifiedTime(f).toMillis();
                            } catch (IOException e) {
                                e.printStackTrace();
                                return 0;
                            }
                        }))
                        .toArray(Path[]::new);

                if (pdfFiles.length > 0) {
                    Path latestFile = pdfFiles[pdfFiles.length - 1];
                    Path newFilePath = resultDir.resolve("cd.pdf");

                    if (Files.exists(newFilePath)) {
                        Files.delete(newFilePath);
                        System.out.println("Существующий result/cd.pdf удален.");
                    }

                    Files.copy(latestFile, newFilePath);
                    System.out.println("PDF файл успешно скопирован как result/cd.pdf");

                    Files.delete(latestFile);
                    System.out.println("Временный скачанный PDF файл удален: " + latestFile.getFileName());

                } else {
                    System.out.println("PDF файлы не найдены в директории загрузки: " + resultDir.toAbsolutePath());
                    System.out.println("Содержимое директории:");
                    Files.list(resultDir).forEach(p -> System.out.println(" - " + p.getFileName()));
                }
            } catch (Exception e) {
                System.out.println("Ошибка при работе с файлами PDF:");
                e.printStackTrace();
            }

        } catch (Exception e) {
            System.out.println("Произошла ошибка во время выполнения Selenium скрипта:");
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private static Map<String, Object> readCDData(String filePath) throws IOException {
        Map<String, Object> data = new HashMap<>();
        List<String> tracks = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean readingTracks = false;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Artist:")) {
                    data.put("Artist", line.substring("Artist:".length()).trim());
                } else if (line.startsWith("Title:")) {
                    data.put("Title", line.substring("Title:".length()).trim());
                } else if (line.trim().equalsIgnoreCase("Tracks:")) {
                    readingTracks = true;
                } else if (readingTracks) {
                    String trackText = line.trim();
                    if (!trackText.isEmpty()) {
                        int firstDotPos = trackText.indexOf('.');
                        if (firstDotPos > 0 && firstDotPos < trackText.length() - 1 && Character.isDigit(trackText.charAt(0))) {
                            trackText = trackText.substring(firstDotPos + 1).trim();
                        }
                        if (trackText.startsWith(" ")) {
                            trackText = trackText.substring(1);
                        }
                        if (!trackText.isEmpty()) {
                            tracks.add(trackText);
                        }
                    }
                }
            }
        }

        data.put("Tracks", tracks);
        return data;
    }
}