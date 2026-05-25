
package com.papercdcase;

import org.openqa.selenium.By;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class App {

    public static void main(String[] args) throws Exception {
        App runner = new App();
        runner.start();
    }

    public void start() throws Exception {
        Map<String, String> info = parseFile("data/data.txt");
        String outputPath = Paths.get("result").toAbsolutePath().toString();
        Files.createDirectories(Paths.get(outputPath));

        WebDriverManager.chromedriver().setup();

        ChromeSettings cfg = new ChromeSettings();
        ChromeOptions opts = cfg.prepareOptions(outputPath);

        WebDriver browser = new ChromeDriver(opts);
        browser.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));

        boolean success = false;

        try {
            browser.get("http://www.papercdcase.com");
            Thread.sleep(2000);

            inputText(browser, "artist", info.get("artist"));
            inputText(browser, "title", info.get("title"));
            
            for (int num = 1; num <= 18 && info.containsKey("track" + num); num++) {
                inputText(browser, "track" + num, info.get("track" + num));
            }

            selectRadio(browser, "//input[@value='jewel']");
            selectRadio(browser, "//input[@value='a4']");

            browser.findElement(By.xpath("//input[@type='submit']")).click();

            File downloadedFile = new File(outputPath, "papercdcase.pdf");
            File tempFile = new File(outputPath, "papercdcase.pdf.crdownload");

            for (int waitCount = 0; waitCount < 15; waitCount++) {
                if (downloadedFile.exists() && downloadedFile.length() > 1000 && !tempFile.exists()) {
                    Files.copy(downloadedFile.toPath(), Paths.get(outputPath, "cd.pdf"), StandardCopyOption.REPLACE_EXISTING);
                    success = true;
                    System.out.println("PDF готов: " + outputPath + "/cd.pdf");
                    break;
                }
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
        } finally {
            Thread.sleep(1000);
            browser.quit();
        }

        if (!success) {
            httpRequest(info, outputPath);
        }
    }

    private Map<String, String> parseFile(String filename) throws Exception {
        Map<String, String> storage = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Artist:")) {
                    storage.put("artist", line.substring(7).trim());
                } else if (line.startsWith("Title:")) {
                    storage.put("title", line.substring(6).trim());
                } else if (line.startsWith("Track ")) {
                    int pos = line.indexOf(":");
                    if (pos > 0) {
                        storage.put("track" + line.substring(6, pos).trim(), line.substring(pos + 1).trim());
                    }
                }
            }
        }
        return storage;
    }

    private void inputText(WebDriver driver, String fieldName, String value) {
        try { 
            driver.findElement(By.name(fieldName)).sendKeys(value); 
        } catch (Exception ignore) {}
    }

    private void selectRadio(WebDriver driver, String xpathExpression) {
        try { 
            driver.findElement(By.xpath(xpathExpression)).click(); 
        } catch (Exception ignore) {}
    }

    private void httpRequest(Map<String, String> info, String folder) {
        try {
            StringBuilder query = new StringBuilder("http://www.papercdcase.com/papercdcase.cgi/papercdcase.pdf?");
            query.append("artist=").append(URLEncoder.encode(info.get("artist"), "UTF-8"));
            query.append("&title=").append(URLEncoder.encode(info.get("title"), "UTF-8"));
            
            for (int idx = 1; idx <= 16; idx++) {
                String val = info.getOrDefault("track" + idx, "");
                query.append("&track").append(idx).append("=").append(URLEncoder.encode(val, "UTF-8"));
            }
            
            query.append("&template=jewel&size=a4&lang=west&force_saveas=yes&submit.x=0&submit.y=0");

            HttpURLConnection conn = (HttpURLConnection) new URL(query.toString()).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            try (InputStream inStream = conn.getInputStream();
                 FileOutputStream outStream = new FileOutputStream(folder + "/cd.pdf")) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = inStream.read(buffer)) > 0) {
                    outStream.write(buffer, 0, count);
                }
            }
            System.out.println("PDF получен: " + folder + "/cd.pdf");

        } catch (Exception err) {
            System.err.println("HTTP ошибка: " + err.getMessage());
        }
    }
}

class ChromeSettings {
    
    public ChromeOptions prepareOptions(String downloadFolder) {
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--no-sandbox", "--disable-dev-shm-usage");
        opts.addArguments("--disable-gpu", "--disable-blink-features=AutomationControlled");
        opts.setAcceptInsecureCerts(true);
        opts.setPageLoadStrategy(PageLoadStrategy.EAGER);

        Map<String, Object> settings = new HashMap<>();
        settings.put("download.default_directory", downloadFolder);
        settings.put("download.prompt_for_download", false);
        settings.put("plugins.always_open_pdf_externally", true);
        opts.setExperimentalOption("prefs", settings);
        
        return opts;
    }
}
