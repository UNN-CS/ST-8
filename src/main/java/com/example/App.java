package com.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class App {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.setProperty("webdriver.chrome.driver", "D:/ST8/chromedriver-win64/chromedriver.exe");

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", Paths.get("result").toAbsolutePath().toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--ignore-certificate-errors");
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().window().maximize();

        try {
            driver.get("http://www.papercdcase.com");
            Thread.sleep(3000);

            // Если поле track1 не найдено - обновляем страницу (решает проблему ручного обновления)
            if (driver.findElements(By.xpath("/html[1]/body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[1]/form[1]/table[1]/tbody[1]/tr[3]/td[2]/table[1]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[1]/td[2]/input[1]")).isEmpty()) {
                System.out.println("Поля не найдены, обновляем страницу...");
                driver.navigate().refresh();
                Thread.sleep(3000);
            }

            // Читаем данные
            List<String> lines = Files.readAllLines(Paths.get("data/data.txt"));
            String artist = null, title = null;
            List<String> tracks = null;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.startsWith("Artist: ")) artist = line.substring(8).trim();
                else if (line.startsWith("Title: ")) title = line.substring(7).trim();
                else if (line.equals("Tracks:")) {
                    tracks = lines.stream().skip(i+1).filter(l -> !l.isEmpty()).collect(Collectors.toList());
                    break;
                }
            }

            // === XPath для Artist ===
            driver.findElement(By.xpath("/html[1]/body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[1]/form[1]/table[1]/tbody[1]/tr[1]/td[2]/input[1]"))
                  .sendKeys(artist);
            // === XPath для Title ===
            driver.findElement(By.xpath("/html[1]/body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[1]/form[1]/table[1]/tbody[1]/tr[2]/td[2]/input[1]"))
                  .sendKeys(title);

            // === 16 XPath для треков (твой массив) ===
            String[] trackXPaths = {
                "/html[1]/body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[1]/form[1]/table[1]/tbody[1]/tr[3]/td[2]/table[1]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[1]/td[2]/input[1]",
                "/html[1]/body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[1]/form[1]/table[1]/tbody[1]/tr[3]/td[2]/table[1]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[2]/td[2]/input[1]",
                "/html[1]/body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[1]/form[1]/table[1]/tbody[1]/tr[3]/td[2]/table[1]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[3]/td[2]/input[1]",
                "/html[1]/body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[1]/form[1]/table[1]/tbody[1]/tr[3]/td[2]/table[1]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[4]/td[2]/input[1]",
                "/html[1]/body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[1]/form[1]/table[1]/tbody[1]/tr[3]/td[2]/table[1]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[5]/td[2]/input[1]",
                "/html[1]/body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[1]/form[1]/table[1]/tbody[1]/tr[3]/td[2]/table[1]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[6]/td[2]/input[1]",
                "/html[1]/body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[1]/form[1]/table[1]/tbody[1]/tr[3]/td[2]/table[1]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[7]/td[2]/input[1]",
                "/html[1]/body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[1]/form[1]/table[1]/tbody[1]/tr[3]/td[2]/table[1]/tbody[1]/tr[1]/td[1]/table[1]/tbody[1]/tr[8]/td[2]/input[1]",
                "/html[1]/body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[1]/form[1]/table[1]/tbody[1]/tr[3]/td[2]/table[1]/tbody[1]/tr[1]/td[2]/table[1]/tbody[1]/tr[1]/td[2]/input[1]",
                "/html[1]/body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[1]/form[1]/table[1]/tbody[1]/tr[3]/td[2]/table[1]/tbody[1]/tr[1]/td[2]/table[1]/tbody[1]/tr[2]/td[2]/input[1]",
                "/html[1]/body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[1]/form[1]/table[1]/tbody[1]/tr[3]/td[2]/table[1]/tbody[1]/tr[1]/td[2]/table[1]/tbody[1]/tr[3]/td[2]/input[1]",
                "/html[1]/body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[1]/form[1]/table[1]/tbody[1]/tr[3]/td[2]/table[1]/tbody[1]/tr[1]/td[2]/table[1]/tbody[1]/tr[4]/td[2]/input[1]",
                "/html[1]/body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[1]/form[1]/table[1]/tbody[1]/tr[3]/td[2]/table[1]/tbody[1]/tr[1]/td[2]/table[1]/tbody[1]/tr[5]/td[2]/input[1]",
                "/html[1]/body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[1]/form[1]/table[1]/tbody[1]/tr[3]/td[2]/table[1]/tbody[1]/tr[1]/td[2]/table[1]/tbody[1]/tr[6]/td[2]/input[1]",
                "/html[1]/body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[1]/form[1]/table[1]/tbody[1]/tr[3]/td[2]/table[1]/tbody[1]/tr[1]/td[2]/table[1]/tbody[1]/tr[7]/td[2]/input[1]",
                "/html[1]/body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[1]/form[1]/table[1]/tbody[1]/tr[3]/td[2]/table[1]/tbody[1]/tr[1]/td[2]/table[1]/tbody[1]/tr[8]/td[2]/input[1]"
            };

            for (int i = 0; i < tracks.size() && i < trackXPaths.length; i++) {
                driver.findElement(By.xpath(trackXPaths[i])).sendKeys(tracks.get(i));
            }

            // Jewel Case
            driver.findElement(By.xpath("/html[1]/body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[1]/form[1]/table[1]/tbody[1]/tr[4]/td[2]/input[2]")).click();
            // A4
            driver.findElement(By.xpath("/html[1]/body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[1]/form[1]/table[1]/tbody[1]/tr[5]/td[2]/input[2]")).click();
            // Кнопка
            driver.findElement(By.xpath("/html[1]/body[1]/table[2]/tbody[1]/tr[1]/td[1]/div[1]/form[1]/p[1]/input[1]")).click();

            // Ожидание PDF
            Path resultDir = Paths.get("result");
            boolean downloaded = false;
            for (int i = 0; i < 60; i++) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(resultDir, "*.pdf")) {
                    for (Path entry : stream) {
                        Files.move(entry, resultDir.resolve("cd.pdf"), StandardCopyOption.REPLACE_EXISTING);
                        downloaded = true;
                        break;
                    }
                }
                if (downloaded) break;
                Thread.sleep(1000);
            }
            System.out.println(downloaded ? "SUCCESS: PDF сохранён в result/cd.pdf" : "ERROR: PDF не найден");
            Thread.sleep(2000);
        } finally {
            driver.quit();
        }
    }
}