package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {

    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "D:\\chromedriver-win64\\chromedriver.exe");

        try {
            Path resultDir = Paths.get("result");
            if (!Files.exists(resultDir)) {
                Files.createDirectories(resultDir);
            }
            String downloadPath = resultDir.toAbsolutePath().toString();

            ChromeOptions options = new ChromeOptions();
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("download.default_directory", downloadPath);
            prefs.put("plugins.always_open_pdf_externally", true);
            prefs.put("safebrowsing.enabled", false);
            options.setExperimentalOption("prefs", prefs);

            options.addArguments("--unsafely-treat-insecure-origin-as-secure=http://www.papercdcase.com");
            options.addArguments("--allow-running-insecure-content");

            WebDriver webDriver = new ChromeDriver(options);

            List<String> lines = Files.readAllLines(Paths.get("data", "data.txt"));

            webDriver.get("http://www.papercdcase.com");
            Thread.sleep(2000);

            webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input")).sendKeys(lines.get(0));
            webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input")).sendKeys(lines.get(1));

            int trackIndex = 1;
            for (int i = 2; i < lines.size(); i++) {
                String track = lines.get(i).trim();
                if (track.isEmpty() || trackIndex > 16) continue;

                int col = (trackIndex <= 8) ? 1 : 2;
                int row = (trackIndex <= 8) ? trackIndex : (trackIndex - 8);
                String xpath = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[" + col + "]/table/tbody/tr[" + row + "]/td[2]/input";

                webDriver.findElement(By.xpath(xpath)).sendKeys(track);
                trackIndex++;
            }

            webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]")).click();
            webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]")).click();
            webDriver.findElement(By.xpath("//input[@type='checkbox']")).click();
            webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input")).submit();

            Thread.sleep(5000);

            File dir = new File(downloadPath);
            File[] files = dir.listFiles((d, name) -> name.endsWith(".pdf") && !name.equals("cd.pdf"));

            if (files != null && files.length > 0) {
                Files.move(files[0].toPath(), Paths.get(downloadPath, "cd.pdf"), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Успех. Файл cd.pdf сохранен в папке result");
            } else {
                System.out.println("Файл не найден");
            }

            webDriver.quit();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}