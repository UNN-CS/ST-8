package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

public class App {
    public static void main(String[] args) throws IOException, InterruptedException {
        // 1) читаем data.txt
        Path dataFile = Paths.get("..", "data", "data.txt");
        if (!Files.exists(dataFile)) {
            System.err.println("Не найден файл: " + dataFile.toAbsolutePath());
            return;
        }
        String artist = "", title = "";
        List<String> tracks = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(dataFile)) {
            String line; boolean inTracks = false;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Artist:")) {
                    artist = line.substring(7).trim();
                } else if (line.startsWith("Title:")) {
                    title = line.substring(6).trim();
                    inTracks = true;
                } else if (inTracks && !line.isBlank()) {
                    String[] parts = line.split("\\.\\s+", 2);
                    if (parts.length == 2) tracks.add(parts[1].trim());
                }
            }
        }

        // 2) создаём result-директорию 
        Path resultDir = Paths.get("..", "result");
        Files.createDirectories(resultDir);

        // 3) настраиваем Selenium ChromeDriver 
        ChromeOptions options = new ChromeOptions();
        // не ставим headless, браузер будет виден
        options.addArguments("--disable-web-security",
                             "--allow-running-insecure-content");
        System.setProperty("webdriver.chrome.driver",
            "C:/Users/rerar/Documents/chromedriver-win64/chromedriver.exe");
        WebDriver driver = new ChromeDriver(options);

        try {
            // 4) открываем форму и заполняем её 
            driver.get("http://www.papercdcase.com/index.php");

            driver.findElement(By.name("artist")).sendKeys(artist);
            driver.findElement(By.name("title")).sendKeys(title);
            for (int i = 0; i < tracks.size() && i < 18; i++) {
                driver.findElement(By.name("track" + (i + 1)))
                      .sendKeys(tracks.get(i));
            }
            // переключатели
            driver.findElement(By.cssSelector("input[name='size'][value='a4']")).click();
            driver.findElement(By.cssSelector("input[name='template'][value='jewel']")).click();

            // нажимаем «Generate»
            driver.findElement(By.name("submit")).click();

            // 5) Пауза 10 сек
            Thread.sleep(Duration.ofSeconds(10).toMillis());

        } finally {
            // оставляем драйвер открытым до скачивания PDF
        }

        // 6) Формируем URL точно так же, как если бы мы отправили GET‑форму ────
        Map<String,String> params = new LinkedHashMap<>();
        params.put("artist", artist);
        params.put("title",  title);
        for (int i = 0; i < tracks.size() && i < 18; i++) {
            params.put("track" + (i + 1), tracks.get(i));
        }
        params.put("size",     "a4");
        params.put("template", "jewel");


        StringBuilder qs = new StringBuilder();
        for (var e : params.entrySet()) {
            if (qs.length() > 0) qs.append('&');
            qs.append(URLEncoder.encode(e.getKey(), "UTF-8"))
              .append('=')
              .append(URLEncoder.encode(e.getValue(), "UTF-8"));
        }
        String fullUrl = "https://www.papercdcase.com/papercdcase.cgi/papercdcase.pdf?" + qs;
        System.out.println("Downloading PDF from: " + fullUrl);

        // 7) HTTP‑запрос и сохранение PDF 
        URL url = new URL(fullUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Accept", "application/pdf");
        conn.connect();

        int code = conn.getResponseCode();
        String type = conn.getContentType();
        System.out.printf("HTTP %d, Content-Type: %s%n", code, type);

        if (code == 200 && type != null && type.startsWith("application/pdf")) {
            Path out = resultDir.resolve("cd.pdf");
            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
            }
            System.out.println("PDF saved to: " + out.toAbsolutePath());
        } else {
            System.err.println("Failed to download PDF — HTTP " + code);
            try (InputStream err = conn.getErrorStream()) {
                if (err != null) new BufferedReader(new InputStreamReader(err))
                    .lines().forEach(System.err::println);
            }
        }

        // 8) Закрываем браузер
        driver.quit();
    }
}