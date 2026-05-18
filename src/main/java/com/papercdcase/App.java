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
        Map<String, String> data = loadData("data/data.txt");
        String resultDir = Paths.get("result").toAbsolutePath().toString();
        Files.createDirectories(Paths.get(resultDir));

        WebDriverManager.chromedriver().browserVersion("147").setup();

        ChromeOptions options = new ChromeOptions();
        options.setBinary("/snap/bin/chromium");
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--remote-debugging-port=9222");
        options.addArguments("--user-data-dir=/tmp/chrome-" + System.currentTimeMillis());
        options.addArguments("--disable-gpu", "--disable-blink-features=AutomationControlled");
        options.setAcceptInsecureCerts(true);
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", resultDir);
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));

        boolean downloaded = false;

        try {
            driver.get("http://www.papercdcase.com");
            Thread.sleep(2000);

            fillField(driver, "artist", data.get("artist"));
            fillField(driver, "title", data.get("title"));
            for (int i = 1; i <= 18 && data.containsKey("track" + i); i++) {
                fillField(driver, "track" + i, data.get("track" + i));
            }

            clickElement(driver, "//input[@value='jewel']");
            clickElement(driver, "//input[@value='a4']");

            driver.findElement(By.xpath("//input[@type='submit']")).click();

            File pdfFile = new File(resultDir, "papercdcase.pdf");
            File crFile = new File(resultDir, "papercdcase.pdf.crdownload");

            for (int i = 0; i < 15; i++) {
                if (pdfFile.exists() && pdfFile.length() > 1000 && !crFile.exists()) {
                    Files.copy(pdfFile.toPath(), Paths.get(resultDir, "cd.pdf"), StandardCopyOption.REPLACE_EXISTING);
                    downloaded = true;
                    break;
                }
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            System.err.println("Browser method: " + e.getMessage());
        } finally {
            Thread.sleep(1000);
            driver.quit();
        }

        if (!downloaded) {
            downloadViaHttp(data, resultDir);
        }
    }

    private static Map<String, String> loadData(String file) throws Exception {
        Map<String, String> data = new HashMap<>();
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("Artist:")) data.put("artist", line.substring(7).trim());
                else if (line.startsWith("Title:")) data.put("title", line.substring(6).trim());
                else if (line.startsWith("Track ")) {
                    int idx = line.indexOf(":");
                    if (idx > 0) data.put("track" + line.substring(6, idx).trim(), line.substring(idx + 1).trim());
                }
            }
        }
        return data;
    }

    private static void fillField(WebDriver driver, String name, String value) {
        try { driver.findElement(By.name(name)).sendKeys(value); } catch (Exception e) {}
    }

    private static void clickElement(WebDriver driver, String xpath) {
        try { driver.findElement(By.xpath(xpath)).click(); } catch (Exception e) {}
    }

    private static void downloadViaHttp(Map<String, String> data, String dir) {
        try {
            StringBuilder url = new StringBuilder("http://www.papercdcase.com/papercdcase.cgi/papercdcase.pdf?");
            url.append("artist=").append(URLEncoder.encode(data.get("artist"), "UTF-8"));
            url.append("&title=").append(URLEncoder.encode(data.get("title"), "UTF-8"));
            for (int i = 1; i <= 16; i++) {
                String t = data.getOrDefault("track" + i, "");
                url.append("&track").append(i).append("=").append(URLEncoder.encode(t, "UTF-8"));
            }
            url.append("&template=jewel&size=a4&lang=west&force_saveas=yes&submit.x=0&submit.y=0");

            HttpURLConnection conn = (HttpURLConnection) new URL(url.toString()).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(dir + "/cd.pdf")) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }
            System.out.println("PDF сохранен: " + dir + "/cd.pdf");

        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
        }
    }
}