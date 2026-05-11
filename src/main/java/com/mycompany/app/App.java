package com.mycompany.app;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        /*
            artist
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input",
            title
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input",
            1 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[1]/td[2]/input",
            2 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[2]/td[2]/input",
            3 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[3]/td[2]/input",
            4 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[4]/td[2]/input",
            5 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[5]/td[2]/input",
            6 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[6]/td[2]/input",
            7 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[7]/td[2]/input",
            8 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[8]/td[2]/input",
            9 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[1]/td[2]/input",
            10 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[2]/td[2]/input",
            11 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[3]/td[2]/input",
            12 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[4]/td[2]/input",
            13 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[5]/td[2]/input",
            14 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[6]/td[2]/input",
            15 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[7]/td[2]/input",
            16 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[8]/td[2]/input",
            jewel case
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]",
            a4
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]",
            button
            "/html/body/table[2]/tbody/tr/td[1]/div/form/p/input",
        */
        System.setProperty("webdriver.chrome.driver", "/home/nikita/Downloads/chromedriver-linux64/chromedriver");
        String downloadFilepath = "result/";
        
        Map<String, Object> prefs = new HashMap<>();
        // Запретить открытие PDF в браузере
        prefs.put("plugins.always_open_pdf_externally", true);
        // Указать папку загрузки
        prefs.put("download.default_directory", downloadFilepath);
        
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", prefs);

        WebDriver webDriver = new ChromeDriver(options);
        try {
            webDriver.get("http://www.papercdcase.com");
            String[] addrs = {
            //artist
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input",
            //title
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input",
            //1 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[1]/td[2]/input",
            //2 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[2]/td[2]/input",
            //3 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[3]/td[2]/input",
            //4 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[4]/td[2]/input",
            //5 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[5]/td[2]/input",
            //6 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[6]/td[2]/input",
            //7 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[7]/td[2]/input",
            //8 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[8]/td[2]/input",
            //9 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[1]/td[2]/input",
            //10 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[2]/td[2]/input",
            //11 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[3]/td[2]/input",
            //12 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[4]/td[2]/input",
            //13 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[5]/td[2]/input",
            //14 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[6]/td[2]/input",
            //15 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[7]/td[2]/input",
            //16 track
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[8]/td[2]/input",
            //jewel case
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]",
            //a4
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]",
            //button
            "/html/body/table[2]/tbody/tr/td[1]/div/form/p/input"};
            
            
            List<String> lines = Files.readAllLines(Paths.get("data/data.txt"));
            
            String[] data = lines.toArray(new String[0]); // >= 18; >= min 3

            int i = 0;
            WebElement artiste = webDriver.findElement(By.xpath(addrs[i]));
            artiste.sendKeys(data[i]);
            i++;
            WebElement titlee = webDriver.findElement(By.xpath(addrs[i]));
            titlee.sendKeys(data[i]);
            i++;
            WebElement[] els = new WebElement[16];
            for (int j = 0; j + 2 < addrs.length - 3 && j < data.length && j < els.length; j++) {
                els[j] = webDriver.findElement(By.xpath(addrs[i]));
                els[j].sendKeys(data[i]);
                i++;
            }
            WebElement jewelfonte = webDriver.findElement(By.xpath(addrs[i]));
            jewelfonte.submit();
            i++;
            WebElement a4e = webDriver.findElement(By.xpath(addrs[i]));
            a4e.submit();
            i++;
            WebElement buttone = webDriver.findElement(By.xpath(addrs[i]));
            buttone.submit();
            i++;
            //System.out.println(elem.getText());
        } catch (Exception e) {
            System.out.println("Error");
            System.out.println(e.toString());
        }
        //System.out.println("Hello World!");
        webDriver.quit();
    }
}
