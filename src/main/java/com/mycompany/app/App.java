package com.mycompany.app;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class App {
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "C:\\Program Files\\Google\\chromedriver-win64\\chromedriver.exe");

        Path outputDir = Paths.get("C:\\st8\\my-app\\result");
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            System.err.println("Ошибка при создании директории: " + e.getMessage());
        }

        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = Map.of(
                "download.default_directory", outputDir.toString(),
                "download.prompt_for_download", false,
                "plugins.always_open_pdf_externally", true
        );
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);

        try {
            Map<String, Object> cdInfo = parseCdData();

            driver.manage().window().maximize();
            driver.get("https://www.papercdcase.com/index.php");

            driver.findElement(By.xpath(xPaths.artist)).sendKeys((String) cdInfo.get("Artist"));
            driver.findElement(By.xpath(xPaths.title)).sendKeys((String) cdInfo.get("Title"));

            @SuppressWarnings("unchecked")
            List<String> allTracks = (List<String>) cdInfo.get("Tracks");

            List<String> leftTracks = allTracks.subList(0, Math.min(8, allTracks.size()));
            List<String> rightTracks = allTracks.size() > 8 ? allTracks.subList(8, Math.min(16, allTracks.size())) : List.of();

            for (int i = 0; i < leftTracks.size(); i++) {
                String xpath = String.format(xPaths.trackLeft, i + 1);
                driver.findElement(By.xpath(xpath)).sendKeys(leftTracks.get(i));
            }

            for (int i = 0; i < rightTracks.size(); i++) {
                String xpath = String.format(xPaths.trackRight, i + 1);
                driver.findElement(By.xpath(xpath)).sendKeys(rightTracks.get(i));
            }

            WebElement jewelCase = driver.findElement(By.xpath(xPaths.caseType));
            if (!jewelCase.isSelected()) jewelCase.click();

            WebElement a4Paper = driver.findElement(By.xpath(xPaths.paperFormat));
            if (!a4Paper.isSelected()) a4Paper.click();

            driver.findElement(By.xpath(xPaths.submit)).click();
            Thread.sleep(5000);

            renameLatestPdf(outputDir);

        } catch (Exception e) {
            System.err.println("Ошибка выполнения: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    private static class xPaths {
        static final String artist = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input";
        static final String title = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input";
        static final String trackLeft = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[%d]/td[2]/input";
        static final String trackRight = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[%d]/td[2]/input";
        static final String caseType = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]";
        static final String paperFormat = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]";
        static final String submit = "/html/body/table[2]/tbody/tr/td[1]/div/form/p/input";
    }

    private static Map<String, Object> parseCdData() throws IOException {
        Map<String, Object> cdMap = new HashMap<>();
        List<String> trackList = new ArrayList<>();

        Path inputFile = Paths.get("C:\\st8\\my-app\\data\\data.txt");

        try (BufferedReader br = Files.newBufferedReader(inputFile)) {
            String line;
            boolean readTracks = false;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("Artist:")) {
                    cdMap.put("Artist", line.substring(7).trim());
                    readTracks = false;
                } else if (line.startsWith("Title:")) {
                    cdMap.put("Title", line.substring(6).trim());
                    readTracks = false;
                } else if (line.equalsIgnoreCase("Tracks:")) {
                    readTracks = true;
                } else if (readTracks) {
                    trackList.add(line);
                }
            }
        }

        if (!cdMap.containsKey("Artist") || !cdMap.containsKey("Title") || trackList.isEmpty()) {
            throw new IOException("Ошибка: неполные данные в data.txt");
        }

        cdMap.put("Tracks", trackList);
        return cdMap;
    }

    private static void renameLatestPdf(Path directory) {
        try {
            Optional<Path> latest = Files.list(directory)
                    .filter(path -> !Files.isDirectory(path) && path.toString().endsWith(".pdf"))
                    .max(Comparator.comparingLong(path -> path.toFile().lastModified()));

            if (latest.isEmpty()) {
                System.out.println("PDF не найден в папке " + directory);
                return;
            }

            Path newName = directory.resolve("cd.pdf");
            Files.deleteIfExists(newName);
            Files.copy(latest.get(), newName);
            Files.deleteIfExists(latest.get());

            System.out.println("Файл PDF переименован в cd.pdf");
        } catch (IOException e) {
            System.err.println("Ошибка при работе с PDF: " + e.getMessage());
        }
    }
}
