package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        String downloadDir = new File("result").getAbsolutePath();
        new File(downloadDir).mkdirs();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadDir);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            driver.get("http://www.papercdcase.com");

            List<String> lines = Files.readAllLines(Paths.get("data", "data.txt"));
            if (lines.size() < 2) {
                System.out.println("Not enough data in data.txt");
                return;
            }

            WebElement artistInput = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input"));
            artistInput.sendKeys(lines.get(0));

            WebElement titleInput = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input"));
            titleInput.sendKeys(lines.get(1));

            int trackIndex = 2;
            for (int col = 1; col <= 2; col++) {
                for (int row = 1; row <= 8; row++) {
                    if (trackIndex < lines.size()) {
                        String xpath = String.format("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[%d]/table/tbody/tr[%d]/td[2]/input", col, row);
                        WebElement trackInput = driver.findElement(By.xpath(xpath));
                        trackInput.sendKeys(lines.get(trackIndex));
                        trackIndex++;
                    }
                }
            }

            WebElement typeJewel = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]"));
            typeJewel.click();

            WebElement sizeA4 = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]"));
            sizeA4.click();

            WebElement submitBtn = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input"));
            submitBtn.submit();

            // Wait for download
            File downloadedFile = new File(downloadDir, "papercdcase.pdf");
            long timeout = System.currentTimeMillis() + 30000;
            while (!downloadedFile.exists() && System.currentTimeMillis() < timeout) {
                Thread.sleep(500);
            }

            if (downloadedFile.exists()) {
                File renamedFile = new File(downloadDir, "cd.pdf");
                Files.move(downloadedFile.toPath(), renamedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Success: cd.pdf has been generated in the result directory.");
            } else {
                System.out.println("Error: Download timed out.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}
