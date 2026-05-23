import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class App {
    public static void main(String[] args) {
        String dataFilePath = "data/data.txt";
        String resultDirPath = "result";
        
        File resultDir = new File(resultDirPath);
        if (!resultDir.exists()) {
            resultDir.mkdirs();
        }

        String downloadPath = resultDir.getAbsolutePath();

        String artist = "";
        String title = "";
        List<String> tracks = new ArrayList<>();

        try (Scanner scanner = new Scanner(new File(dataFilePath))) {
            boolean readingTracks = false;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("Artist:")) {
                    artist = line.substring(7).trim();
                } else if (line.startsWith("Title:")) {
                    title = line.substring(6).trim();
                } else if (line.startsWith("Tracks:")) {
                    readingTracks = true;
                } else if (readingTracks) {
                    tracks.add(line);
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading data.txt: " + e.getMessage());
            return;
        }

        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadPath);
        prefs.put("plugins.always_open_pdf_externally", true);
        prefs.put("download.prompt_for_download", false);
        options.setExperimentalOption("prefs", prefs);
        options.setAcceptInsecureCerts(true);

        WebDriver webDriver = new ChromeDriver(options);

        try {
            webDriver.get("http://www.papercdcase.com/");
            
            System.out.println("Current URL: " + webDriver.getCurrentUrl());
            System.out.println("Page Title: " + webDriver.getTitle());

            webDriver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(10));

            WebElement artistInput = webDriver.findElement(By.name("artist"));
            artistInput.sendKeys(artist);

            WebElement titleInput = webDriver.findElement(By.name("title"));
            titleInput.sendKeys(title);

            for (int i = 0; i < tracks.size() && i < 16; i++) {
                WebElement trackInput = webDriver.findElement(By.name("track" + (i + 1)));
                trackInput.sendKeys(tracks.get(i));
            }

            WebElement templateJewel = webDriver.findElement(By.xpath("//input[@name='template' and @value='jewel']"));
            templateJewel.click();

            WebElement sizeA4 = webDriver.findElement(By.xpath("//input[@name='size' and @value='a4']"));
            sizeA4.click();

            WebElement submitBtn = webDriver.findElement(By.name("submit"));
            submitBtn.submit();

            System.out.println("Waiting for download...");
            Thread.sleep(5000); 

            File dir = new File(downloadPath);
            File[] files = dir.listFiles((d, name) -> name.endsWith(".pdf"));
            boolean found = false;
            if (files != null) {
                for (File file : files) {
                    if (file.getName().equals("cd.pdf")) continue;
                    Path source = file.toPath();
                    Path target = Paths.get(downloadPath, "cd.pdf");
                    Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("PDF saved to result/cd.pdf");
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.out.println("Could not find the downloaded PDF.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            webDriver.quit();
        }
    }
}
