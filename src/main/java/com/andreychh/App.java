package com.andreychh;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.BufferedReader;
import java.io.FileReader;
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
        String downloadDir = "%s/result".formatted(System.getProperty("user.dir"));

        System.setProperty("webdriver.chrome.driver", "/Users/andrey-ch/Downloads/chromedriver-mac-x64/chromedriver");

        ChromeOptions options = new ChromeOptions();
        options.setBinary("/Users/andrey-ch/Downloads/chrome-mac-x64/Google Chrome for Testing.app/Contents/MacOS/Google Chrome for Testing");

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadDir);
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);
        try {
            String artist = "";
            String title = "";
            List<String> tracks = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader("data/data.txt"))) {
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    } else if (line.startsWith("Artist:")) {
                        artist = line.substring(7).trim();
                    } else if (line.startsWith("Title:")) {
                        title = line.substring(6).trim();
                    } else if (line.startsWith("Track")) {
                        tracks.add(line.substring(line.indexOf(":") + 1).trim());
                    }
                }
            }

            driver.get("https://www.papercdcase.com/");

            driver.findElement(
                    By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input")
            ).sendKeys(artist);

            driver.findElement(
                    By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input")
            ).sendKeys(title);

            for (int i = 0; i < tracks.size() && i < 16; i++) {
                driver.findElement(
                        By.xpath(
                                "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[%d]/table/tbody/tr[%d]/td[2]/input".formatted(
                                        i / 8 + 1,
                                        i % 8 + 1
                                )
                        )
                ).sendKeys(tracks.get(i));
            }

            driver.findElement(
                    By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]")
            ).click();

            driver.findElement(
                    By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]")
            ).click();

            driver.findElement(
                    By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input")
            ).click();

            Thread.sleep(5000);

            Path resultPath = Paths.get(downloadDir);
            Files.move(
                    resultPath.resolve("papercdcase.pdf"),
                    resultPath.resolve("cd.pdf"),
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (Exception e) {
            throw new RuntimeException("Error during task execution", e);
        } finally {
            driver.quit();
        }
    }
}
