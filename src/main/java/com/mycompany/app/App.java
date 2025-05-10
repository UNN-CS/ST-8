package com.mycompany.app;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class App {
    public static void main(String[] args) {
        // 1. Настройка ChromeDriver
        System.setProperty("webdriver.chrome.driver", "D:\\chromedriver-win64\\chromedriver.exe");
        
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--remote-allow-origins=*");
        
        // Настройка загрузки файлов
        String downloadPath = "D:\\Games\\testing\\ST-8\\result";
        new File(downloadPath).mkdirs();
        
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadPath);
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);
        
        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        
        try {
            // 2. Открытие страницы
            driver.get("http://www.papercdcase.com/index.php");
            System.out.println("Страница загружена: " + driver.getTitle());
            
            // 3. Заполнение формы
            fillForm(driver, wait, "data/data.txt");
            
            // 4. Нажатие кнопки генерации
            clickGenerateButton(driver, wait);
            
            // 5. Ожидание и проверка загрузки файла
            waitForPdfDownload(downloadPath, 30);
            
        } catch (Exception e) {
            System.err.println("Ошибка:");
            e.printStackTrace();
            takeScreenshot(driver, "error.png");
        } finally {
            driver.quit();
        }
    }
    
    private static void fillForm(WebDriver driver, WebDriverWait wait, String dataFile) throws Exception {
        List<String> lines = Files.readAllLines(Paths.get(dataFile));
        String artist = lines.get(0).split(": ")[1];
        String title = lines.get(1).split(": ")[1];
        List<String> tracks = lines.subList(3, lines.size());
        
        // Заполнение Artist
        WebElement artistField = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input")));
        artistField.clear();
        artistField.sendKeys(artist);
        
        // Заполнение Title
        WebElement titleField = driver.findElement(
            By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input"));
        titleField.clear();
        titleField.sendKeys(title);
        
        // Заполнение Tracks (16 полей)
        for (int i = 0; i < Math.min(tracks.size(), 16); i++) {
            int row = (i < 8) ? (i + 1) : (i - 7);
            String column = (i < 8) ? "1" : "2";
            
            String xpath = String.format(
                "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[%s]/table/tbody/tr[%d]/td[2]/input",
                column, row);
            
            WebElement trackField = driver.findElement(By.xpath(xpath));
            trackField.clear();
            trackField.sendKeys(tracks.get(i));
        }
        
        // Выбор Type (Jewel Case)
        WebElement jewelCase = driver.findElement(
            By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[@value='jewel']"));
        if (!jewelCase.isSelected()) {
            jewelCase.click();
        }
        
        // Выбор Paper (A4)
        WebElement a4Paper = driver.findElement(
            By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[@value='a4']"));
        if (!a4Paper.isSelected()) {
            a4Paper.click();
        }
        
        System.out.println("Форма успешно заполнена");
    }
    
    private static void clickGenerateButton(WebDriver driver, WebDriverWait wait) {
        // Кнопка генерации
        WebElement generateButton = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input")));
        
        // Клик через JavaScript для надежности
        ((JavascriptExecutor)driver).executeScript("arguments[0].click();", generateButton);
        System.out.println("Кнопка генерации нажата");
    }
    
    private static void waitForPdfDownload(String folderPath, int timeoutSeconds) throws Exception {
        File folder = new File(folderPath);
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000);
        
        System.out.println("Ожидание загрузки файла в: " + folder.getAbsolutePath());
        
        while (System.currentTimeMillis() < endTime) {
            File[] files = folder.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".pdf") && 
                !name.toLowerCase().contains("temp") &&
                !name.toLowerCase().contains("crdownload"));
            
            if (files != null && files.length > 0) {
                File pdfFile = files[0];
                if (pdfFile.length() > 0) {
                    System.out.println("Файл успешно загружен: " + pdfFile.getName());
                    return;
                }
            }
            Thread.sleep(1000);
        }
        
        throw new RuntimeException("PDF файл не был загружен в течение " + timeoutSeconds + " секунд");
    }
    
    private static void takeScreenshot(WebDriver driver, String fileName) {
        try {
            File screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File(fileName));
            System.out.println("Скриншот сохранен: " + fileName);
        } catch (Exception e) {
            System.err.println("Ошибка при создании скриншота: " + e.getMessage());
        }
    }
}