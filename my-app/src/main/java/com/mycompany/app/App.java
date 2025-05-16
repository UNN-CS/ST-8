package com.mycompany.app;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class App {
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "C:\\PROGRAMMING\\Testing PO\\ST-8\\chromedriver-win64\\chromedriver.exe");

        WebDriver driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

        try {
            List<String> lines = Files.readAllLines(Paths.get("C:\\PROGRAMMING\\Testing PO\\ST-8\\data\\data.txt"));
            String artist = "", album = "";
            List<String> tracks = new ArrayList<>();

            for (String line : lines) {
                if (line.startsWith("Artist:")) {
                    artist = line.replace("Artist:", "").trim();
                } else if (line.startsWith("Album:")) {
                    album = line.replace("Album:", "").trim();
                } else if (line.matches("^\\d+\\..*")) {
                    String trackName = line.replaceAll("^\\d+\\.", "").trim();
                    tracks.add(trackName);
                }
            }

            driver.get("http://www.papercdcase.com/index.php");

            driver.findElement(By.name("artist")).sendKeys(artist);
            driver.findElement(By.name("title")).sendKeys(album);

            for (int i = 0; i < tracks.size() && i < 18; i++) {
                String inputName = "track" + (i + 1);
                driver.findElement(By.name(inputName)).sendKeys(tracks.get(i));
            }

            driver.findElement(By.cssSelector("input[name='template'][value='origami']")).click();
            driver.findElement(By.cssSelector("input[name='size'][value='a4']")).click();
            driver.findElement(By.cssSelector("input[name='force_saveas'][value='yes']")).click();

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            WebElement generateButton = wait.until(ExpectedConditions.elementToBeClickable(By.name("submit")));
            generateButton.submit();

            String mainWindow = driver.getWindowHandle();
            for (String handle : driver.getWindowHandles()) {
                if (!handle.equals(mainWindow)) {
                    driver.switchTo().window(handle);
                    break;
                }
            }

            WebElement pdfLink = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.partialLinkText("Download the resulting PDF file")));
            String pdfUrl = pdfLink.getAttribute("href");

            InputStream in = new URL(pdfUrl).openStream();
            Path outputPath = Paths.get("C:\\PROGRAMMING\\Testing PO\\ST-8\\result\\cd.pdf");
            Files.createDirectories(outputPath.getParent());
            Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
            in.close();

            System.out.println("PDF успешно сохранён по пути: " + outputPath);

            Thread.sleep(3000);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}
