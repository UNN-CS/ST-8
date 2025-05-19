package com.example.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class App {
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\Artem\\ST-8\\my-app\\chromedriver\\chromedriver.exe");

        String artist, title;
        ArrayList<String> tracks = new ArrayList<>();

        try (BufferedReader r = new BufferedReader(new FileReader("./data/data.txt"))) {
            artist = r.readLine();
            title = r.readLine();
            String line;
            while ((line = r.readLine()) != null) {
                tracks.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        ChromeOptions options = new ChromeOptions();
        HashMap<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", new File("./result").getAbsolutePath());
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);
        
        try {
            driver.get("http://www.papercdcase.com/index.php");

            WebElement artistField = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input"));
            artistField.sendKeys(artist);

            WebElement titleField = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input"));
            titleField.sendKeys(title);

            for (int i = 0; i < tracks.size(); i++) {
                WebElement tracksField = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[" + (i + 1) + "]/td[2]/input"));
                tracksField.sendKeys(tracks.get(i));
            }

            WebElement typeJewel = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]"));
            typeJewel.click();

            WebElement paperA4 = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]"));
            paperA4.click();

            WebElement submitButton = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input"));
            submitButton.click();

            Thread.sleep(20000);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}