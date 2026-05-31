package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class App {
    public static void main(String[] args) {
        System.out.println("--- ST-8: Paper CD Case Generator ---");
        
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        // НЕ используем --headless, чтобы видеть окно браузера!
        
        WebDriver driver = new ChromeDriver(options);
        try {
            driver.get("http://www.papercdcase.com");
            System.out.println("Сайт открыт. Заполняем форму...");

            List<String> lines = Files.readAllLines(Paths.get("data/data.txt"));
            String artist = lines.size() > 0 ? lines.get(0) : "Unknown";
            String title = lines.size() > 1 ? lines.get(1) : "Unknown";
            
            driver.findElement(By.name("artist")).sendKeys(artist);
            driver.findElement(By.name("title")).sendKeys(title);
            
            int trackCount = 0;
            for (int i = 2; i < lines.size() && i < 18; i++) {
                String track = lines.get(i).trim();
                if (!track.isEmpty()) {
                    trackCount++;
                    driver.findElement(By.name("track" + trackCount)).sendKeys(track);
                }
            }
            
            try { driver.findElement(By.cssSelector("input[value='jewel']")).click(); } catch(Exception e) {}
            try { driver.findElement(By.cssSelector("input[value='a4']")).click(); } catch(Exception e) {}
            
            WebElement btn = driver.findElement(By.name("submit"));
            btn.click();
            
            System.out.println("Кнопка нажата! PDF должен загрузиться в браузере.");
            System.out.println("ВНИМАНИЕ: Сохраните PDF вручную в папку result как cd.pdf");
            System.out.println("Нажмите Enter в этой консоли, чтобы закрыть браузер...");
            
            // Ждем, пока ты вручную скачаешь файл и нажмешь Enter
            System.in.read();
            
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
            System.out.println("Браузер закрыт.");
        }
    }
}
