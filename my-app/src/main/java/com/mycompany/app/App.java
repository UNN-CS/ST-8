package com.mycompany.app;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class App {
  public static void main(String[] args) {
    System.setProperty("webdriver.chrome.driver", "D:\\chromedriver-win64\\chromedriver.exe");

    WebDriver driver = new ChromeDriver();
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

    try {
      List<String> fileLines = Files.readAllLines(Paths.get("../data", "data.txt"));

      String artist = fileLines.get(0);
      String title = fileLines.get(1);

      List<String> trackList = new ArrayList<>();
      for (int i = 2; i < fileLines.size(); i++) {
        String line = fileLines.get(i).trim();
        if (!line.isEmpty()) {
          trackList.add(line);
        }
      }

      driver.get("http://www.papercdcase.com/");

      driver
          .findElement(
              By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input"))
          .sendKeys(artist);
      driver
          .findElement(
              By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input"))
          .sendKeys(title);

      for (int i = 0; i < trackList.size() && i < 16; i++) {
        int col = (i < 8) ? 1 : 2;
        int row = (i < 8) ? (i + 1) : (i - 7);

        String trackXpath =
            String.format("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/"
                    + "table/tbody/tr/td[%d]/table/tbody/tr[%d]/td[2]/input",
                col, row);
        driver.findElement(By.xpath(trackXpath)).sendKeys(trackList.get(i));
      }

      driver
          .findElement(By.xpath(
              "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]"))
          .click();
      driver
          .findElement(By.xpath(
              "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]"))
          .click();

      WebElement submitButton =
          driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input"));
      submitButton.submit();

      wait.until(ExpectedConditions.urlContains(".pdf"));
      String currentUrl = driver.getCurrentUrl();

      Path resultDir = Paths.get("../result");

      try (InputStream in = URI.create(currentUrl).toURL().openStream()) {
        Files.copy(in, resultDir.resolve("cd.pdf"), StandardCopyOption.REPLACE_EXISTING);
      }

      System.out.println("Сохранено: result/cd.pdf");

    } catch (Exception e) {
      System.err.println("Ошибка: " + e.getMessage());
      e.printStackTrace();
    } finally {
      driver.quit();
    }
  }
}
