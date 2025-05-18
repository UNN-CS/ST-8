package com.mycompany.app;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

public class App {
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "C:\\driver\\chromedriver-win64\\chromedriver.exe");
        WebDriver driver = new ChromeDriver();

        try {
            Map<String, String> data = readData("data/data.txt");

            driver.get("http://www.papercdcase.com/index.php");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            WebElement artistField = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//input[@name='artist']")));
            artistField.sendKeys(data.getOrDefault("Artist", ""));

            WebElement titleField = driver.findElement(By.xpath("//input[@name='title']"));
            titleField.sendKeys(data.getOrDefault("Title", ""));

            for (int i = 1; i <= 18; i++) {
                String key = "Track" + i;
                if (data.containsKey(key)) {
                    WebElement trackField = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//input[@name='track" + i + "']")));
                    trackField.sendKeys(data.get(key));
                }
            }

            WebElement a4Radio = driver.findElement(By.xpath("//input[@name='size'][@value='a4']"));
            a4Radio.click();

            WebElement jewelRadio = driver.findElement(By.xpath("//input[@name='template'][@value='jewel']"));
            jewelRadio.click();

            WebElement submitButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//input[@name='submit']")));
            submitButton.click();

            Thread.sleep(5000);

            downloadPdf(data);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    private static Map<String, String> readData(String filePath) {
        Map<String, String> data = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    data.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    private static void downloadPdf(Map<String, String> data) throws IOException {
        StringBuilder urlBuilder = new StringBuilder("https://www.papercdcase.com/papercdcase.cgi/papercdcase.pdf?");
        urlBuilder.append("artist=").append(URLEncoder.encode(data.getOrDefault("Artist", ""), "UTF-8"));
        urlBuilder.append("&title=").append(URLEncoder.encode(data.getOrDefault("Title", ""), "UTF-8"));

        for (int i = 1; i <= 18; i++) {
            String key = "Track" + i;
            if (data.containsKey(key)) {
                urlBuilder.append("&track").append(i).append("=")
                        .append(URLEncoder.encode(data.get(key), "UTF-8"));
            }
        }

        urlBuilder.append("&size=a4&template=jewel");

        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Accept", "application/pdf");
        conn.connect();

        int code = conn.getResponseCode();
        String contentType = conn.getContentType();
        System.out.println("HTTP " + code + ", Content-Type: " + contentType);

        if (code == 200 && contentType.startsWith("application/pdf")) {
            Path output = Paths.get("result", "cd.pdf");
            Files.createDirectories(output.getParent());
            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, output, StandardCopyOption.REPLACE_EXISTING);
            }
            System.out.println("PDF сохранён в: " + output.toAbsolutePath());
        } else {
            System.err.println("Ошибка при скачивании PDF");
        }

        conn.disconnect();
    }
}
