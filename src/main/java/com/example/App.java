package com.example;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class App {
    public static void main(String[] args) throws Exception {
        // Read data.txt
        List<String> lines = Files.readAllLines(Paths.get("data/data.txt"));
        String artist = lines.get(0).trim();
        String title  = lines.get(1).trim();
        List<String> tracks = new ArrayList<>();
        for (int i = 2; i < lines.size(); i++) {
            String t = lines.get(i).trim();
            if (!t.isEmpty()) tracks.add(t);
        }

        // Absolute path to result directory
        File resultDir = new File("result").getAbsoluteFile();
        resultDir.mkdirs();

        // Configure Chrome to auto-download PDFs
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", resultDir.getAbsolutePath());
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        prefs.put("download.directory_upgrade", true);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        options.setExperimentalOption("prefs", prefs);
        WebDriver wd = new ChromeDriver(options);

        try {
            wd.get("http://www.papercdcase.com");
            Thread.sleep(3000);

            // Fill Artist and Title
            wd.findElement(By.name("artist")).sendKeys(artist);
            wd.findElement(By.name("title")).sendKeys(title);

            // Fill tracks
            for (int i = 0; i < Math.min(tracks.size(), 16); i++) {
                wd.findElement(By.name("track" + (i + 1))).sendKeys(tracks.get(i));
            }

            // Select Jewel case (template=jewel)
            WebElement jewel = wd.findElement(By.xpath("//input[@name='template' and @value='jewel']"));
            if (!jewel.isSelected()) jewel.click();

            // Select A4 (size=a4)
            WebElement a4 = wd.findElement(By.xpath("//input[@name='size' and @value='a4']"));
            if (!a4.isSelected()) a4.click();

            // Check Force Save-as to ensure download (not browser preview)
            WebElement forceSave = wd.findElement(By.name("force_saveas"));
            if (!forceSave.isSelected()) forceSave.click();

            System.out.println("Form filled. Submitting...");
            // Enable downloads in headless Chrome via CDP
            ((ChromeDriver) wd).executeCdpCommand("Browser.setDownloadBehavior",
                    Map.of("behavior", "allow", "downloadPath", resultDir.getAbsolutePath()));

            // Submit via image button
            WebElement btn = wd.findElement(By.name("submit"));
            btn.submit();
            Thread.sleep(3000);

            // Check current URL (may be the PDF link or redirect)
            String currentUrl = wd.getCurrentUrl();
            System.out.println("Current URL after submit: " + currentUrl);

            // Wait for download to complete (up to 30 seconds)
            System.out.println("Waiting for PDF download...");
            File downloaded = null;
            for (int i = 0; i < 30; i++) {
                Thread.sleep(1000);
                File[] files = resultDir.listFiles((dir, name) ->
                        name.endsWith(".pdf") && !name.endsWith(".crdownload"));
                if (files != null && files.length > 0) {
                    downloaded = files[0];
                    break;
                }
            }

            if (downloaded != null) {
                File dest = new File(resultDir, "cd.pdf");
                if (!downloaded.getName().equals("cd.pdf")) {
                    Files.move(downloaded.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                System.out.println("PDF saved: " + dest.getAbsolutePath() + " (" + dest.length() + " bytes)");
            } else {
                // Fallback: if current URL is a PDF link, download it via HTTP
                if (currentUrl != null && (currentUrl.contains(".pdf") || currentUrl.contains("papercdcase"))) {
                    System.out.println("Trying HTTP download from: " + currentUrl);
                    java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
                    java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                            .uri(java.net.URI.create(currentUrl))
                            .GET().build();
                    java.net.http.HttpResponse<byte[]> resp = httpClient.send(req,
                            java.net.http.HttpResponse.BodyHandlers.ofByteArray());
                    if (resp.statusCode() == 200) {
                        Files.write(new File(resultDir, "cd.pdf").toPath(), resp.body());
                        System.out.println("PDF downloaded via HTTP: " + resp.body().length + " bytes");
                    } else {
                        System.out.println("HTTP download failed: status " + resp.statusCode());
                    }
                } else {
                    System.out.println("No PDF found. URL: " + currentUrl);
                }
            }
        } finally {
            wd.quit();
        }
    }
}
