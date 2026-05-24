package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class App {
    public static void main(String[] args) {
        WebDriver driver = null;

        try {
            Files.createDirectories(Paths.get("result"));

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--ignore-certificate-errors");
            options.addArguments("--allow-insecure-localhost");

            String downloadDir = new File("result").getAbsolutePath();
            java.util.HashMap<String, Object> prefs = new java.util.HashMap<>();
            prefs.put("download.default_directory", downloadDir);
            options.setExperimentalOption("prefs", prefs);

            driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            driver.get("http://www.papercdcase.com");
            Thread.sleep(2000);

            List<String> data = loadData("data/data.txt");
            if (data.size() < 2) {
                System.err.println("Ошибка: data.txt должен содержать минимум исполнителя и название альбома");
                return;
            }

            String artist = data.get(0);
            String title = data.get(1);
            List<String> tracks = data.subList(2, Math.min(data.size(), 18));

            WebElement artistInput = driver.findElement(By.xpath(
                    "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input"));
            artistInput.clear();
            artistInput.sendKeys(artist);

            WebElement titleInput = driver.findElement(By.xpath(
                    "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input"));
            titleInput.clear();
            titleInput.sendKeys(title);

            fillTracks(driver, tracks);

            WebElement jewelCase = driver.findElement(By.xpath(
                    "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]"));
            jewelCase.click();

            WebElement a4 = driver.findElement(By.xpath(
                    "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]"));
            a4.click();

            WebElement submitBtn = driver.findElement(By.xpath(
                    "/html/body/table[2]/tbody/tr/td[1]/div/form/p/input"));
            submitBtn.submit();

            File downloadDirFile = new File(downloadDir);
            File[] files = downloadDirFile.listFiles((dir, name) -> name.endsWith(".pdf"));

            if (files != null && files.length > 0) {
                File downloadedPdf = files[0];
                for (File f : files) {
                    if (f.lastModified() > downloadedPdf.lastModified()) {
                        downloadedPdf = f;
                    }
                }

                File renamedPdf = new File(downloadDir, "cd.pdf");
                Files.move(downloadedPdf.toPath(), renamedPdf.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Успех! PDF сохранен: " + renamedPdf.getAbsolutePath());
            } else {
                System.err.println("Ошибка: PDF файл не найден");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private static void fillTracks(WebDriver driver, List<String> tracks) {
        List<String> trackXpaths = new ArrayList<>();

        for (int i = 1; i <= 8; i++) {
            trackXpaths.add("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[" + i + "]/td[2]/input");
        }

        for (int i = 1; i <= 8; i++) {
            trackXpaths.add("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[" + i + "]/td[2]/input");
        }

        int maxTracks = Math.min(tracks.size(), trackXpaths.size());
        for (int i = 0; i < maxTracks; i++) {
            WebElement trackField = driver.findElement(By.xpath(trackXpaths.get(i)));
            trackField.clear();
            trackField.sendKeys(tracks.get(i));
        }
    }

    private static List<String> loadData(String filePath) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.strip();
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }
}