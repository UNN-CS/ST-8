package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Hello world!
 */
public class App {
    private static final String DRIVER = "/Users/rdtyworldd/Downloads/chromedriver-mac-arm64/chromedriver";
    private static final String DATA_PATH = "/Users/rdtyworldd/ST-8/data/data.txt";
    private static final String WEB_PATH = "http://www.papercdcase.com";

    private static final String ARTIST_FIELD = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input";
    private static final String ALBUM_FIELD = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input";
    private static final String FORMAT = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]";
    private static final String PAPER = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]";

    private static final String LEFT_TABLE_TRACKS = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[";
    private static final String RIGHT_TABLE_TRACKS = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[";
    private static final String TRACKS_END = "]/td[2]/input";
    private static final String CONFIRM_BUTTON = "/html/body/table[2]/tbody/tr/td[1]/div/form/p/input";

    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", DRIVER);
        WebDriver webDriver = new ChromeDriver();
        try {
            webDriver.get(WEB_PATH);
            WebElement artist = webDriver.findElement(By.xpath(ARTIST_FIELD));
            WebElement album = webDriver.findElement(By.xpath(ALBUM_FIELD));

            List<WebElement> left_table = new ArrayList<>();
            List<WebElement> right_table = new ArrayList<>();

            getTrackElements(left_table, LEFT_TABLE_TRACKS, webDriver);
            getTrackElements(right_table, RIGHT_TABLE_TRACKS, webDriver);


            WebElement format = webDriver.findElement(By.xpath(FORMAT));
            WebElement paper = webDriver.findElement(By.xpath(PAPER));
            WebElement confirm = webDriver.findElement(By.xpath(CONFIRM_BUTTON));

            String[] data = getData();

            artist.sendKeys(data[0]);
            album.sendKeys(data[1]);
            setTracksLeft(left_table, data);
            setTracksRight(right_table, data);

            format.submit();
            paper.submit();
            confirm.submit();

        } catch (Exception e) {
            System.out.println("Error");
            System.out.println(e.toString());
        }
    }

    private static void getTrackElements(List<WebElement> list, String path, WebDriver webDriver) {
        for(int i = 1; i <= 8; i++) {
            String xpath = path + i + TRACKS_END;
            list.add(webDriver.findElement(By.xpath(xpath)));
        }
    }

    private static String[] getData() {
        String[] linesArray = null;
        try (Stream<String> stream = Files.lines(Paths.get(DATA_PATH))) {
            linesArray = stream.toArray(String[]::new);

        } catch (IOException e) {
            System.err.println("Ошибка при чтении файла: " + e.getMessage());
        }
        return linesArray;
    }

    private static void setTracksLeft(List<WebElement> tracks, String[] data) {
        int last = 2 + 8;
        if (last > data.length){
            last = data.length;
        }

        for(int i = 2; i < last; i++) {
            tracks.get(i - 2).sendKeys(data[i]);
        }
    }

    private static void setTracksRight(List<WebElement> tracks, String[] data) {
        for(int i = 10; i < data.length; i++) {
            tracks.get(i - 10).sendKeys(data[i]);
        }
    }
}
