package org.example;

import org.openqa.selenium.By;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ST-8: заполнение формы на www.papercdcase.com через Selenium
 * и сохранение сгенерированной обложки CD в result/cd.pdf.
 */
public class App {

    // На базовой форме сайта доступно ровно 16 полей для треков
    private static final int MAX_TRACKS = 16;
    private static final String BASE_URL = "http://www.papercdcase.com";
    // Имя, под которым Chrome сохранит файл (последний сегмент URL формы)
    private static final String DOWNLOADED_NAME = "papercdcase.pdf";

    public static void main(String[] args) throws Exception {
        String projectDir = System.getProperty("user.dir");

        // Локальные сборки Chrome и ChromeDriver, лежащие в проекте
        String chromeBinary = Paths.get(projectDir, "chrome-win64", "chrome.exe").toString();
        String chromeDriver = Paths.get(projectDir, "chromedriver-win64", "chromedriver.exe").toString();
        System.setProperty("webdriver.chrome.driver", chromeDriver);

        // Читаем исходные данные обложки
        CoverData data = readData(Paths.get(projectDir, "data", "data.txt"));
        System.out.println("Artist: " + data.artist);
        System.out.println("Title:  " + data.title);
        System.out.println("Tracks: " + data.tracks.size());

        // Папка для результата и целевой файл
        Path resultDir = Paths.get(projectDir, "result");
        Files.createDirectories(resultDir);
        Path downloaded = resultDir.resolve(DOWNLOADED_NAME);
        Path target = resultDir.resolve("cd.pdf");
        // Удаляем возможные остатки прошлых запусков, чтобы не спутать файлы
        Files.deleteIfExists(downloaded);

        ChromeOptions options = new ChromeOptions();
        options.setBinary(chromeBinary);
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--start-maximized");
        // Сертификат сайта просрочен, а форма отдаёт PDF по HTTPS -> разрешаем
        // небезопасные сертификаты, иначе скачивание заблокируется.
        options.setAcceptInsecureCerts(true);
        // Открытый во встроенном просмотрщике PDF "подвешивает" драйвер
        // (страница никогда не догружается). Поэтому заставляем Chrome НЕ открывать
        // PDF, а сразу скачивать его в папку result.
        options.setPageLoadStrategy(PageLoadStrategy.NONE);
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", resultDir.toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            // Задание 3: открываем базовый адрес страницы
            driver.get(BASE_URL);

            // Получаем доступ к полям формы и впечатываем данные
            wait.until(ExpectedConditions.presenceOfElementLocated(By.name("artist")));
            driver.findElement(By.name("artist")).sendKeys(data.artist);
            driver.findElement(By.name("title")).sendKeys(data.title);

            int count = Math.min(data.tracks.size(), MAX_TRACKS);
            for (int i = 0; i < count; i++) {
                driver.findElement(By.name("track" + (i + 1))).sendKeys(data.tracks.get(i));
            }

            // Программно выбираем переключатели: формат A4 и Jewel Case
            driver.findElement(By.xpath("//input[@name='size' and @value='a4']")).click();
            driver.findElement(By.xpath("//input[@name='template' and @value='jewel']")).click();

            // Активируем генерацию обложки -> Chrome скачает PDF в result
            WebElement form = driver.findElement(By.xpath("//form"));
            form.submit();

            // Ждём появления полностью скачанного файла (без .crdownload)
            waitForDownload(downloaded, resultDir, Duration.ofSeconds(60));

            // Сохраняем под требуемым именем result/cd.pdf
            Files.move(downloaded, target, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Saved: " + target + " (" + Files.size(target) + " bytes)");
        } finally {
            driver.quit();
        }
    }

    /** Простой контейнер для данных обложки. */
    private static class CoverData {
        String artist = "";
        String title = "";
        List<String> tracks = new ArrayList<>();
    }

    /**
     * Формат data.txt:
     *   строка "Artist: ..."  — исполнитель
     *   строка "Title: ..."   — название альбома
     *   остальные непустые строки — названия треков (по одному в строке)
     */
    private static CoverData readData(Path dataFile) throws IOException {
        CoverData data = new CoverData();
        for (String raw : Files.readAllLines(dataFile, StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            String lower = line.toLowerCase();
            if (lower.startsWith("artist:")) {
                data.artist = line.substring(line.indexOf(':') + 1).trim();
            } else if (lower.startsWith("title:")) {
                data.title = line.substring(line.indexOf(':') + 1).trim();
            } else {
                data.tracks.add(line);
            }
        }
        return data;
    }

    /** Ждёт, пока файл скачается и исчезнут временные файлы .crdownload. */
    private static void waitForDownload(Path file, Path dir, Duration timeout)
            throws IOException, InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            boolean partial = false;
            try (var stream = Files.newDirectoryStream(dir, "*.crdownload")) {
                partial = stream.iterator().hasNext();
            }
            if (Files.exists(file) && Files.size(file) > 0 && !partial) {
                return;
            }
            Thread.sleep(500);
        }
        throw new IOException("PDF не был скачан за " + timeout.getSeconds() + " c");
    }
}
