package com.mycompany.app;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

public class App {
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "C:\\Webdrivers\\chromedriver-win64\\chromedriver.exe");
        String downloadFilepath = Paths.get("..", "result").toAbsolutePath().normalize().toString();
        Path resultDir = Paths.get(downloadFilepath);

        try {
            Files.createDirectories(resultDir);
        } catch (IOException e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadFilepath);
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        try {
            // Чтение данных
            Map<String, Object> cdData = readDataFromFile(Paths.get("..", "data", "data.txt"));
            
            // Открытие страницы
            driver.get("http://www.papercdcase.com/index.php");
            
            // Заполнение формы
            fillFormFields(driver, cdData);
            
            // Отправка формы
            submitForm(driver);
            
            // Ожидание и обработка PDF
            waitForAndSavePdf(driver, Paths.get("..", "result"));

        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    private static Map<String, Object> readDataFromFile(Path filePath) throws IOException {
        Map<String, Object> data = new HashMap<>();
        List<String> lines = Files.readAllLines(filePath);
        List<String> tracks = new ArrayList<>();

        data.put("Artist", lines.get(0).split(":")[1].trim());
        data.put("Title", lines.get(1).split(":")[1].trim());
        
        for (int i = 3; i < lines.size(); i++) {
            if (!lines.get(i).trim().isEmpty()) {
                tracks.add(lines.get(i).trim());
            }
        }
        data.put("Tracks", tracks);
        
        return data;
    }

    private static void fillFormFields(WebDriver driver, Map<String, Object> cdData) {
        WebElement artistField = driver.findElement(
            By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input"));
        artistField.sendKeys((String) cdData.get("Artist"));

        WebElement titleField = driver.findElement(
            By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input"));
        titleField.sendKeys((String) cdData.get("Title"));

        @SuppressWarnings("unchecked")
        List<String> tracks = (List<String>) cdData.get("Tracks");

        for (int i = 0; i < Math.min(8, tracks.size()); i++) {
            WebElement trackField = driver.findElement(
                By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[" + (i + 1) + "]/td[2]/input"));
            trackField.sendKeys(tracks.get(i));
        }
        for (int i = 8; i < Math.min(16, tracks.size()); i++) {
            WebElement trackField = driver.findElement(
                By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[" + (i - 7) + "]/td[2]/input"));
            trackField.sendKeys(tracks.get(i));
        }

        WebElement caseTypeRadio = driver.findElement(
            By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]"));
        if (!caseTypeRadio.isSelected()) caseTypeRadio.click();

        WebElement paperRadio = driver.findElement(
            By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]"));
        if (!paperRadio.isSelected()) paperRadio.click();
    }

    private static void submitForm(WebDriver driver) {
        WebElement submitButton = driver.findElement(
            By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input"));
        submitButton.click();
    }

    private static void waitForAndSavePdf(WebDriver driver, Path resultDir) throws Exception {
        Thread.sleep(5000);
        
        Optional<Path> latestPdf = Files.list(resultDir)
            .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
            .max(Comparator.comparingLong(p -> {
                    try {
                        return Files.getLastModifiedTime(p).toMillis();
                    } catch (IOException e) {
                        return 0L;
                    }
                }));
        
        if (latestPdf.isPresent()) {
            Path target = resultDir.resolve("cd.pdf");
            Files.move(latestPdf.get(), target, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("PDF успешно сохранен как: " + target);
        } else {
            System.out.println("PDF не был найден в папке " + resultDir);
        }
    }
}