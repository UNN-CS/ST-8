package com.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class App {
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\ajlak\\Downloads\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");
        
        ChromeOptions options = new ChromeOptions();
        options.setBinary("C:\\Users\\ajlak\\Downloads\\chrome-win64\\chrome-win64\\chrome.exe");
        
        String downloadPath = System.getProperty("user.dir") + "\\result";
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--ignore-ssl-errors");
        options.setAcceptInsecureCerts(true);
        
        options.setExperimentalOption("prefs", new java.util.HashMap<String, Object>() {{
            put("download.default_directory", downloadPath);
            put("download.prompt_for_download", false);
            put("download.directory_upgrade", true);
            put("safebrowsing.enabled", false);
        }});
        
        WebDriver driver = new ChromeDriver(options);
        
        try {
            driver.get("http://www.papercdcase.com");
            driver.manage().window().maximize();
            driver.manage().timeouts().implicitlyWait(15, TimeUnit.SECONDS);
            
            Thread.sleep(3000);
            
            List<String> lines = Files.readAllLines(Paths.get("data/data.txt"));
            String artist = lines.get(0);
            String title = lines.get(1);
            
            System.out.println("=== FILLING FORM ===");
            System.out.println("Artist: " + artist);
            System.out.println("Title: " + title);
            
            WebElement artistField = driver.findElement(By.name("artist"));
            artistField.clear();
            artistField.sendKeys(artist);
            System.out.println("Artist filled");
            
            WebElement titleField = driver.findElement(By.name("title"));
            titleField.clear();
            titleField.sendKeys(title);
            System.out.println("Title filled");
            
            int trackNumber = 1;
            for (int i = 2; i < lines.size() && i < 20; i++) {
                String trackName = lines.get(i);
                if (!trackName.trim().isEmpty()) {
                    WebElement trackField = driver.findElement(By.name("track" + trackNumber));
                    trackField.clear();
                    trackField.sendKeys(trackName);
                    System.out.println("Track " + trackNumber + ": " + trackName);
                    trackNumber++;
                }
            }
            
            List<WebElement> templateRadios = driver.findElements(By.name("template"));
            for (WebElement radio : templateRadios) {
                String value = radio.getAttribute("value");
                if (value != null && value.toLowerCase().contains("jewel")) {
                    radio.click();
                    System.out.println("Jewel Case selected");
                    break;
                }
            }
            
            try {
                List<WebElement> sizeRadios = driver.findElements(By.name("size"));
                for (WebElement radio : sizeRadios) {
                    if ("A4".equals(radio.getAttribute("value"))) {
                        radio.click();
                        System.out.println("A4 format selected");
                        break;
                    }
                }
            } catch (Exception e) {
                System.out.println("A4 format not found, using default");
            }
            
            WebElement submitBtn = driver.findElement(By.name("submit"));
            submitBtn.click();
            System.out.println("Submit button clicked");
            
            System.out.println("\nWaiting for PDF download...");
            Thread.sleep(10000);
            
            File resultDir = new File("result");
            File[] pdfFiles = resultDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
            
            if (pdfFiles != null && pdfFiles.length > 0) {
                File latestPdf = pdfFiles[0];
                for (File pdf : pdfFiles) {
                    if (pdf.lastModified() > latestPdf.lastModified()) {
                        latestPdf = pdf;
                    }
                }
                
                File targetFile = new File("result/cd.pdf");
                if (!latestPdf.getName().equals("cd.pdf")) {
                    Files.move(latestPdf.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                System.out.println("\nPDF saved to result/cd.pdf");
                System.out.println("File size: " + targetFile.length() + " bytes");
            } else {
                System.out.println("\nPDF file not found in result folder");
                
                String currentUrl = driver.getCurrentUrl();
                System.out.println("PDF download URL: " + currentUrl);
                Files.write(Paths.get("result/pdf_url.txt"), currentUrl.getBytes());
                System.out.println("URL saved to result/pdf_url.txt");
                
                Files.write(Paths.get("result/final_page.html"), driver.getPageSource().getBytes());
            }
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            
            try {
                if (driver != null) {
                    Files.write(Paths.get("result/error_page.html"), driver.getPageSource().getBytes());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            driver.quit();
        }
    }
}