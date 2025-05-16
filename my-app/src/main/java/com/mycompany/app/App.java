package com.mycompany.app;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class App {
    private static final String CHROME_DRIVER_PATH = "C:\\Program Files\\Google Chrome Driver\\chromedriver.exe";

    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH);
        
        Path resultDir = Paths.get("..", "result").toAbsolutePath().normalize();
        prepareResultDirectory(resultDir);

        ChromeOptions chromeOptions = configureChromeOptions(resultDir.toString());
        WebDriver driver = new ChromeDriver(chromeOptions);

        try {
            processCdCoverCreation(driver, resultDir);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    private static void prepareResultDirectory(Path resultDir) {
        try {
            Files.createDirectories(resultDir);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static ChromeOptions configureChromeOptions(String downloadPath) {
        ChromeOptions options = new ChromeOptions();
        Map<String, Object> preferences = new HashMap<>();
        preferences.put("download.default_directory", downloadPath);
        preferences.put("download.prompt_for_download", false);
        preferences.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", preferences);
        return options;
    }

    private static void processCdCoverCreation(WebDriver driver, Path resultDir) throws Exception {
        Map<String, Object> cdData = loadCdData();
        driver.manage().window().maximize();
        driver.get("https://www.papercdcase.com/index.php");

        populateForm(driver, cdData);
        submitCdForm(driver);
        Thread.sleep(5000);
        processDownloadedPdf(resultDir);
    }

    private static Map<String, Object> loadCdData() throws IOException {
        Path dataFile = Paths.get("..", "data", "data.txt");
        Map<String, Object> data = new HashMap<>();
        List<String> tracks = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(dataFile)) {
            String line;
            boolean tracksSection = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("Artist:")) {
                    data.put("Artist", line.substring(7).trim());
                } else if (line.startsWith("Title:")) {
                    data.put("Title", line.substring(6).trim());
                } else if (line.equalsIgnoreCase("Tracks:")) {
                    tracksSection = true;
                } else if (tracksSection) {
                    tracks.add(line);
                }
            }
        }

        data.put("Tracks", tracks);
        return data;
    }

    private static void populateForm(WebDriver driver, Map<String, Object> cdData) {
        enterText(driver, "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input", 
                 cdData.get("Artist").toString());
        enterText(driver, "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input", 
                 cdData.get("Title").toString());

        @SuppressWarnings("unchecked")
        List<String> tracks = (List<String>) cdData.get("Tracks");

        for (int i = 0; i < Math.min(16, tracks.size()); i++) {
            String xpath = i < 8 ? 
                "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[" + (i + 1) + "]/td[2]/input" :
                "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[" + (i - 7) + "]/td[2]/input";
            enterText(driver, xpath, tracks.get(i));
        }

        selectRadioOption(driver, "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]");
        selectRadioOption(driver, "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]");
    }

    private static void enterText(WebDriver driver, String xpath, String text) {
        WebElement element = driver.findElement(By.xpath(xpath));
        element.clear();
        element.sendKeys(text);
    }

    private static void selectRadioOption(WebDriver driver, String xpath) {
        WebElement radio = driver.findElement(By.xpath(xpath));
        if (!radio.isSelected()) {
            radio.click();
        }
    }

    private static void submitCdForm(WebDriver driver) {
        driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input")).click();
    }

    private static void processDownloadedPdf(Path resultDir) throws IOException {
        Optional<Path> latestPdf = Files.list(resultDir)
            .filter(p -> p.toString().endsWith(".pdf"))
            .max(Comparator.comparing(p -> {
                try {
                    return Files.getLastModifiedTime(p);
                } catch (IOException e) {
                    return null;
                }
            }));

        if (latestPdf.isPresent()) {
            Path target = resultDir.resolve("cd.pdf");
            Files.deleteIfExists(target);
            Files.move(latestPdf.get(), target);
        }
    }
}