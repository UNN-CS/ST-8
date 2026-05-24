package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public class App {
    private static final String BASE_URL = "https://www.papercdcase.com/";
    private static final Path DATA_FILE = Path.of("data", "data.txt");
    private static final Path RESULT_DIR = Path.of("result");
    private static final Path RESULT_FILE = RESULT_DIR.resolve("cd.pdf");

    public static void main(String[] args) throws IOException {
        CoverData coverData = readCoverData(DATA_FILE);

        Files.createDirectories(RESULT_DIR);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--allow-insecure-localhost");
        options.addArguments("--headless=new");
        options.addArguments("--window-size=1920,1080");

        // Путь до chromedriver нужно передать через параметр JVM:
        // -Dwebdriver.chrome.driver=/path/to/chromedriver
        try (WebDriver driver = new ChromeDriver(options)) {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            driver.get(BASE_URL);

            WebElement artistInput = wait.until(ExpectedConditions.elementToBeClickable(By.name("artist")));
            WebElement titleInput = driver.findElement(By.name("title"));
            WebElement tracksInput = driver.findElement(By.name("songs"));

            artistInput.clear();
            artistInput.sendKeys(coverData.artist());
            titleInput.clear();
            titleInput.sendKeys(coverData.albumTitle());
            tracksInput.clear();
            tracksInput.sendKeys(String.join("\n", coverData.tracks()));

            // переключатели: формат A4 и тип Jewel Case
            selectIfPresent(driver, By.xpath("//input[@type='radio' and (@value='a4' or @id='a4')]"));
            selectIfPresent(driver, By.xpath("//input[@type='radio' and (contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'jewel') or contains(translate(@id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'jewel'))]"));

            WebElement submitButton = driver.findElement(By.cssSelector("input[type='submit'], button[type='submit']"));
            submitButton.submit();

            wait.until(ExpectedConditions.urlContains("pdf"));
            String pdfUrl = driver.getCurrentUrl();

            byte[] pdfData = java.net.http.HttpClient.newHttpClient()
                    .send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(pdfUrl)).GET().build(),
                            java.net.http.HttpResponse.BodyHandlers.ofByteArray())
                    .body();

            Files.write(RESULT_FILE, pdfData);
            System.out.println("PDF saved to: " + RESULT_FILE.toAbsolutePath());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while downloading PDF", e);
        }
    }

    private static void selectIfPresent(WebDriver driver, By selector) {
        List<WebElement> elements = driver.findElements(selector);
        if (!elements.isEmpty() && !elements.get(0).isSelected()) {
            elements.get(0).click();
        }
    }

    private static CoverData readCoverData(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();

        if (lines.size() < 3) {
            throw new IllegalArgumentException("data.txt must contain artist, album title and at least one track");
        }

        String artist = lines.get(0);
        String albumTitle = lines.get(1);
        List<String> tracks = lines.subList(2, Math.min(lines.size(), 20)); // до 18 треков

        return new CoverData(artist, albumTitle, tracks);
    }

    private record CoverData(String artist, String albumTitle, List<String> tracks) {
    }
}
