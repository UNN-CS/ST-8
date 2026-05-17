package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        System.out.println("=== Запуск Лабораторной работы ST-8 ===");

        String artist = "";
        String title = "";
        List<String> tracks = new ArrayList<>();

        try {
            File dataFile = new File("data/data.txt");
            if (!dataFile.exists()) {
                dataFile = new File("../data/data.txt");
            }
            if (!dataFile.exists()) {
                throw new FileNotFoundException("Файл data.txt не найден ни в data/data.txt, ни в ../data/data.txt!");
            }

            System.out.println("Считываем данные из файла: " + dataFile.getAbsolutePath());
            try (BufferedReader reader = new BufferedReader(new FileReader(dataFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("Artist:")) {
                        artist = line.substring("Artist:".length()).trim();
                    } else if (line.startsWith("Title:")) {
                        title = line.substring("Title:".length()).trim();
                    } else if (line.startsWith("Track")) {
                        int colonIndex = line.indexOf(':');
                        if (colonIndex != -1) {
                            tracks.add(line.substring(colonIndex + 1).trim());
                        }
                    }
                }
            }

            System.out.println("Данные успешно прочитаны:");
            System.out.println("Исполнитель: " + artist);
            System.out.println("Альбом: " + title);
            System.out.println("Количество треков: " + tracks.size());
            System.out.println();

        } catch (Exception e) {
            System.err.println("Ошибка при чтении файла данных: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        File resultDir = new File("result");
        try {
            if (!resultDir.exists() || !resultDir.getCanonicalPath().contains("ST-8")) {
                resultDir = new File("../result");
            }
            if (!resultDir.exists()) {
                resultDir.mkdirs();
            }
            System.out.println("Папка для сохранения PDF: " + resultDir.getCanonicalPath());
        } catch (IOException e) {
            System.err.println("Не удалось настроить директорию результатов: " + e.getMessage());
        }

        WebDriver webDriver = null;
        boolean downloadSuccess = false;

        try {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--remote-allow-origins=*");

            Map<String, Object> chromePrefs = new HashMap<>();
            chromePrefs.put("download.default_directory", resultDir.getCanonicalPath());
            chromePrefs.put("download.prompt_for_download", false);
            chromePrefs.put("plugins.always_open_pdf_externally", true);
            options.setExperimentalOption("prefs", chromePrefs);

            System.out.println("Запускаем браузер Chrome...");
            webDriver = new ChromeDriver(options);
            webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            System.out.println("Переходим на сайт papercdcase.com...");
            webDriver.get("http://www.papercdcase.com");

            System.out.println("Заполняем поле Artist...");
            WebElement artistInput = webDriver.findElement(By.name("artist"));
            artistInput.clear();
            artistInput.sendKeys(artist);

            System.out.println("Заполняем поле Title...");
            WebElement titleInput = webDriver.findElement(By.name("title"));
            titleInput.clear();
            titleInput.sendKeys(title);
            System.out.println("Заполняем поля треков...");
            for (int i = 0; i < tracks.size() && i < 16; i++) {
                WebElement trackInput = webDriver.findElement(By.name("track" + (i + 1)));
                trackInput.clear();
                trackInput.sendKeys(tracks.get(i));
            }

            System.out.println("Выбираем формат бумаги A4...");
            WebElement paperA4 = webDriver.findElement(By.xpath("//input[@name='size' and @value='a4']"));
            if (!paperA4.isSelected()) {
                paperA4.click();
            }

            System.out.println("Выбираем тип обложки Jewel Case...");
            WebElement templateJewel = webDriver.findElement(By.xpath("//input[@name='template' and @value='jewel']"));
            if (!templateJewel.isSelected()) {
                templateJewel.click();
            }

            System.out.println("Активируем опцию Force Save-as...");
            WebElement forceSaveAs = webDriver.findElement(By.name("force_saveas"));
            if (!forceSaveAs.isSelected()) {
                forceSaveAs.click();
            }

            System.out.println("Отправляем форму генерации CD-обложки...");
            WebElement submitBtn = webDriver.findElement(By.name("submit"));
            submitBtn.click();
            System.out.println("Ожидание завершения скачивания файла Chrome...");
            File downloadedFile = new File(resultDir, "papercdcase.pdf");
            File crDownloadFile = new File(resultDir, "papercdcase.pdf.crdownload");
            
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < 15000) {
                if (downloadedFile.exists() && downloadedFile.length() > 0 && !crDownloadFile.exists()) {
                    downloadSuccess = true;
                    break;
                }
                Thread.sleep(1000);
            }

            if (downloadSuccess) {
                File targetFile = new File(resultDir, "cd.pdf");
                Files.copy(downloadedFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                downloadedFile.delete();
                System.out.println("Файл успешно скачан через браузер и сохранен как: " + targetFile.getAbsolutePath());
            }

        } catch (Exception e) {
            System.err.println("Предупреждение: не удалось завершить скачивание через UI браузера: " + e.getMessage());
        } finally {
            if (webDriver != null) {
                System.out.println("Закрываем сессию браузера...");
                webDriver.quit();
            }
        }

        if (!downloadSuccess) {
            System.out.println("Запускаем резервный сетевой метод скачивания обложки...");
            try {
                StringBuilder urlBuilder = new StringBuilder("http://www.papercdcase.com/papercdcase.cgi/papercdcase.pdf?");
                urlBuilder.append("artist=").append(URLEncoder.encode(artist, "UTF-8"));
                urlBuilder.append("&title=").append(URLEncoder.encode(title, "UTF-8"));
                for (int i = 0; i < 16; i++) {
                    String trackVal = i < tracks.size() ? tracks.get(i) : "";
                    urlBuilder.append("&track").append(i + 1).append("=").append(URLEncoder.encode(trackVal, "UTF-8"));
                }
                urlBuilder.append("&template=jewel");
                urlBuilder.append("&size=a4");
                urlBuilder.append("&lang=west");
                urlBuilder.append("&force_saveas=yes");
                urlBuilder.append("&submit.x=0&submit.y=0");

                String downloadUrl = urlBuilder.toString();
                System.out.println("Запрос по адресу: " + downloadUrl);

                URL url = new URL(downloadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                File targetFile = new File(resultDir, "cd.pdf");
                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(targetFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                System.out.println("Резервный метод успешно сработал! Файл сохранен как: " + targetFile.getAbsolutePath());
                downloadSuccess = true;
            } catch (Exception e) {
                System.err.println("Ошибка при выполнении резервного скачивания: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (downloadSuccess) {
            System.out.println("=== Выполнение Лабораторной работы ST-8 успешно завершено! ===");
        } else {
            System.err.println("=== Ошибка: не удалось сформировать и получить PDF-файл! ===");
        }
    }
}
