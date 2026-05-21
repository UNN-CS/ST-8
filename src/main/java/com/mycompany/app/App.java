package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class App {
    public static void main(String[] args) throws Exception {
        List<String> data = Files.readAllLines(Paths.get("data/data.txt"));

        System.setProperty("webdriver.chrome.driver", "C:/chromedriver/chromedriver.exe");
        ChromeDriver webDriver = new ChromeDriver();

        webDriver.get("http://www.papercdcase.com");
        String form = "/html/body/table[2]/tbody/tr/td[1]/div/form";
        webDriver.findElement(By.xpath(form + "/table/tbody/tr[1]/td[2]/input")).sendKeys(data.get(0));
        webDriver.findElement(By.xpath(form + "/table/tbody/tr[2]/td[2]/input")).sendKeys(data.get(1));

        for (int i = 0; i + 2 < data.size(); i++) {
            webDriver.findElement(By.xpath(
                form + "/table/tbody/tr[3]/td[2]/table/tbody/tr/td[" + (i / 8 + 1) +
                "]/table/tbody/tr[" + (i % 8 + 1) + "]/td[2]/input"
            )).sendKeys(data.get(i + 2));
        }

        webDriver.findElement(By.xpath(form + "/table/tbody/tr[4]/td[2]/input[2]")).click();
        webDriver.findElement(By.xpath(form + "/table/tbody/tr[5]/td[2]/input[2]")).click();
        webDriver.findElement(By.xpath(form + "/p/input")).submit();

        Files.write(Paths.get("result/cd.pdf"),
            new URL(webDriver.getCurrentUrl()).openStream().readAllBytes());

        webDriver.quit();
        System.exit(0);
    }
}
