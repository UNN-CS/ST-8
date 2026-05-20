package com.mycompany.app;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.time.Duration;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.net.*;

public class App {
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\nyuti\\repos\\ST-7\\chromedriver.exe");

        WebDriver webDriver = new ChromeDriver();

        try {
            Map<String, Object> data = loadData();
            String artist = (String) data.get("artist");
            String title = (String) data.get("title");
            @SuppressWarnings("unchecked")
            List<String> tracks = (List<String>) data.get("tracks");

            webDriver.get("http://www.papercdcase.com");

            fillForm(webDriver, artist, title, tracks);

            webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input")).submit();

            downloadPdf(webDriver);

        } catch (Exception e) {
            System.out.println("Error: " + e.toString());
            e.printStackTrace();
        } finally {
            webDriver.quit();
        }
    }

    private static Map<String, Object> loadData() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get("data/data.txt"));
        String artist = lines.get(0).split(": ")[1];
        String title = lines.get(1).split(": ")[1];
        List<String> tracks = new ArrayList<>();
        for (int i = 3; i < lines.size(); i++) {
            tracks.add(lines.get(i));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("artist", artist);
        data.put("title", title);
        data.put("tracks", tracks);

        return data;
    }

    private static void fillForm(WebDriver webDriver, String artist, String title, List<String> tracks) {
        webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input")).sendKeys(artist);
        webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input")).sendKeys(title);

        int trackIndex = 0;
        for (int column = 1; column <= 2 && trackIndex < tracks.size(); column++) {
            for (int row = 1; row <= 8 && trackIndex < tracks.size(); row++) {
                String xpath = String.format(
                    "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[%d]/table/tbody/tr[%d]/td[2]/input",
                    column, row
                );
                webDriver.findElement(By.xpath(xpath)).sendKeys(tracks.get(trackIndex++));
            }
        }

        webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]")).click();
        webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]")).click();
    }

    private static void downloadPdf(WebDriver webDriver) throws IOException {
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains("papercdcase.pdf"));

        String pdfUrl = webDriver.getCurrentUrl();

        try (InputStream in = URI.create(pdfUrl).toURL().openStream()) {
            Files.copy(in, Paths.get("result/cd.pdf"));
        }
    }
}