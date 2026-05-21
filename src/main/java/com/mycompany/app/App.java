package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class App {
    public static void main(String[] args) {
        System.setProperty(
            "webdriver.chrome.driver",
            "chromedriver-win64/chromedriver-win64/chromedriver.exe"
        );

        File resultDir = new File("result");
        if (!resultDir.exists()) {
            resultDir.mkdirs();
        }

        // Обычные опции, больше не нужно бороться с настройками загрузок Chrome
        ChromeOptions options = new ChromeOptions();
        WebDriver webDriver = new ChromeDriver(options);

        try {
            System.out.println("Чтение данных из файла...");
            List<String> lines = Files.readAllLines(Paths.get("data/data.txt"), StandardCharsets.UTF_8);
            if (lines.size() < 3) {
                throw new Exception("Файл data.txt должен содержать как минимум исполнителя, альбом и 1 трек.");
            }
            
            String artist = lines.get(0);
            String title = lines.get(1);
            List<String> tracks = lines.subList(2, Math.min(lines.size(), 20));

            System.out.println("Открытие страницы www.papercdcase.com...");
            webDriver.get("http://www.papercdcase.com/");

            // 1. Заполняем форму через Selenium
            webDriver.findElement(By.xpath("//input[@name='artist']")).sendKeys(artist);
            webDriver.findElement(By.xpath("//input[@name='title']")).sendKeys(title);

            for (int i = 0; i < tracks.size(); i++) {
                String trackName = "track" + (i + 1);
                List<WebElement> trackInputs = webDriver.findElements(By.xpath("//input[@name='" + trackName + "']"));
                if (!trackInputs.isEmpty()) {
                    trackInputs.get(0).sendKeys(tracks.get(i));
                }
            }

            WebElement jewelRadio = webDriver.findElement(By.xpath("//input[@value='jewel']"));
            if (!jewelRadio.isSelected()) { jewelRadio.click(); }

            WebElement a4Radio = webDriver.findElement(By.xpath("//input[@value='a4']"));
            if (!a4Radio.isSelected()) { a4Radio.click(); }

            // 2. ВМЕСТО НАЖАТИЯ КНОПКИ: Генерируем URL напрямую, как сделал студент
            System.out.println("Форма заполнена. Генерируем прямую ссылку на PDF...");
            StringBuilder pdfUrl = new StringBuilder("http://www.papercdcase.com/papercdcase.cgi/papercdcase.pdf?");
            pdfUrl.append("artist=").append(URLEncoder.encode(artist, StandardCharsets.UTF_8.toString()));
            pdfUrl.append("&title=").append(URLEncoder.encode(title, StandardCharsets.UTF_8.toString()));
            
            for (int i = 0; i < tracks.size(); i++) {
                pdfUrl.append("&track").append(i + 1).append("=")
                      .append(URLEncoder.encode(tracks.get(i), StandardCharsets.UTF_8.toString()));
            }
            pdfUrl.append("&template=jewel&size=a4&lang=west");

            System.out.println("Скачиваем PDF средствами Java...");
            
            // 3. Скачиваем файл через Java (HttpURLConnection)
            File finalFile = new File(resultDir, "cd.pdf");
            URL url = new URL(pdfUrl.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = connection.getInputStream()) {
                    Files.copy(inputStream, finalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                System.out.println("Успех! Файл сохранен как: " + finalFile.getAbsolutePath());
            } else {
                System.out.println("Ошибка скачивания: HTTP код " + connection.getResponseCode());
            }
            connection.disconnect();

        } catch (Exception e) {
            System.out.println("Произошла ошибка при выполнении скрипта:");
            e.printStackTrace();
        } finally {
            if (webDriver != null) {
                webDriver.quit();
            }
        }
    }
}