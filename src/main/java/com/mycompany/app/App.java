package com.mycompany.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class App {
    public static void main(String[] args) {
        try {
            printPassword();
        } catch (Exception e) {
            System.out.println("Error");
            System.out.println(e.toString());
        }

        Task2.run();
        Task3.run();
    }

    private static void printPassword() throws IOException {
        WebDriver driver = null;
        try {
            driver = SeleniumSupport.createChromeDriver();
            String password = Task1.generatePassword(driver);
            System.out.println(password);
        } catch (Exception e) {
            fallbackPrintPassword();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private static void fallbackPrintPassword() throws IOException {
        Path dataFile = Paths.get("data", "data.txt");
        List<String> lines = Files.readAllLines(dataFile, StandardCharsets.UTF_8);
        String artist = lines.isEmpty() ? "Unknown Artist" : lines.get(0);
        String title = lines.size() > 1 ? lines.get(1) : "Unknown Album";
        System.out.println("Password source: " + artist + " - " + title);
    }
}
