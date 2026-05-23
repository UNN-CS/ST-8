package com.mycompany.app;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import java.util.List;

public class FindElements 
{
    public static void main( String[] args )
    {
        System.setProperty("webdriver.chrome.driver", "chromedriver.exe");
        WebDriver webDriver = new ChromeDriver();
        try {
            webDriver.get("http://www.papercdcase.com");
            
            Thread.sleep(3000);
            
            List<WebElement> inputs = webDriver.findElements(By.tagName("input"));
            System.out.println("=== INPUT элементов ===");
            for (int i = 0; i < inputs.size(); i++) {
                WebElement el = inputs.get(i);
                System.out.println(i + ": name=" + el.getAttribute("name") + 
                                   ", type=" + el.getAttribute("type") +
                                   ", id=" + el.getAttribute("id"));
            }
            
            List<WebElement> textareas = webDriver.findElements(By.tagName("textarea"));
            System.out.println("\n=== TEXTAREA элементов ===");
            for (int i = 0; i < textareas.size(); i++) {
                WebElement el = textareas.get(i);
                System.out.println(i + ": name=" + el.getAttribute("name") +
                                   ", id=" + el.getAttribute("id"));
            }
            
            List<WebElement> selects = webDriver.findElements(By.tagName("select"));
            System.out.println("\n=== SELECT элементов ===");
            for (int i = 0; i < selects.size(); i++) {
                WebElement el = selects.get(i);
                System.out.println(i + ": name=" + el.getAttribute("name"));
            }
            
            List<WebElement> buttons = webDriver.findElements(By.xpath("//input[@type='submit']"));
            System.out.println("\n=== SUBMIT кнопок ===");
            for (int i = 0; i < buttons.size(); i++) {
                WebElement el = buttons.get(i);
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