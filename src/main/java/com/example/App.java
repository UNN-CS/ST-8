package com.example;

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
import java.util.stream.IntStream;

public class CDCoverGenerator {
    private static final String WEB_DRIVER_PATH = "C:\\WebDrivers\\chromedriver-win64\\chromedriver.exe";
    private static final String RESULT_DIR = System.getProperty("user.dir") + "/generated_covers";
    private static final String DATA_FILE = "resources/cd_info.txt";
    private static final String TARGET_URL = "https://www.papercdcase.com/index.php";

    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", WEB_DRIVER_PATH);
        setupDownloadDirectory();

        ChromeOptions options = configureChromeOptions();
        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get(TARGET_URL);
            Map<String, Object> cdData = readCDData();
            populateFormFields(driver, cdData);
            customizePreferences(driver);
            submitForm(driver);
            processDownloadedFile();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    private static void setupDownloadDirectory() {
        Path resultDir = Paths.get(RESULT_DIR);
        try {
            Files.createDirectories(resultDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ChromeOptions configureChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", RESULT_DIR);
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);
        return options;
    }

    private static Map<String, Object> readCDData() throws IOException {
        Map<String, Object> data = new HashMap<>();
        List<String> tracks = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            boolean readingTracks = false;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Artist:")) {
                    data.put("Artist", line.substring("Artist:".length()).trim());
                } else if (line.startsWith("Title:")) {
                    data.put("Title", line.substring("Title:".length()).trim());
                } else if (line.startsWith("Tracks:")) {
                    readingTracks = true;
                } else if (readingTracks) {
                    String trackText = line.trim();
                    if (!trackText.isEmpty()) {
                        int firstSpacePos = trackText.indexOf(' ');
                        if (firstSpacePos > 0) {
                            trackText = trackText.substring(firstSpacePos + 1);
                        }
                        tracks.add(trackText);
                    }
                }
            }
        }

        data.put("Tracks", tracks);
        return data;
    }

    private static void populateFormFields(WebDriver driver, Map<String, Object> cdData) {
        sendKeysToField(driver, "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input", (String) cdData.get("Artist"));
        sendKeysToField(driver, "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input", (String) cdData.get("Title"));

        @SuppressWarnings("unchecked")
        List<String> tracks = (List<String>) cdData.get("Tracks");
        IntStream.range(0, Math.min(8, tracks.size()))
                .forEach(i -> sendKeysToField(driver, "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[" + (i + 1) + "]/td[2]/input", tracks.get(i)));

        IntStream.range(8, Math.min(16, tracks.size()))
                .forEach(i -> sendKeysToField(driver, "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[" + (i - 7) + "]/td[2]/input", tracks.get(i)));
    }

    private static void sendKeysToField(WebDriver driver, String xpath, String value) {
        WebElement element = driver.findElement(By.xpath(xpath));
        element.sendKeys(value);
    }

    private static void customizePreferences(WebDriver driver) {
        selectRadioIfNotSelected(driver, "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]");
        selectRadioIfNotSelected(driver, "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]");
    }

    private static void selectRadioIfNotSelected(WebDriver driver, String xpath) {
        WebElement radio = driver.findElement(By.xpath(xpath));
        if (!radio.isSelected()) {
            radio.click();
        }
    }

    private static void submitForm(WebDriver driver) {
        WebElement submitButton = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input"));
        submitButton.click();
    }

    private static void processDownloadedFile() {
        Path resultDir = Paths.get(RESULT_DIR);
        try {
            Path[] pdfFiles = Files.list(resultDir)
                    .filter(file -> !Files.isDirectory(file) && file.toString().endsWith(".pdf"))
                    .toArray(Path[]::new);

            if (pdfFiles.length > 0) {
                Path latestFile = findLatestFile(pdfFiles);
                renameAndCleanUp(resultDir, latestFile);
            } else {
                Files.list(resultDir).forEach(p -> System.out.println(" - " + p.getFileName()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Path findLatestFile(Path[] files) throws IOException {
        Path latestFile = files[0];
        long latestTime = Files.getLastModifiedTime(latestFile).toMillis();

        for (int i = 1; i < files.length; i++) {
            long fileTime = Files.getLastModifiedTime(files[i]).toMillis();
            if (fileTime > latestTime) {
                latestTime = fileTime;
                latestFile = files[i];
            }
        }
        return latestFile;
    }

    private static void renameAndCleanUp(Path resultDir, Path latestFile) throws IOException {
        Path newFilePath = resultDir.resolve("cd.pdf");
        if (Files.exists(newFilePath)) {
            Files.delete(newFilePath);
        }

        Files.copy(latestFile, newFilePath);
        Files.delete(latestFile);
    }
}