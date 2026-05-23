package com.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Selenium WebDriver application for generating CD cover using papercdcase.com
 */
public class App {
    
    private static final String BASE_URL = "http://www.papercdcase.com";
    private static final String DATA_FILE = "data/data.txt";
    private static final String RESULT_FILE = "result/cd.pdf";
    private static final int MAX_TRACKS = 16; // Site only supports 16 tracks
    
    public static void main(String[] args) {
        WebDriver driver = null;
        
        try {
            // Read data from file
            CDData cdData = readDataFromFile(DATA_FILE);
            System.out.println("Data loaded successfully:");
            System.out.println("Artist: " + cdData.artist);
            System.out.println("Title: " + cdData.title);
            System.out.println("Tracks count: " + cdData.tracks.size());
            
            // Setup Chrome driver
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--remote-allow-origins=*");
            options.addArguments("--disable-blink-features=AutomationControlled");
            
            // Set download directory to result folder
            File resultDir = new File("result").getAbsoluteFile();
            if (!resultDir.exists()) {
                resultDir.mkdirs();
            }
            String downloadPath = resultDir.getAbsolutePath();
            
            java.util.Map<String, Object> prefs = new java.util.HashMap<>();
            prefs.put("download.default_directory", downloadPath);
            prefs.put("download.prompt_for_download", false);
            prefs.put("plugins.always_open_pdf_externally", true);
            prefs.put("download.directory_upgrade", true);
            prefs.put("safebrowsing.enabled", true);
            
            options.setExperimentalOption("prefs", prefs);
            
            driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            
            System.out.println("Opening " + BASE_URL);
            driver.get(BASE_URL);
            
            // Wait for page to load
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.name("artist")));
            
            System.out.println("Page loaded successfully");
            
            // Save page source for debugging
            try (FileWriter writer = new FileWriter("page_source.html")) {
                writer.write(driver.getPageSource());
                System.out.println("Page source saved to page_source.html for debugging");
            } catch (Exception e) {
                System.out.println("Could not save page source: " + e.getMessage());
            }
            
            // Fill in the form
            fillForm(driver, cdData);
            
            System.out.println("Form filled successfully");
            
            // Get the form action URL
            WebElement form = driver.findElement(By.cssSelector("form"));
            String formAction = form.getAttribute("action");
            System.out.println("Form action: " + formAction);
            
            // Build the full URL with parameters
            String pdfUrl = buildPdfUrl(BASE_URL, formAction, cdData);
            System.out.println("PDF URL: " + pdfUrl);
            
            // Download PDF directly
            downloadPDF(pdfUrl, RESULT_FILE);
            System.out.println("PDF saved to: " + RESULT_FILE);
            
            System.out.println("Process completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
                System.out.println("Browser closed");
            }
        }
    }
    
    /**
     * Read CD data from text file
     */
    private static CDData readDataFromFile(String filename) throws IOException {
        CDData data = new CDData();
        List<String> lines = Files.readAllLines(Paths.get(filename));
        
        boolean readingTracks = false;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.isEmpty()) {
                continue;
            }
            
            if (line.startsWith("Artist:")) {
                data.artist = line.substring("Artist:".length()).trim();
            } else if (line.startsWith("Title:")) {
                data.title = line.substring("Title:".length()).trim();
            } else if (line.equals("Tracks:")) {
                readingTracks = true;
            } else if (readingTracks && data.tracks.size() < MAX_TRACKS) {
                data.tracks.add(line);
            }
        }
        
        return data;
    }
    
    /**
     * Fill the form with CD data
     */
    private static void fillForm(WebDriver driver, CDData cdData) {
        // Fill artist field
        WebElement artistField = driver.findElement(By.name("artist"));
        artistField.clear();
        artistField.sendKeys(cdData.artist);
        
        // Fill title field
        WebElement titleField = driver.findElement(By.name("title"));
        titleField.clear();
        titleField.sendKeys(cdData.title);
        
        // Fill track fields (max 16 tracks)
        for (int i = 0; i < cdData.tracks.size() && i < MAX_TRACKS; i++) {
            String trackFieldName = "track" + (i + 1);
            try {
                WebElement trackField = driver.findElement(By.name(trackFieldName));
                trackField.clear();
                trackField.sendKeys(cdData.tracks.get(i));
            } catch (Exception e) {
                System.out.println("Could not fill track " + (i + 1) + ": " + e.getMessage());
            }
        }
        
        // Select paper type (A4)
        try {
            WebElement paperA4 = driver.findElement(By.cssSelector("input[name='size'][value='a4']"));
            if (!paperA4.isSelected()) {
                paperA4.click();
            }
        } catch (Exception e) {
            System.out.println("Could not select A4 paper: " + e.getMessage());
        }
        
        // Select case type (Jewel Case)
        try {
            WebElement jewelCase = driver.findElement(By.cssSelector("input[name='template'][value='jewel']"));
            if (!jewelCase.isSelected()) {
                jewelCase.click();
            }
        } catch (Exception e) {
            System.out.println("Could not select Jewel Case: " + e.getMessage());
        }
    }
    
    /**
     * Build PDF URL with form parameters
     */
    private static String buildPdfUrl(String baseUrl, String formAction, CDData cdData) throws java.io.UnsupportedEncodingException {
        StringBuilder url = new StringBuilder();
        
        // Handle relative URL
        if (formAction.startsWith("/")) {
            url.append(baseUrl).append(formAction);
        } else if (formAction.startsWith("http")) {
            url.append(formAction);
        } else {
            url.append(baseUrl).append("/").append(formAction);
        }
        
        url.append("?");
        
        // Add parameters
        url.append("artist=").append(java.net.URLEncoder.encode(cdData.artist, "UTF-8"));
        url.append("&title=").append(java.net.URLEncoder.encode(cdData.title, "UTF-8"));
        
        // Add tracks
        for (int i = 0; i < cdData.tracks.size() && i < MAX_TRACKS; i++) {
            url.append("&track").append(i + 1).append("=")
               .append(java.net.URLEncoder.encode(cdData.tracks.get(i), "UTF-8"));
        }
        
        // Add template and size
        url.append("&template=jewel");
        url.append("&size=a4");
        url.append("&lang=west");
        
        return url.toString();
    }
    
    /**
     * Download PDF from URL
     */
    private static void downloadPDF(String urlString, String destinationFile) throws IOException {
        java.net.URL url = new java.net.URL(urlString);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        
        // Ensure result directory exists
        File resultFile = new File(destinationFile);
        resultFile.getParentFile().mkdirs();
        
        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(destinationFile)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            int totalBytes = 0;
            
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
            
            System.out.println("Downloaded " + totalBytes + " bytes");
        }
    }
    
    /**
     * Data class for CD information
     */
    static class CDData {
        String artist = "";
        String title = "";
        List<String> tracks = new ArrayList<>();
    }
}
