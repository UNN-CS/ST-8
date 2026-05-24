package com.mycompany.app;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class App {
    public static void main(String[] args) throws Exception {
        System.setProperty("webdriver.chrome.driver", "C:/tmp/chrome-win64/chromedriver-win64/chromedriver-win64/chromedriver.exe");

        File resultDir = new File("result");
        WebDriver driver = getWebDriver(resultDir);

        try {
            List<String> lines = Files.readAllLines(Paths.get("data/data.txt"));

            driver.get("http://www.papercdcase.com");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input")));

            WebElement artist = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input"));
            artist.clear();
            artist.sendKeys(lines.get(0).trim());

            WebElement title = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input"));
            title.clear();
            title.sendKeys(lines.get(1).trim());

            int trackIdx = 2;

            for (int row = 1; row <= 8 && trackIdx < lines.size(); row++) {
                String leftXPath = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[" + row + "]/td[2]/input";
                WebElement leftField = driver.findElement(By.xpath(leftXPath));
                leftField.clear();
                leftField.sendKeys(lines.get(trackIdx).trim());
                trackIdx++;
            }

            for (int row = 1; row <= 8 && trackIdx < lines.size(); row++) {
                String rightXPath = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[" + row + "]/td[2]/input";
                WebElement rightField = driver.findElement(By.xpath(rightXPath));
                rightField.clear();
                rightField.sendKeys(lines.get(trackIdx).trim());
                trackIdx++;
            }

            WebElement jewel = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]"));
            if (!jewel.isSelected()) jewel.click();

            WebElement a4 = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]"));
            if (!a4.isSelected()) a4.click();

            WebElement submit = driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input"));
            submit.click();

            File downloaded = new File(resultDir, "papercdcase.pdf");
            File crdownload = new File(resultDir, "papercdcase.pdf.crdownload");
            long deadline = System.currentTimeMillis() + 30000;

            while (System.currentTimeMillis() < deadline) {
                if (downloaded.exists() && downloaded.length() > 0) {
                    File target = new File(resultDir, "cd.pdf");
                    Files.move(downloaded.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    break;
                }
                if (crdownload.exists()) {
                    Thread.sleep(1000);
                    continue;
                }
                Thread.sleep(200);
            }

        } finally {
            Thread.sleep(3000);
            driver.quit();
        }
    }

    private static WebDriver getWebDriver(File resultDir) {
        ChromeOptions options = new ChromeOptions();
        options.setBinary("C:/tmp/chrome-win64/chrome-win64/chrome.exe");
        options.addArguments("--lang=ru-RU");

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", resultDir.getAbsolutePath());
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);

        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-web-security");
        options.addArguments("--disable-features=BlockInsecureDownloads");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--ignore-ssl-errors");
        options.addArguments("--allow-insecure-localhost");

        return new ChromeDriver(options);
    }
}