package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class App {
    public static void main(String[] args) {
        List<String> dataLines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("data/data.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                dataLines.add(line);
            }
        } catch (Exception e) {
            System.out.println("Error reading data.txt: " + e.getMessage());
            return;
        }

        if (dataLines.size() < 2) {
            System.out.println("Not enough data in data.txt");
            return;
        }

        String artist = dataLines.get(0);
        String title = dataLines.get(1);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver webDriver = new ChromeDriver(options);
        webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));

        try {
            webDriver.get("http://www.papercdcase.com/advanced.php");

            WebElement artistInput = webDriver.findElement(By.name("artist"));
            artistInput.sendKeys(artist);

            WebElement titleInput = webDriver.findElement(By.name("title"));
            titleInput.sendKeys(title);

            for (int i = 2; i < dataLines.size() && i < 20; i++) {
                int trackIndex = i - 1;
                WebElement trackInput = webDriver.findElement(By.name("track" + trackIndex));
                trackInput.sendKeys(dataLines.get(i));
            }

            WebElement paperA4 = webDriver.findElement(By.xpath("//input[@name='size' and @value='a4']"));
            if (!paperA4.isSelected()) {
                paperA4.click();
            }

            WebElement typeJewel = webDriver.findElement(By.xpath("//input[@name='template' and @value='jewel']"));
            if (!typeJewel.isSelected()) {
                typeJewel.click();
            }

            WebElement submitBtn = webDriver.findElement(By.name("submit"));
            submitBtn.click();

            Thread.sleep(5000);

            String pdfUrl = webDriver.getCurrentUrl();
            if (!pdfUrl.contains("papercdcase.pdf")) {
                pdfUrl = "http://www.papercdcase.com/papercdcase.cgi/papercdcase.pdf";
            }

            File dir = new File("result");
            if (!dir.exists()) {
                dir.mkdir();
            }

            URL url = new URL(pdfUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0"); // Притворяемся браузером

            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream("result/cd.pdf")) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                System.out.println("PDF successfully downloaded to result/cd.pdf");
            }

        } catch (Exception e) {
            System.out.println("Error during execution: " + e.toString());
        } finally {
            webDriver.quit();
        }
    }
}