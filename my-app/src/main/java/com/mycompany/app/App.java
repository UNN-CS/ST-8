package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class App {
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "Z:\\Coding\\chromedriver-win64\\chromedriver.exe");
        WebDriver driver = new ChromeDriver();
        driver.get("http://www.papercdcase.com/index.php");

        List<String> lines;
        try {
            lines = Files.readAllLines(Path.of("Z:\\Coding\\Testing\\ST-8\\data\\data.txt"));
        } catch (IOException e) {
            System.err.println("Не удалось прочитать data.txt: " + e.getMessage());
            return;
        }
        if (lines.size() < 2) {
            System.err.println("В data.txt должно быть минимум 2 строки (исполнитель и альбом)");
            return;
        }
        String artist = lines.get(0);
        String title = lines.get(1);

        String[] trackInputs = new String[3];
        for (int i = 0; i < 3; i++) {
            int idx = 2 + i;
            trackInputs[i] = idx < lines.size() ? lines.get(idx) : "";
        }

        try {
            driver.findElement(By.xpath(
              "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input"
            )).sendKeys(artist);

            driver.findElement(By.xpath(
              "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input"
            )).sendKeys(title);

            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[1]/td[2]/input"))
                  .sendKeys(trackInputs[0]);

            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[2]/td[2]/input"))
                  .sendKeys(trackInputs[1]);

            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[3]/td[2]/input"))
                  .sendKeys(trackInputs[2]);

            // 5) Выбрать формат (A4) и тип (Jewel Case)
            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]"))
                  .click();
            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]"))
                  .click();

            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input")).submit();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}
