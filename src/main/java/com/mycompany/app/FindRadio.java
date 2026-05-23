package com.mycompany.app;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import java.util.List;

public class FindRadio 
{
    public static void main( String[] args )
    {
        System.setProperty("webdriver.chrome.driver", "chromedriver.exe");
        WebDriver webDriver = new ChromeDriver();
        try {
            webDriver.get("http://www.papercdcase.com");
            
            Thread.sleep(3000);
            
            List<WebElement> radios = webDriver.findElements(By.xpath("//input[@type='radio']"));
            System.out.println("=== RADIO кнопки ===");
            for (int i = 0; i < radios.size(); i++) {
                WebElement el = radios.get(i);
                System.out.println(i + ": name=" + el.getAttribute("name") + 
                                   ", value=" + el.getAttribute("value"));
            }
            
            Thread.sleep(5000);
            
        } catch (Exception e) {
            System.out.println("Error");
            System.out.println(e.toString());
        } finally {
            webDriver.quit();
        }
    }
}