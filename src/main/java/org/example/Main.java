package org.example;


import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) {

        String artist = "";
        String title = "";
        List<String> tracks = new ArrayList<>();

        Pattern artistPattern = Pattern.compile("^Artist:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
        Pattern titlePattern = Pattern.compile("^Title:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
        Pattern trackPattern = Pattern.compile("^Track\\s*\\d+:\\s*(.+)$", Pattern.CASE_INSENSITIVE);

        try (BufferedReader reader = new BufferedReader(new FileReader("data/data.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher artistMatcher = artistPattern.matcher(line);
                Matcher titleMatcher = titlePattern.matcher(line);
                Matcher trackMatcher = trackPattern.matcher(line);

                if (artistMatcher.find()) {
                    artist = artistMatcher.group(1).trim();
                } else if (titleMatcher.find()) {
                    title = titleMatcher.group(1).trim();
                } else if (trackMatcher.find()) {
                    tracks.add(trackMatcher.group(1).trim());
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка при чтении файла: " + e.getMessage());
        }

        System.setProperty("webdriver.chrome.driver", "C:\\WebDriver\\chromedriver.exe");
        WebDriver webDriver = new ChromeDriver();
        try {
            webDriver.get("https://www.papercdcase.com/index.php");
            webDriver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);


            WebElement artistField = webDriver.findElement(By.xpath("//input[@name='artist']"));
            artistField.sendKeys(artist);

            WebElement titleField = webDriver.findElement(By.xpath("//input[@name='title']"));
            titleField.sendKeys(title);


            for (int i = 0; i < tracks.size(); i++) {
                WebElement trackField = webDriver.findElement(By.xpath("//input[@name='track" + (i + 1) + "']"));
                trackField.sendKeys(tracks.get(i));
            }

            WebElement paperA4Radio = webDriver.findElement(By.xpath("//input[@name='size' and @value='a4']"));
            paperA4Radio.click();

            WebElement typeJewelCaseRadio = webDriver.findElement(By.xpath("//input[@name='template' and @value='jewel']"));
            typeJewelCaseRadio.click();

            WebElement generateButton = webDriver.findElement(By.xpath("//input[@name='submit']"));
            generateButton.click();

            TimeUnit.SECONDS.sleep(10);

        } catch (Exception e) {
            System.out.println("Error");
            System.out.println(e.toString());
        }
        finally {
            webDriver.quit();
        }
    }
}