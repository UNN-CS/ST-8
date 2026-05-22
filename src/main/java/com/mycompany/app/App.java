package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.time.Duration;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.*;
import java.util.*;

public class App 
{
    private static final String BASE_URL = "http://www.papercdcase.com";
    private static final int MAX_TRACKS = 16;

    public static void main(String[] args) throws Exception
    {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--ignore-certificate-errors");
        
        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        
        try {
            driver.get(BASE_URL + "/index.php");
            
            List<String> lines = Files.readAllLines(Paths.get("data/data.txt"));
            String artist = lines.get(0).trim();
            String title = lines.get(1).trim();
            List<String> tracks = new ArrayList<>();
            for (int i = 2; i < lines.size() && tracks.size() < MAX_TRACKS; i++) {
                String line = lines.get(i).trim();
                if (!line.isEmpty()) {
                    tracks.add(line);
                }
            }
            
            System.out.println("Artist: " + artist);
            System.out.println("Title: " + title);
            System.out.println("Tracks count: " + tracks.size());
            
            WebElement artistField = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@name='artist']"))
            );
            artistField.clear();
            artistField.sendKeys(artist);
            
            WebElement titleField = driver.findElement(By.xpath("//input[@name='title']"));
            titleField.clear();
            titleField.sendKeys(title);
            
            for (int i = 0; i < tracks.size(); i++) {
                String trackName = "track" + (i + 1);
                WebElement trackField = driver.findElement(By.xpath("//input[@name='" + trackName + "']"));
                trackField.clear();
                trackField.sendKeys(tracks.get(i));
            }
            
            WebElement a4Radio = driver.findElement(By.xpath("//input[@name='size' and @value='a4']"));
            if (!a4Radio.isSelected()) a4Radio.click();
            
            WebElement jewelRadio = driver.findElement(By.xpath("//input[@name='template' and @value='jewel']"));
            if (!jewelRadio.isSelected()) jewelRadio.click();
            
            StringBuilder pdfUrl = new StringBuilder(BASE_URL);
            pdfUrl.append("/papercdcase.cgi/papercdcase.pdf?");
            pdfUrl.append("artist=").append(URLEncoder.encode(artist, "UTF-8"));
            pdfUrl.append("&title=").append(URLEncoder.encode(title, "UTF-8"));
            for (int i = 0; i < tracks.size(); i++) {
                pdfUrl.append("&track").append(i + 1).append("=")
                      .append(URLEncoder.encode(tracks.get(i), "UTF-8"));
            }
            pdfUrl.append("&template=jewel");
            pdfUrl.append("&size=a4");
            pdfUrl.append("&lang=west");
            
            System.out.println("PDF URL: " + pdfUrl);
            
            Files.createDirectories(Paths.get("result"));
            downloadPdf(pdfUrl.toString(), Paths.get("result/cd.pdf"));
            
            System.out.println("PDF сохранён: result/cd.pdf");
            
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
    
    private static void downloadPdf(String urlString, Path outputPath) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        
        int responseCode = conn.getResponseCode();
        System.out.println("HTTP Response: " + responseCode);
        
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
            }
            System.out.println("Downloaded: " + outputPath.toAbsolutePath() + " (" + Files.size(outputPath) + " bytes)");
        } else {
            throw new IOException("HTTP error: " + responseCode);
        }
        conn.disconnect();
    }
}