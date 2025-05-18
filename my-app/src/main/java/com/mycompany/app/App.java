package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Select;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class App {
    public static void main(String[] args) {
        try {
            String artist = "";
            String album = "";
            List<String> tracks = new ArrayList<>();
            
            try (BufferedReader reader = new BufferedReader(new FileReader("data/data.txt"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("Artist:")) {
                        artist = line.substring(7).trim();
                    } else if (line.startsWith("Album:")) {
                        album = line.substring(6).trim();
                    } else if (line.matches("\\d+\\..*")) {
                        tracks.add(line.substring(line.indexOf(".") + 1).trim());
                    }
                }
            }

            System.setProperty("webdriver.chrome.driver", "chromedriver-win64/chromedriver.exe");
            
            ChromeOptions options = new ChromeOptions();
            HashMap<String, Object> chromePrefs = new HashMap<>();
            chromePrefs.put("profile.default_content_settings.popups", 0);
            chromePrefs.put("download.default_directory", Paths.get("result").toAbsolutePath().toString());
            chromePrefs.put("download.prompt_for_download", false);
            chromePrefs.put("plugins.always_open_pdf_externally", true);
            options.setExperimentalOption("prefs", chromePrefs);
            
            WebDriver driver = new ChromeDriver(options);
            driver.get("http://www.papercdcase.com/index.php");

            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input")).sendKeys(artist);
            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input")).sendKeys(album);

            int i = 0;
            for (String track : tracks) {
                String xpath = String.format("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[%d]/table/tbody/tr[%d]/td[2]/input",
                        i / 8 + 1, i % 8 + 1);
                WebElement trackArea = driver.findElement(By.xpath(xpath));
                trackArea.sendKeys(track);
                i++;
            }

            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]")).click();
            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]")).click();
            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input")).click();
            
            Thread.sleep(5000);
            driver.quit();

            Path downloadedFile = Files.list(Paths.get("result"))
                    .filter(file -> file.toString().endsWith(".pdf"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("PDF not found"));
            
            Files.move(downloadedFile, downloadedFile.resolveSibling("cd.pdf"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 