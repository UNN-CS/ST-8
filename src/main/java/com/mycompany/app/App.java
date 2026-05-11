package com.mycompany.app;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "D:\\chromedriver-win64\\chromedriver.exe");
        ChromeOptions options = new ChromeOptions();
        options.setBinary("D:\\chrome-win64\\chrome.exe");
        WebDriver webDriver = new ChromeDriver(options);
        try {

        } catch (NoSuchElementException ex) {
            System.out.println("Error: " + ex.getRawMessage());
        } catch (TimeoutException ex) {
            System.out.println("Error: timeout");
        } finally {
            webDriver.quit();
        }
    }
}
