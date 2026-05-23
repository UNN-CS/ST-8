package com.mycompany.app;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.By;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class App 
{
    public static void main( String[] args ) throws Exception
    {
        System.setProperty("webdriver.chrome.driver", "chromedriver.exe");
        WebDriver webDriver = new ChromeDriver();
        
        webDriver.get("http://www.papercdcase.com");
        
        Thread.sleep(3000);
        
        List<String> lines = Files.readAllLines(Paths.get("data/data.txt"));
        
        String artist = "";
        String title = "";
        String[] tracks = new String[16];
        int trackIndex = 0;
        
        for (String line : lines) {
            if (line.startsWith("Artist:")) {
                artist = line.substring(7).trim();
            } else if (line.startsWith("Title:")) {
                title = line.substring(6).trim();
            } else if (line.matches("^\\d+\\..*")) {
                String track = line.substring(line.indexOf(".") + 1).trim();
                if (trackIndex < tracks.length) {
                    tracks[trackIndex] = track;
                    trackIndex++;
                }
            }
        }
        
        webDriver.findElement(By.xpath("//input[@name='artist']")).sendKeys(artist);
        webDriver.findElement(By.xpath("//input[@name='title']")).sendKeys(title);
        
        for (int i = 0; i < trackIndex; i++) {
            webDriver.findElement(By.xpath("//input[@name='track" + (i + 1) + "']")).sendKeys(tracks[i]);
        }
        
        webDriver.findElement(By.xpath("//input[@name='template'][@value='jewel']")).click();
        
        webDriver.findElement(By.xpath("//input[@name='size'][@value='a4']")).click();
        
        webDriver.findElement(By.xpath("//input[@name='submit']")).click();
        
        Thread.sleep(5000);
        
        webDriver.quit();
    }
}