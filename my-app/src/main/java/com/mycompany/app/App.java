package com.mycompany.app;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class App {
    public static void main(String[] args) {
        new File("data").mkdirs();
        new File("result").mkdirs();

        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get("data/data.txt"));
        } catch (IOException e) {
            System.err.println("Ошибка чтения файла: " + e.getMessage());
            return;
        }

        if (lines.size() < 2) {
            System.err.println("Файл должен содержать исполнителя и название");
            return;
        }

        String artist = lines.get(0);
        String title = lines.get(1);
        List<String> tracks = lines.subList(2, Math.min(lines.size(), 18));

        System.setProperty("webdriver.chrome.driver", "C:\\Dev\\chromedriver-win64\\chromedriver.exe");

        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", new File("result").getAbsolutePath());
        prefs.put("download.prompt_for_download", false);
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
            driver.get("http://www.papercdcase.com/index.php");

            // Заполнение Artist
            WebElement artistField = driver.findElement(By.xpath(
                    "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input"));
            artistField.sendKeys(artist);

            // Заполнение Title
            WebElement titleField = driver.findElement(By.xpath(
                    "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input"));
            titleField.sendKeys(title);

            // Заполнение треков (первые 8 в левой колонке)
            for (int i = 0; i < 8 && i < tracks.size(); i++) {
                WebElement trackField = driver.findElement(By.xpath(
                        String.format("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[%d]/td[2]/input", i+1)));
                trackField.sendKeys(tracks.get(i));
            }

            // Заполнение треков (следующие 8 в правой колонке)
            for (int i = 8; i < 16 && i < tracks.size(); i++) {
                WebElement trackField = driver.findElement(By.xpath(
                        String.format("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[%d]/td[2]/input", i-7)));
                trackField.sendKeys(tracks.get(i));
            }

            // Выбор формата A4
            driver.findElement(By.xpath(
                    "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[1]")).click();

            // Выбор Jewel Case
            driver.findElement(By.xpath(
                    "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[1]")).click();

            // Отправка формы
            driver.findElement(By.xpath(
                    "/html/body/table[2]/tbody/tr/td[1]/div/form/p/input")).click();

            // Ожидание и обработка файла
            Thread.sleep(10000);
            File downloadDir = new File("result");
            File[] files = downloadDir.listFiles((dir, name) -> name.endsWith(".pdf"));

            if (files != null && files.length > 0) {
                File newestFile = Arrays.stream(files)
                        .max(Comparator.comparingLong(File::lastModified))
                        .orElseThrow();

                File target = new File("result/cd.pdf");
                if (target.exists()) target.delete();

                if (newestFile.renameTo(target)) {
                    System.out.println("PDF успешно сохранен: result/cd.pdf");
                } else {
                    System.err.println("Ошибка переименования файла");
                }
            } else {
                System.err.println("Файл не загружен");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}