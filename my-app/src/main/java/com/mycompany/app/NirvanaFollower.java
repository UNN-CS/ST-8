package com.mycompany.app;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.By;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class NirvanaFollower {
    private WebDriver webDriver;
    private AlbumData albumData;
    private String filePath = "C:\\Projects\\ST-8\\result";
    NirvanaFollower(){
        System.setProperty(" webdriver.chrome.driver", "C:\\Projects\\chrome-win64.zip");
        webDriver = new ChromeDriver();
        webDriver.get("https://www.papercdcase.com/index.php");
    }
    private void readFileData(){
        albumData = new AlbumData();
        try {
            FileReader fileReader = new FileReader("..\\data\\data.txt");
            BufferedReader reader = new BufferedReader(fileReader);
            var line = reader.readLine();
            while (line != null){
                if (albumData.albumName == null){
                    albumData.albumName = line;
                } else if (albumData.albumAuthor == null) {
                    albumData.albumAuthor = line;
                }else {
                    albumData.songs.add(line);
                }
                line = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void sendData(){
        readFileData();
        webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input")).sendKeys(albumData.albumName);
        webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input")).sendKeys(albumData.albumAuthor);
        var table =  webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table"));
        for (int i = 0; i < albumData.songs.size(); ++i){
            if (i < 8){
                table.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[" + String.valueOf(i+ + 1) + "]/td[2]/input")).sendKeys(albumData.songs.get(i));
            }else{
                table.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[" + String.valueOf(i - 7) + "]/td[2]/input")).sendKeys(albumData.songs.get(i));
            }
        }
        webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]")).click();
        webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]")).click();
        webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input")).submit();
    }

    public void saveData() {
        String pdfUrl = webDriver.getCurrentUrl();
        try {
            URL url = new URL(pdfUrl);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.addRequestProperty("User-Agent", "Mozilla/5.0");
            try (InputStream inputStream = httpConn.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream("C:\\Projects\\ST-8\\result\\cd.pdf")) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } finally {
                httpConn.disconnect();
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
