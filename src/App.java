package com.mycompany.app;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Selenium-скрипт для автоматического заполнения формы на сайте papercdcase.com
 * и сохранения сгенерированного PDF-файла с обложкой компакт-диска.
 *
 * Лабораторная работа ST-8: Тестирование web-приложений с использованием Java
 * и фреймворка Selenium.
 */
public class App {

    // Пути к файлам проекта (относительно корня проекта my-app)
    private static final String CHROMEDRIVER_PATH = "../drivers/chromedriver.exe";
    private static final String DATA_FILE_PATH = "../data/data.txt";
    private static final String RESULT_DIR = "../result";
    private static final String RESULT_FILE = "../result/cd.pdf";

    // Базовый URL сайта
    private static final String BASE_URL = "http://www.papercdcase.com";

    // Максимальное количество треков на форме
    private static final int MAX_TRACKS = 16;

    public static void main(String[] args) {
        System.out.println("=== Запуск Selenium-скрипта для papercdcase.com ===");

        // Читаем данные из файла data.txt
        List<String> data = readDataFile(DATA_FILE_PATH);
        if (data.size() < 3) {
            System.err.println("Ошибка: файл data.txt должен содержать минимум 3 строки "
                    + "(исполнитель, название альбома, хотя бы 1 трек)");
            return;
        }

        String artist = data.get(0);
        String title = data.get(1);
        List<String> tracks = data.subList(2, Math.min(data.size(), 2 + MAX_TRACKS));

        System.out.println("Исполнитель: " + artist);
        System.out.println("Альбом: " + title);
        System.out.println("Количество треков: " + tracks.size());

        // Настраиваем ChromeDriver
        System.setProperty("webdriver.chrome.driver",
                Paths.get(CHROMEDRIVER_PATH).toAbsolutePath().normalize().toString());

        ChromeOptions options = new ChromeOptions();
        // Разрешаем работу с небезопасными сертификатами (у сайта может быть просрочен)
        options.setAcceptInsecureCerts(true);
        // Отключаем уведомления браузера
        options.addArguments("--disable-notifications");
        // Устанавливаем настройки для автоматического скачивания PDF
        Map<String, Object> prefs = new HashMap<>();
        String downloadDir = Paths.get(RESULT_DIR).toAbsolutePath().normalize().toString();
        prefs.put("download.default_directory", downloadDir);
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = null;

        try {
            driver = new ChromeDriver(options);
            driver.manage().window().maximize();
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            System.out.println("Открываем страницу: " + BASE_URL);
            driver.get(BASE_URL);

            // Ожидаем загрузки страницы
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.name("artist")));

            System.out.println("Страница загружена. Заполняем форму...");

            // 1. Вводим имя исполнителя
            WebElement artistField = driver.findElement(By.name("artist"));
            artistField.clear();
            artistField.sendKeys(artist);
            System.out.println("  -> Artist: " + artist);

            // 2. Вводим название альбома
            WebElement titleField = driver.findElement(By.name("title"));
            titleField.clear();
            titleField.sendKeys(title);
            System.out.println("  -> Title: " + title);

            // 3. Вводим треки (каждый трек — отдельное поле track1..track16)
            for (int i = 0; i < tracks.size(); i++) {
                String trackName = "track" + (i + 1);
                WebElement trackField = driver.findElement(By.name(trackName));
                trackField.clear();
                trackField.sendKeys(tracks.get(i));
                System.out.println("  -> " + trackName + ": " + tracks.get(i));
            }

            // 4. Выбираем тип обложки: Jewel Case
            WebElement jewelRadio = driver.findElement(
                    By.cssSelector("input[name='template'][value='jewel']"));
            jewelRadio.click();
            System.out.println("  -> Type: Jewel Case");

            // 5. Выбираем формат бумаги: A4
            WebElement a4Radio = driver.findElement(
                    By.cssSelector("input[name='size'][value='a4']"));
            a4Radio.click();
            System.out.println("  -> Paper: A4");

            // 6. Собираем URL для скачивания PDF напрямую (форма использует GET)
            StringBuilder pdfUrl = new StringBuilder(BASE_URL);
            pdfUrl.append("/papercdcase.cgi/papercdcase.pdf?");
            pdfUrl.append("artist=").append(java.net.URLEncoder.encode(artist, "UTF-8"));
            pdfUrl.append("&title=").append(java.net.URLEncoder.encode(title, "UTF-8"));
            for (int i = 0; i < tracks.size(); i++) {
                pdfUrl.append("&track").append(i + 1).append("=")
                      .append(java.net.URLEncoder.encode(tracks.get(i), "UTF-8"));
            }
            pdfUrl.append("&template=jewel");
            pdfUrl.append("&size=a4");
            pdfUrl.append("&lang=west");

            System.out.println("\nОтправляем форму и скачиваем PDF...");

            // Создаём директорию result, если её нет
            Path resultDirPath = Paths.get(RESULT_DIR).toAbsolutePath().normalize();
            Files.createDirectories(resultDirPath);

            // Скачиваем PDF напрямую по URL
            Path resultFilePath = Paths.get(RESULT_FILE).toAbsolutePath().normalize();
            downloadPdf(pdfUrl.toString(), resultFilePath);

            System.out.println("\n=== Готово! ===");
            System.out.println("PDF-файл сохранён: " + resultFilePath);

        } catch (Exception e) {
            System.err.println("Ошибка при выполнении скрипта: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Закрываем браузер
            if (driver != null) {
                driver.quit();
                System.out.println("Браузер закрыт.");
            }
        }
    }

    /**
     * Читает данные из файла data.txt.
     * Формат файла:
     *   Строка 1: Исполнитель
     *   Строка 2: Название альбома
     *   Строки 3+: Названия треков (по одному на строку)
     *
     * @param filePath путь к файлу
     * @return список строк из файла
     */
    private static List<String> readDataFile(String filePath) {
        List<String> lines = new ArrayList<>();
        Path path = Paths.get(filePath).toAbsolutePath().normalize();
        System.out.println("Читаем данные из: " + path);

        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка при чтении файла " + path + ": " + e.getMessage());
        }

        return lines;
    }

    /**
     * Скачивает PDF-файл по указанному URL и сохраняет в указанный путь.
     *
     * @param urlString URL для скачивания
     * @param outputPath путь для сохранения файла
     * @throws IOException в случае ошибки ввода-вывода
     */
    private static void downloadPdf(String urlString, Path outputPath) throws IOException {
        System.out.println("Скачиваем PDF с URL: " + urlString);

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setInstanceFollowRedirects(true);

        int responseCode = connection.getResponseCode();
        System.out.println("HTTP Response Code: " + responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (InputStream inputStream = connection.getInputStream()) {
                Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
            }
            long fileSize = Files.size(outputPath);
            System.out.println("Файл сохранён (" + fileSize + " байт): " + outputPath);
        } else {
            throw new IOException("Ошибка HTTP: код ответа " + responseCode);
        }

        connection.disconnect();
    }
}
