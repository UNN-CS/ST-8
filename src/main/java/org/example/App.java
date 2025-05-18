package org.example;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.nio.file.*;
import java.util.List;
import java.util.HashMap;
import java.time.Duration;
import java.io.*;

public class App {
    public static void main(String[] args) {
        // Настройка путей и директорий

        Path projectDir = Paths.get(System.getProperty("user.dir"));
        Path dataPath = Paths.get(System.getProperty("user.dir"), "data", "data.txt");


        // Настройка драйвера Chrome
        System.setProperty("webdriver.chrome.driver", "D:\\Games\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");
        System.setProperty("webdriver.chrome.whitelistedIps", "");
        System.setProperty("webdriver.chrome.silentOutput", "true");

        Path downloadDir = Paths.get(System.getProperty("java.io.tmpdir"), "selenium_download_" + System.currentTimeMillis());
        Path resultDir = Paths.get(System.getProperty("user.dir"), "result");

        try {
            // Создание необходимых директорий
            Files.createDirectories(resultDir);
            Files.createDirectories(downloadDir);

            if (!Files.exists(dataPath)) {
                System.err.println("Ошибка: Файл данных не найден: " + dataPath);
                return;
            }

            // Настройка параметров Chrome для скачивания
            HashMap<String, Object> chromePrefs = new HashMap<>();
            chromePrefs.put("download.default_directory", downloadDir.toString());
            chromePrefs.put("download.prompt_for_download", false);
            chromePrefs.put("plugins.always_open_pdf_externally", true);

            ChromeOptions options = new ChromeOptions();
            options.setExperimentalOption("prefs", chromePrefs);
            options.addArguments("--window-size=1920,1080");

            WebDriver driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            try {
                // Чтение данных из файла
                List<String> data = Files.readAllLines(dataPath);
                if (data.size() < 4) {
                    System.err.println("Ошибка: Файл данных должен содержать как минимум 4 строки");
                    return;
                }

                String artist = data.get(0).split(":")[1].trim();
                String title = data.get(1).split(":")[1].trim();

                // Открытие страницы
                driver.get("https://www.papercdcase.com/index.php");

                // Заполнение полей Artist и Title
                WebElement artistField = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//input[contains(@name, 'artist')]")));
                artistField.clear();
                artistField.sendKeys(artist);

                WebElement titleField = driver.findElement(By.xpath("//input[contains(@name, 'title')]"));
                titleField.clear();
                titleField.sendKeys(title);

                // Заполнение треков (2 варианта в зависимости от структуры формы)
                try {
                    // Вариант 1: Текстовое поле для всех треков
                    WebElement tracksField = driver.findElement(By.xpath("//textarea[contains(@name, 'tracks')]"));
                    tracksField.clear();

                    for (int i = 3; i < data.size(); i++) {
                        String track = data.get(i).replaceFirst("^\\d+\\.\\s*", "");
                        tracksField.sendKeys(track + "\n");
                    }
                } catch (NoSuchElementException e) {
                    // Вариант 2: Отдельные поля для каждого трека
                    for (int i = 0; i < data.size() - 3; i++) {
                        String track = data.get(i + 3).replaceFirst("^\\d+\\.\\s*", "");
                        String xpath = String.format("//input[contains(@name, 'track%d')]", i + 1);
                        WebElement trackField = driver.findElement(By.xpath(xpath));
                        trackField.clear();
                        trackField.sendKeys(track);
                    }
                }

                // Выбор типа коробки (Jewel case)
                WebElement jewelCase = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//input[@type='radio' and contains(@value, 'jewel')]")));
                if (!jewelCase.isSelected()) {
                    jewelCase.click();
                }

                // Выбор формата бумаги (A4)
                WebElement a4Paper = driver.findElement(
                        By.xpath("//input[@type='radio' and contains(@value, 'a4')]"));
                if (!a4Paper.isSelected()) {
                    a4Paper.click();
                }



                // Ожидание и обработка скачивания PDF
                boolean pdfFound = false;
                Path pdfFile = null;
                long startTime = System.currentTimeMillis();
                long timeout = 30000; // 30 секунд

                while (!pdfFound && System.currentTimeMillis() - startTime < timeout) {
                    try {
                        pdfFile = Files.list(downloadDir)
                                .filter(file -> file.toString().toLowerCase().endsWith(".pdf"))
                                .findFirst()
                                .orElse(null);

                        if (pdfFile != null) {
                            pdfFound = true;
                        } else {
                            Thread.sleep(500);
                        }
                    } catch (Exception e) {
                        Thread.sleep(500);
                    }
                }

                if (!pdfFound) {
                    System.err.println("Ошибка: PDF файл не был создан");
                    return;
                }

                // Сохранение результата
                Path resultFile = resultDir.resolve("cd.pdf");
                Files.copy(pdfFile, resultFile, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Файл успешно сохранен: " + resultFile);

            } catch (Exception e) {
                System.err.println("Ошибка при выполнении: " + e.getMessage());
                e.printStackTrace();
            } finally {
                driver.quit();
                // Очистка временной директории
                try {
                    Files.walk(downloadDir)
                            .sorted(java.util.Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (IOException e) {
                    System.err.println("Ошибка при очистке временных файлов: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при инициализации: " + e.getMessage());
            e.printStackTrace();
        }
    }
}