package org.example;


import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class App {
    public static void main(String[] args) {

        String artist = "";
        String title = "";
        List<String> tracks = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader("data/data.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Artist: ")) {
                    artist = line.substring("Artist: ".length());
                } else if (line.startsWith("Title: ")) {
                    title = line.substring("Title: ".length());
                } else if (line.startsWith("Track: ")) {
                    tracks.add(line.substring("Track: ".length()));
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка при чтении файла: " + e.getMessage());
        }

        System.out.println("Artist: " + artist);
        System.out.println("Title: " + title);
        System.out.println("Tracks:");
        for (int i = 0; i < tracks.size(); i++) {
            System.out.println((i + 1) + ": " + tracks.get(i));
        }

        System.setProperty("webdriver.chrome.driver", "C:\\WebDriver\\chromedriver.exe");
        WebDriver webDriver = new ChromeDriver();
        try {
            webDriver.get("https://www.papercdcase.com/index.php");
            webDriver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
            webDriver
                    .findElement(By.xpath("//input[@name='artist']"))
                    .sendKeys(artist);
            webDriver
                    .findElement(By.xpath("//input[@name='title']"))
                    .sendKeys(title);
            IntStream.range(0, tracks.size())
                    .forEach(i -> webDriver.findElement(By.xpath("//input[@name='track" + (i + 1) + "']"))
                                    .sendKeys(tracks.get(i)));
            webDriver
                    .findElement(By.xpath("//input[@name='template' and @value='jewel']"))
                    .click();
            webDriver
                    .findElement(By.xpath("//input[@name='size' and @value='a4']"))
                    .click();
            webDriver
                    .findElement(By.xpath("//input[@name='submit']"))
                    .click();
        } catch (Exception e) {
            System.out.println("Error");
        }

    }
}