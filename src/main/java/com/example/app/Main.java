package com.example.app;

import com.example.app.Entity.MusicAlbum;
import com.google.gson.Gson;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class Main {
    static final String ARTIST = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input";
    static final String TITLE = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input";
    static final String TRACK_LEFT = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[%d]/td[2]/input";
    static final String TRACK_RIGHT = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[%d]/td[2]/input";
    static final String CASE_TYPE = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]";
    static final String PAPER_FORMAT = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]";
    static final String SUBMIT = "/html/body/table[2]/tbody/tr/td[1]/div/form/p/input";

    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\egorm\\Downloads\\chromedriver-win64\\chromedriver.exe");
        Path outputDir = Paths.get("result");
        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = Map.of(
                "download.default_directory", outputDir.toString(),
                "download.prompt_for_download", false,
                "plugins.always_open_pdf_externally", true
        );
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);

        try {
            String content = Files.readString(Paths.get("data/data.txt"));
            Gson gson = new Gson();
            MusicAlbum album = gson.fromJson(content, MusicAlbum.class);

            driver.manage().window().maximize();
            driver.get("https://www.papercdcase.com/index.php");

            driver.findElement(By.xpath(ARTIST)).sendKeys(album.getArtistName());
            driver.findElement(By.xpath(TITLE)).sendKeys(album.getTitle());

            List<String> allTracks = album.getNameSongs();

            List<String> leftTracks = allTracks.subList(0, Math.min(8, allTracks.size()));
            List<String> rightTracks = allTracks.size() > 8 ? allTracks.subList(8, Math.min(16, allTracks.size())) : List.of();

            for (int i = 0; i < leftTracks.size(); i++) {
                String xpath = String.format(TRACK_LEFT, i + 1);
                driver.findElement(By.xpath(xpath)).sendKeys(leftTracks.get(i));
            }

            for (int i = 0; i < rightTracks.size(); i++) {
                String xpath = String.format(TRACK_RIGHT, i + 1);
                driver.findElement(By.xpath(xpath)).sendKeys(rightTracks.get(i));
            }

            WebElement jewelCase = driver.findElement(By.xpath(CASE_TYPE));
            if (!jewelCase.isSelected()) jewelCase.click();

            WebElement a4Paper = driver.findElement(By.xpath(PAPER_FORMAT));
            if (!a4Paper.isSelected()) a4Paper.click();

            driver.findElement(By.xpath(SUBMIT)).click();
            Thread.sleep(5000);


        } catch (Exception e) {
            System.err.println("Ошибка выполнения: " + e.getMessage());
        } finally {
            driver.quit();
        }
    }
}
