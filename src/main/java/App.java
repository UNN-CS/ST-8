package com.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.time.Duration;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class App {
    public static void main(String[] args) {
        WebDriver driver = null;

        try {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--ignore-certificate-errors");
            options.addArguments("--allow-insecure-localhost");
            options.addArguments("--remote-allow-origins=*");

            driver = new ChromeDriver(options);
            driver.manage().window().maximize();
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            driver.get("http://www.papercdcase.com");
            Thread.sleep(3000);
            List<String> data = readDataFromFile("data/data.txt");

            String artist = extractValue(data, "Artist:");
            String title = extractValue(data, "Title:");
            String type = extractValue(data, "Type:");
            String paper = extractValue(data, "Paper:");
            List<String> tracks = extractTracks(data);

            // Поле Artist
            WebElement artistField = driver.findElement(By.xpath("//*[contains(text(),'Artist')]/following::input[1]"));
            artistField.clear();
            artistField.sendKeys(artist);

            // Поле Title
            WebElement titleField = driver.findElement(By.xpath("//*[contains(text(),'Title')]/following::input[1]"));
            titleField.clear();
            titleField.sendKeys(title);

            // Поле Type (тип упаковки)
            if (!type.isEmpty()) {
                try {
                    WebElement typeRadio = driver.findElement(By.xpath("//input[@value='" + type.toLowerCase() + "']"));
                    if (!typeRadio.isSelected()) {
                        typeRadio.click();
                    }
                    System.out.println("Выбран тип: " + type);
                } catch (Exception e) {
                    System.out.println("Тип '" + type + "' не найден, используется значение по умолчанию");
                }
            } else {
                // Если Type не указан в файле, выбираем Jewel Case по умолчанию
                try {
                    WebElement jewelCaseRadio = driver.findElement(By.xpath("//input[@value='jewel']"));
                    if (!jewelCaseRadio.isSelected()) {
                        jewelCaseRadio.click();
                    }
                    System.out.println("Выбран тип: Jewel Case (по умолчанию)");
                } catch (Exception e) {
                    System.out.println("Не найден Jewel Case");
                }
            }

            // Поле Paper (формат бумаги)
            if (!paper.isEmpty()) {
                try {
                    WebElement paperRadio = driver.findElement(By.xpath("//input[@value='" + paper.toLowerCase() + "']"));
                    if (!paperRadio.isSelected()) {
                        paperRadio.click();
                    }
                    System.out.println("Выбран формат бумаги: " + paper);
                } catch (Exception e) {
                    System.out.println("Формат бумаги '" + paper + "' не найден, используется A4");
                    // Fallback на A4
                    try {
                        WebElement a4Radio = driver.findElement(By.xpath("//input[@value='a4']"));
                        if (!a4Radio.isSelected()) {
                            a4Radio.click();
                        }
                        System.out.println("Выбран формат: A4 (по умолчанию)");
                    } catch (Exception ex) {
                        System.out.println("Формат A4 не найден");
                    }
                }
            } else {
                // Если Paper не указан в файле, выбираем A4 по умолчанию
                try {
                    WebElement a4Radio = driver.findElement(By.xpath("//input[@value='a4']"));
                    if (!a4Radio.isSelected()) {
                        a4Radio.click();
                    }
                    System.out.println("Выбран формат: A4 (по умолчанию)");
                } catch (Exception e) {
                    System.out.println("Формат A4 не найден");
                }
            }

            // Заполнение треков
            List<WebElement> allInputs = driver.findElements(By.xpath("//input[@type='text']"));
            int trackStartIndex = 2; // Первые 2 поля - Artist и Title

            for (int i = 0; i < tracks.size() && i < 18; i++) {
                if (trackStartIndex + i < allInputs.size()) {
                    WebElement trackField = allInputs.get(trackStartIndex + i);
                    trackField.clear();
                    trackField.sendKeys(tracks.get(i));
                    System.out.println("Трек " + (i + 1) + ": " + tracks.get(i));
                }
            }

            // Кнопка создания
            WebElement createButton = driver.findElement(By.xpath("//input[@type='submit' or @value='create cd case']"));
            createButton.click();

            // Ожидание генерации
            Thread.sleep(5000);

            // Создание папки result
            Files.createDirectories(Paths.get("result"));

            // Сохранение PDF (если генерируется)
            try {
                // Переключаемся на новую вкладку, если открылась
                String originalWindow = driver.getWindowHandle();
                for (String windowHandle : driver.getWindowHandles()) {
                    if (!windowHandle.equals(originalWindow)) {
                        driver.switchTo().window(windowHandle);
                        break;
                    }
                }

                // Сохраняем содержимое как PDF
                byte[] content = driver.getPageSource().getBytes();
                Files.write(Paths.get("result/cd.pdf"), content);
                System.out.println("PDF сохранен в result/cd.pdf");

                // Закрываем дополнительную вкладку
                driver.close();
                driver.switchTo().window(originalWindow);
            } catch (Exception e) {
                System.out.println("Не удалось сохранить PDF: " + e.getMessage());
            }

            System.out.println("Готово! Браузер закроется через 30 секунд...");
            Thread.sleep(30000);

        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private static List<String> readDataFromFile(String filePath) throws Exception {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            System.out.println("Файл data.txt не найден, создаю с тестовыми данными...");
            Files.createDirectories(path.getParent());
            List<String> defaultData = List.of(
                    "Artist: Marilyn Manson",
                    "Title: Mechanical Animals",
                    "Type: jewel",
                    "Paper: a4",
                    "Tracks:",
                    "1. Great Big White World",
                    "2. The Dope Show",
                    "3. Mechanical Animals",
                    "4. Rock Is Dead",
                    "5. Disassociative",
                    "6. The Speed of Pain",
                    "7. Posthuman",
                    "8. I Want to Disappear",
                    "9. I Don't Like the Drugs (But the Drugs Like Me)",
                    "10. New Model No. 15",
                    "11. User Friendly",
                    "12. Fundamentally Loathsome",
                    "13. The Last Day on Earth",
                    "14. Coma White"
            );
            Files.write(path, defaultData);
            return defaultData;
        }
        return Files.readAllLines(path);
    }

    private static String extractValue(List<String> data, String key) {
        for (String line : data) {
            if (line.startsWith(key)) {
                return line.substring(key.length()).trim();
            }
        }
        return "";
    }

    private static List<String> extractTracks(List<String> data) {
        List<String> tracks = new ArrayList<>();
        boolean inTracks = false;

        for (String line : data) {
            if (line.equals("Tracks:")) {
                inTracks = true;
                continue;
            }
            if (inTracks && line.matches("^\\d+\\..*")) {
                String track = line.replaceFirst("^\\d+\\.\\s*", "").trim();
                if (!track.isEmpty()) {
                    tracks.add(track);
                }
            }
        }
        return tracks;
    }
}