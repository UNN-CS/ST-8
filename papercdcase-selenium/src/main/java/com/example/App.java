package com.example;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        String projectPath = System.getProperty("user.dir");
        String downloadPath = projectPath + File.separator + "result";
        String dataPath = projectPath + File.separator + "data" + File.separator + "data.txt";

        new File(downloadPath).mkdirs();

        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);
        options.addArguments("--remote-allow-origins=*");

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadPath);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.manage().window().maximize();
            driver.get("http://www.papercdcase.com");

            List<String> lines = Files.readAllLines(Paths.get(dataPath));
            Thread.sleep(5000);

            List<WebElement> textFields = driver.findElements(By.cssSelector("input[type='text']"));

            if (textFields.size() > 0) {
                textFields.get(0).sendKeys(lines.get(0));
                textFields.get(1).sendKeys(lines.get(1));

                for (int i = 2; i < lines.size() && i < 20; i++) {
                    int fieldIndex = i;
                    if (fieldIndex < textFields.size()) {
                        textFields.get(fieldIndex).sendKeys(lines.get(i));
                    }
                }
            }

            try {
                WebElement jewelRadio = driver.findElement(By.cssSelector("input[value='jewel']"));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", jewelRadio);
            } catch (Exception e) {
            }

            try {
                textFields.get(0).submit();
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript(
                        "document.forms[0].submit();"
                );
            }

            Thread.sleep(15000);

            File dir = new File(downloadPath);
            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));

            if (files != null && files.length > 0) {
                File downloaded = files[0];
                File target = new File(downloadPath + File.separator + "cd.pdf");
                if (target.exists()) {
                    target.delete();
                }
                Files.move(downloaded.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

        } catch (Exception e) {
        } finally {
            driver.quit();
        }
    }
}