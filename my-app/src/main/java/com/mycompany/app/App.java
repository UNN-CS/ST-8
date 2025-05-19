package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "S:/chromedriver-win64/chromedriver.exe");
        Path dir = Paths.get(System.getProperty("user.dir"), "..", "result").normalize();
        try { Files.createDirectories(dir); } catch (Exception ignored) {}

        ChromeOptions opts = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", dir.toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        opts.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(opts);
        try {
            driver.get("https://www.papercdcase.com/index.php");
            Map<String, Object> data = readCDData();
            driver.findElement(By.name("artist")).sendKeys((String) data.get("Artist"));
            driver.findElement(By.name("title")).sendKeys((String) data.get("Title"));

            @SuppressWarnings("unchecked")
            List<String> tracks = (List<String>) data.get("Tracks");
            for (int i = 0; i < Math.min(16, tracks.size()); i++) {
                String xp = i < 8 ? "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[" + (i + 1) + "]/td[2]/input"
                        : "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[" + (i - 7) + "]/td[2]/input";
                driver.findElement(By.xpath(xp)).sendKeys(tracks.get(i));
            }

            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]")).click();
            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]")).click();
            driver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input")).click();

            Thread.sleep(10000);

            Path[] pdfs = Files.list(dir).filter(p -> p.toString().endsWith(".pdf")).toArray(Path[]::new);
            if (pdfs.length > 0) {
                Path latest = pdfs[0];
                for (Path p : pdfs) {
                    if (Files.getLastModifiedTime(p).toMillis() > Files.getLastModifiedTime(latest).toMillis()) latest = p;
                }
                Path out = dir.resolve("cd.pdf");
                if (Files.exists(out)) Files.delete(out);
                Files.copy(latest, out);
                Files.delete(latest);
                System.out.println("Saved cd.pdf");
            } else {
                System.out.println("No PDF in " + dir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    private static Map<String, Object> readCDData() throws IOException {
        Map<String, Object> data = new HashMap<>();
        List<String> tracks = new ArrayList<>();
        Path dataPath = Paths.get(System.getProperty("user.dir"), "..", "data", "data.txt").normalize();
        try (BufferedReader r = new BufferedReader(new FileReader(dataPath.toFile()))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("Artist:")) data.put("Artist", line.substring(7));
                else if (line.startsWith("Title:")) data.put("Title", line.substring(6));
                else if (!line.isEmpty() && !line.startsWith("Tracks:")) {
                    int sp = line.indexOf(' ');
                    tracks.add(sp > 0 ? line.substring(sp + 1) : line);
                }
            }
        }
        data.put("Tracks", tracks);
        return data;
    }
}