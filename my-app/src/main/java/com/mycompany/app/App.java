package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class App {
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "C:/WebDrivers/chromedriver-win64/chromedriver.exe");

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", Paths.get("result").toAbsolutePath().toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);

        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get("http://www.papercdcase.com/index.php");

            driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

            String artist = "";
            String title = "";
            Map<Integer, String> tracks = new HashMap<>();

            try (BufferedReader br = new BufferedReader(new FileReader("data/data.txt"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("Artist:")) {
                        artist = line.substring("Artist:".length()).trim();
                    } else if (line.startsWith("Title:")) {
                        title = line.substring("Title:".length()).trim();
                    } else if (line.startsWith("Track ")) {
                        try {
                            String[] parts = line.split(":", 2);
                            if (parts.length == 2) {
                                String trackPart = parts[0].trim();
                                String trackName = parts[1].trim();
                                int trackNumber = Integer.parseInt(trackPart.substring("Track ".length()));
                                if (trackNumber >= 1 && trackNumber <= 16) {
                                    tracks.put(trackNumber, trackName);
                                }
                            }
                        } catch (NumberFormatException e) {
                           System.err.println("Error parsing track number: " + line);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Error reading data/data.txt");
                return;
            }

            if (artist.isEmpty() || title.isEmpty()) {
                System.err.println("Artist or Title not found in data/data.txt");
                return;
            }

            WebElement artistField = driver.findElement(By.xpath("//input[@name='artist']"));
            artistField.sendKeys(artist);

            WebElement titleField = driver.findElement(By.xpath("//input[@name='title']"));
            titleField.sendKeys(title);

            for (Map.Entry<Integer, String> entry : tracks.entrySet()) {
                int trackNum = entry.getKey();
                String trackName = entry.getValue();
                try {
                    WebElement trackField = driver.findElement(By.xpath("//input[@name='track" + trackNum + "']"));
                    trackField.sendKeys(trackName);
                } catch (Exception e) {
                     System.err.println("Could not find field for Track " + trackNum);
                }
            }

            WebElement paperA4Radio = driver.findElement(By.xpath("//input[@name='size' and @value='a4']"));
            if (!paperA4Radio.isSelected()) {
                paperA4Radio.click();
            }

            WebElement typeJewelCaseRadio = driver.findElement(By.xpath("//input[@name='template' and @value='jewel']"));
            if (!typeJewelCaseRadio.isSelected()) {
                typeJewelCaseRadio.click();
            }

            WebElement generateButton = driver.findElement(By.xpath("//input[@name='submit']"));
            generateButton.click();

            TimeUnit.SECONDS.sleep(10);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}