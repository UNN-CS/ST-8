package com.mycompany.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class App {
    private static final String BASE_URL = "http://www.papercdcase.com";
    private static final int MAX_TRACKS = 16;

    public static void main(String[] args) throws IOException, InterruptedException {
        Path projectRoot = Paths.get("..").toAbsolutePath().normalize();
        Path dataFile = projectRoot.resolve("data").resolve("data.txt");
        Path resultDir = projectRoot.resolve("result");
        Path resultPdf = resultDir.resolve("cd.pdf");

        Files.createDirectories(resultDir);

        List<String> lines = Files.readAllLines(dataFile).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());

        if (lines.size() < 2) {
            throw new IllegalArgumentException("data.txt must contain artist, title and optional tracks");
        }

        String artist = lines.get(0);
        String title = lines.get(1);
        List<String> tracks = lines.subList(2, Math.min(lines.size(), 2 + MAX_TRACKS));

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", resultDir.toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);

        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--allow-insecure-localhost");

        WebDriver webDriver = new ChromeDriver(options);
        try {
            webDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
            webDriver.get(BASE_URL);

            webDriver.findElement(By.name("artist")).sendKeys(artist);
            webDriver.findElement(By.name("title")).sendKeys(title);

            for (int i = 0; i < tracks.size(); i++) {
                webDriver.findElement(By.name("track" + (i + 1))).sendKeys(tracks.get(i));
            }

            webDriver.findElement(By.xpath("//input[@name='size' and @value='a4']")).click();
            webDriver.findElement(By.xpath("//input[@name='template' and @value='jewel']")).click();

            WebElement btn = webDriver.findElement(By.name("submit"));
            btn.submit();

            waitForDownload(resultDir, resultPdf);
            System.out.println("PDF saved to: " + resultPdf);
        } finally {
            webDriver.quit();
        }
    }

    private static void waitForDownload(Path downloadDir, Path targetPdf)
            throws IOException, InterruptedException {
        Path downloadedPdf = null;

        for (int attempt = 0; attempt < 60; attempt++) {
            try (var files = Files.list(downloadDir)) {
                downloadedPdf = files
                        .filter(path -> path.toString().endsWith(".pdf"))
                        .filter(path -> !path.getFileName().toString().endsWith(".crdownload"))
                        .filter(path -> !path.equals(targetPdf))
                        .findFirst()
                        .orElse(null);
            }

            if (downloadedPdf != null && Files.size(downloadedPdf) > 0) {
                break;
            }

            Thread.sleep(1000);
        }

        if (downloadedPdf == null) {
            throw new IOException("PDF download was not completed");
        }

        Files.move(downloadedPdf, targetPdf, StandardCopyOption.REPLACE_EXISTING);
    }
}
