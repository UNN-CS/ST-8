package com.mycompany.app;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.*;

import java.nio.charset.Charset;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

public class App {
    public static void main(String[] args) throws Exception {
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\ivank\\Desktop\\testing\\chromedriver-win64\\chromedriver.exe");

        String downloadDir = "C:\\Users\\ivank\\Desktop\\testing\\ST-8\\result";
        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadDir);
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get("http://www.papercdcase.com/index.php");
            System.out.println("✅ Сайт открыт");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input")));

            Path dataPath = Paths.get("C:\\Users\\ivank\\Desktop\\testing\\ST-8\\data\\data.txt");
            if (!Files.exists(dataPath)) {
                System.out.println("❌ Файл не найден: " + dataPath.toString());
                driver.quit();
                return;
            }

            List<String> lines = Files.readAllLines(dataPath, Charset.forName("windows-1251"));
            String artist = lines.get(0);
            String title = lines.get(1);
            List<String> tracks = lines.subList(2, lines.size());

            WebElement artistField = wait.until(ExpectedConditions.elementToBeClickable(By.name("artist")));
            artistField.clear();
            artistField.sendKeys(artist);

            driver.findElement(By.name("title")).sendKeys(title);

            for (int i = 0; i < Math.min(tracks.size(), 10); i++) {
                String trackName = "track" + (i + 1);
                WebElement trackField = driver.findElement(By.name(trackName));
                trackField.sendKeys(tracks.get(i));
            }

            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[1]")).click();
            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[1]")).click();

            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input")).click();

            System.out.println("⏳ Ожидание загрузки PDF...");
            Thread.sleep(5000); 

            System.out.println("✅ PDF должен быть в папке: " + downloadDir);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}
