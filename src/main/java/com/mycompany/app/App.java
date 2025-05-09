package com.mycompany.app;

import org.apache.commons.io.FileUtils;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.FluentWait;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public class App {
    static Path downloadPath = Path.of(System.getProperty("user.dir") + File.separator +"result");
    static Path downloadFilePath = Path.of(System.getProperty("user.dir") + File.separator +"result" + File.separator + "papercdcase.pdf");
    static Path resultFilePath = Path.of(System.getProperty("user.dir") + File.separator +"result" + File.separator + "cd.pdf");
    static Path dataPath = Path.of("data/data.txt");
    static String url = "http://www.papercdcase.com/index.php";

    public static void main(String[] args) throws IOException {
        Album album = new Album(dataPath);

        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadPath.toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);

        Logger.getLogger("org.openqa.selenium").setLevel(Level.OFF);
        WebDriver driver = new ChromeDriver(options);

        driver.get(url);

        WebElement artistField = driver.findElement(
                By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input"));
        artistField.sendKeys(album.getArtistsString());

        WebElement titleField = driver.findElement(
                By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input"));
        titleField.sendKeys(album.getTitle());

        ArrayList<String> tracks = album.getTracks();

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

        WebElement typeRadioButton = driver.findElement(
                By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]"));
        typeRadioButton.click();

        WebElement paperRadioButton = driver.findElement(
                By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]"));
        paperRadioButton.click();

        WebElement forceDownloadCheckbox = driver.findElement(
                By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[7]/td[2]/input"));
        forceDownloadCheckbox.click();

        WebElement submitButton = driver.findElement(
                By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input"));
        submitButton.submit();


        FluentWait<WebDriver> wait = new FluentWait<WebDriver>(driver).withTimeout(Duration.ofSeconds(5)).pollingEvery(Duration.ofMillis(150));
        try {
            wait.until(x -> downloadFilePath.toFile().exists());
        } catch (TimeoutException e) {
            System.out.println("not downloaded? whyyy?!?!");
            driver.quit();
            return;
        }

        driver.quit();
        if (resultFilePath.toFile().exists())
            Files.delete(resultFilePath);
        Files.move(downloadFilePath, resultFilePath);
        System.out.println("file downloaded");
    }
}
