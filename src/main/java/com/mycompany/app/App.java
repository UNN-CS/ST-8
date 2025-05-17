package com.mycompany.app;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import java.io.*;
import java.util.*;

public class App {
    public static void main(String[] args) {
        String artist, title;
        ArrayList<String> tracks = new ArrayList<>();

        try (BufferedReader r = new BufferedReader(new FileReader("./data/data.txt"))) {
            artist = r.readLine();
            title = r.readLine();
            String line;
            while ((line = r.readLine()) != null) {
                tracks.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", new File("./result").getAbsolutePath());
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);

        driver.get("http://www.papercdcase.com/index.php");


        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("document.querySelector('form').setAttribute('action', 'papercdcase.cgi/cd.pdf')");
            
            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input")).sendKeys(artist);

            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input")).sendKeys(title);

            for (int i = 0; i < tracks.size(); i++) {
                driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[" + (i + 1) + "]/td[2]/input")).sendKeys(tracks.get(i));
            }

            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[1]")).click();
            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[1]")).click();

            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input")).click();

            Thread.sleep(5000);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}
