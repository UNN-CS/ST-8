package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class App {

    public static void main(String[] args) {

        System.setProperty("webdriver.chrome.driver", "D:\\chromedriver-win64\\chromedriver.exe");

        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", "D:\\Testing\\ST-8\\result");
        prefs.put("download.prompt_for_download", false);
        options.setExperimentalOption("prefs", prefs);

        WebDriver webDriver = new ChromeDriver(options);
        try {
            webDriver.get("http://www.papercdcase.com/index.php");

            List<String> lines = Files.readAllLines(Paths.get("..\\data\\data.txt"));
            webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input")).sendKeys(lines.get(0));
            webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input")).sendKeys(lines.get(1));
            for(int i = 0, j = 0; i * 8 + j + 2 < lines.size(); j++){
                if(j > 7){
                    j = 0;
                    i += 1;
                }
                String xPath = String.format("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[%d]/table/tbody/tr[%d]/td[2]/input", i+1, j+1);
                webDriver.findElement(By.xpath(xPath)).sendKeys(lines.get(i*8+j+2));
            }
            webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]")).click();
            webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]")).click();
            webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[7]/td[2]/input")).click();
            webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input")).submit();

            File downloadedFile = new File("D:\\Testing\\ST-8\\result\\papercdcase.pdf");
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(30));
            wait.until (d -> downloadedFile.exists());
            Files.move(
                downloadedFile.toPath(),
                Paths.get("D:\\Testing\\ST-8\\result\\cd.pdf"),
                StandardCopyOption.REPLACE_EXISTING
            );

        } catch (Exception e) {
            System.out.println("Error");
            System.out.println(e.toString());
        }
    }

}
