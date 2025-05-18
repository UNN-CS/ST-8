package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class App {
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "/Users/nikitakazunin/Downloads/chromedriver-mac-arm64/chromedriver");

        WebDriver driver = new ChromeDriver();

        try {
            driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

            driver.get("http://www.papercdcase.com/index.php");

            String artist = "";
            String title = "";
            String tracks = "";

            try (BufferedReader br = new BufferedReader(new FileReader("data/data.txt"))) {
                artist = br.readLine();
                title = br.readLine();
                StringBuilder tracksBuilder = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    tracksBuilder.append(line).append("\n");
                }
                tracks = tracksBuilder.toString().trim();
            } catch (IOException e) {
                System.err.println("Ошибка чтения файла data.txt: " + e.getMessage());
                return;
            }

            WebElement artistField = driver.findElement(By.xpath("//input[@name='Artist']"));
            WebElement titleField = driver.findElement(By.xpath("//input[@name='Title']"));
            WebElement tracksField = driver.findElement(By.xpath("//textarea[@name='Tracks']"));

            artistField.sendKeys(artist);
            titleField.sendKeys(title);
            tracksField.sendKeys(tracks);

            WebElement formatA4 = driver.findElement(By.xpath("//input[@value='a4']"));
            WebElement jewelCase = driver.findElement(By.xpath("//input[@value='jewel']"));

            formatA4.click();
            jewelCase.click();

            WebElement submitButton = driver.findElement(By.xpath("//input[@type='submit']"));
            submitButton.click();

            Thread.sleep(5000);


            System.out.println("Обложка CD успешно создана!");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}