package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import org.apache.commons.io.FileUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "C:\\chromedriver-win64\\chromedriver.exe");
        WebDriver webDriver = new ChromeDriver();
        try {
            // Загрузка данных из файла
            List<String> data = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader("../data/data.txt"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    data.add(line);
                }
            }

            // Открытие страницы
            webDriver.get("http://www.papercdcase.com/index.php");
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));

            // Заполнение формы
            WebElement artistField = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input")));
            WebElement titleField = webDriver.findElement(
                By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input"));

            // Заполняем поля
            artistField.sendKeys(data.get(0).substring(8)); // Убираем "Artist: "
            titleField.sendKeys(data.get(1).substring(7)); // Убираем "Title: "

            // Заполняем треки
            String[] tracks = data.subList(3, data.size()).toArray(new String[0]);
            for (int i = 0; i < tracks.length; i++) {
                // Определяем, в какой колонке искать поле (первая или вторая)
                int column = (i < 8) ? 1 : 2;
                int row = (i < 8) ? i + 1 : i - 7;
                
                // Убираем номер трека из названия (например, "1. Speak to Me" -> "Speak to Me")
                String trackName = tracks[i].replaceFirst("^\\d+\\.\\s*", "");
                
                WebElement trackField = webDriver.findElement(
                    By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[" + column + "]/table/tbody/tr[" + row + "]/td[2]/input"));
                trackField.sendKeys(trackName);
            }

            // Выбираем Jewel Case
            WebElement jewelCaseRadio = webDriver.findElement(
                By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]"));
            jewelCaseRadio.click();

            // Выбираем формат A4
            WebElement a4Radio = webDriver.findElement(
                By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]"));
            a4Radio.click();

            // Нажимаем дополнительную кнопку
            WebElement additionalButton = webDriver.findElement(
                By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[7]/td[2]/input"));
            additionalButton.click();

            // Нажимаем кнопку генерации
            WebElement submitButton = webDriver.findElement(
                By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input"));
            submitButton.click();

            // Ждем загрузки PDF
            Thread.sleep(10000); // Увеличиваем время ожидания до 10 секунд

            // Проверяем наличие файла в Downloads
            Path sourcePath = Paths.get("F:", "Downloads", "papercdcase.pdf");
            int attempts = 0;
            while (!Files.exists(sourcePath) && attempts < 10) {
                Thread.sleep(1000);
                attempts++;
                System.out.println("Ожидание файла... Попытка " + (attempts + 1));
            }

            if (Files.exists(sourcePath)) {
                // Копируем PDF в директорию result
                Path targetPath = Paths.get("../result/cd.pdf");
                Files.copy(sourcePath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.out.println("PDF успешно скопирован в result/cd.pdf");
            } else {
                System.out.println("PDF не был найден в папке Downloads");
                System.out.println("Искали файл: " + sourcePath.toAbsolutePath());
            }

        } catch (Exception e) {
            System.out.println("Error");
            System.out.println(e.toString());
        } finally {
            webDriver.quit();
        }
    }
} 