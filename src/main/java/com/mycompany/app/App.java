package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Scanner;

public class App {
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "A:\\chromedriver-win64\\chromedriver.exe");
        
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--allow-insecure-localhost");
        options.setAcceptInsecureCerts(true);
        
        WebDriver webDriver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(15));
        Scanner scanner = new Scanner(System.in);
        
        try {
            System.out.println("=== ST-8: Paper CD Case для Slipknot ===");
            
            //Читаем данные из файла
            List<String> lines = Files.readAllLines(Paths.get("data/data.txt"));
            
            String artist = extractValue(lines, "Artist:");
            String title = extractValue(lines, "Title:");
            List<String> tracks = extractTracks(lines);
            
            System.out.println("Artist: " + artist);
            System.out.println("Title: " + title);
            System.out.println("Tracks count: " + tracks.size());
            
            //Открываем страницу
            webDriver.get("http://www.papercdcase.com");
            System.out.println("Страница загружена");
            
            Thread.sleep(3000);
            
            //1.Artist
            WebElement artistField = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input")));
            artistField.clear();
            artistField.sendKeys(artist);
            System.out.println("Artist заполнен");
            
            //2.Title
            WebElement titleField = webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input"));
            titleField.clear();
            titleField.sendKeys(title);
            System.out.println("Title заполнен");
            
            //3.Левый столбец треков (1-8)
            for (int i = 0; i < Math.min(tracks.size(), 8); i++) {
                String trackXPath = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[" + (i + 1) + "]/td[2]/input";
                try {
                    WebElement trackField = webDriver.findElement(By.xpath(trackXPath));
                    trackField.clear();
                    trackField.sendKeys(tracks.get(i));
                    System.out.println("Трек " + (i + 1) + ": " + tracks.get(i));
                } catch (Exception e) {
                    System.out.println("Трек " + (i + 1) + " не найден");
                }
            }
            
            //4 Правый столбец треков (9-14)
            for (int i = 8; i < tracks.size() && i < 16; i++) {
                int rightIndex = i - 7;
                String trackXPath = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[" + rightIndex + "]/td[2]/input";
                try {
                    WebElement trackField = webDriver.findElement(By.xpath(trackXPath));
                    trackField.clear();
                    trackField.sendKeys(tracks.get(i));
                    System.out.println("Трек " + (i + 1) + ": " + tracks.get(i));
                } catch (Exception e) {
                    System.out.println("Трек " + (i + 1) + " не найден");
                }
            }
            
            //5.Выбираем Jewel Case
            WebElement jewelCase = webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]"));
            jewelCase.click();
            System.out.println("✓ Выбран Jewel Case");
            
            //6.Выбираем A4
            WebElement a4 = webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]"));
            a4.click();
            System.out.println("Выбран формат A4");
            
            System.out.println("\n==========================================");
            System.out.println("ФОРМА ЗАПОЛНЕНА!");
            System.out.println("==========================================");
            System.out.println("Проверьте данные в браузере.");
            System.out.println("Нажмите Enter для создания PDF...");
            System.out.println("==========================================");
            
            scanner.nextLine();
            
            //7.Нажимаем кнопку Create
            WebElement createBtn = webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input"));
            createBtn.click();
            System.out.println("✓ Кнопка Create нажата!");

            scanner.nextLine();
            
            System.out.println("\nЗадание ST-8 выполнено!");
            
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("\nНажмите Enter для закрытия браузера...");
            scanner.nextLine();
            webDriver.quit();
            scanner.close();
        }
    }
    
    private static String extractValue(List<String> lines, String key) {
        for (String line : lines) {
            if (line.startsWith(key)) {
                return line.substring(key.length()).trim();
            }
        }
        return "";
    }
    
    private static List<String> extractTracks(List<String> lines) {
        java.util.ArrayList<String> tracks = new java.util.ArrayList<>();
        for (String line : lines) {
            if (line.startsWith("Tracks:")) {
                String tracksLine = line.substring(7).trim();
                String[] parts = tracksLine.split(",");
                for (String part : parts) {
                    String track = part.trim();
                    track = track.replaceAll("^\\d+[\\.\\s]*", "");
                    if (!track.isEmpty()) {
                        tracks.add(track);
                    }
                }
            }
        }
        return tracks;
    }
}