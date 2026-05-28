package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Select;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class App
{
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "C:/drivers/chromedriver-win64/chromedriver.exe");

        String resultDir = "C:/ST-8/ST-8/result";

        try {
            Files.createDirectories(Paths.get(resultDir));

            Map<String, String> data = loadData("data/data.txt");

            System.out.println("=== Заполнение формы ===");
            System.out.println("Artist: " + data.get("artist"));
            System.out.println("Title: " + data.get("title"));

            ChromeOptions options = new ChromeOptions();
            options.setAcceptInsecureCerts(true);

            Map<String, Object> prefs = new HashMap<>();
            prefs.put("download.default_directory", new File(resultDir).getAbsolutePath());
            prefs.put("download.prompt_for_download", false);
            prefs.put("plugins.always_open_pdf_externally", true);
            options.setExperimentalOption("prefs", prefs);

            WebDriver driver = new ChromeDriver(options);

            try {
                System.out.println("\nОткрываем страницу...");
                driver.get("http://www.papercdcase.com");
                Thread.sleep(3000);

                WebElement artistField = driver.findElement(By.xpath("//input[@name='artist']"));
                artistField.sendKeys(data.get("artist"));

                WebElement titleField = driver.findElement(By.xpath("//input[@name='title']"));
                titleField.sendKeys(data.get("title"));

                for (int i = 1; i <= 18; i++) {
                    String track = data.get("track" + i);
                    if (track != null && !track.isEmpty()) {
                        WebElement trackField = driver.findElement(By.xpath("//input[@name='track" + i + "']"));
                        trackField.sendKeys(track);
                    }
                }

                WebElement jewelRadio = driver.findElement(By.xpath("//input[@value='jewel']"));
                if (!jewelRadio.isSelected()) {
                    jewelRadio.click();
                }

                WebElement a4Radio = driver.findElement(By.xpath("//input[@value='a4']"));
                if (!a4Radio.isSelected()) {
                    a4Radio.click();
                }

                WebElement submitBtn = driver.findElement(By.xpath("//input[@type='submit']"));
                submitBtn.click();

                System.out.println("\nФорма отправлена, PDF генерируется...");

                Thread.sleep(5000);

                File pdfFile = new File(resultDir, "papercdcase.pdf");
                if (pdfFile.exists()) {
                    Files.copy(pdfFile.toPath(), Paths.get(resultDir, "cd.pdf"), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("\nPDF сохранён: " + resultDir + "/cd.pdf");
                } else {
                    System.out.println("\n⚠ PDF не найден, пробуем скачать через HTTP...");
                    downloadViaHttp(data, resultDir);
                }

            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
                System.out.println("\nПробуем скачать через HTTP...");
                downloadViaHttp(data, resultDir);
            } finally {
                Thread.sleep(3000);
                driver.quit();
            }

        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private static Map<String, String> loadData(String file) throws Exception {
        Map<String, String> data = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Artist:")) {
                    data.put("artist", line.substring(7).trim());
                } else if (line.startsWith("Title:")) {
                    data.put("title", line.substring(6).trim());
                } else if (line.startsWith("Track")) {
                    int idx = line.indexOf(":");
                    if (idx > 0) {
                        String trackNum = line.substring(6, idx).trim();
                        data.put("track" + trackNum, line.substring(idx + 1).trim());
                    }
                }
            }
        }
        return data;
    }

    private static void downloadViaHttp(Map<String, String> data, String dir) {
        try {
            StringBuilder url = new StringBuilder("http://www.papercdcase.com/papercdcase.cgi/papercdcase.pdf?");
            url.append("artist=").append(URLEncoder.encode(data.get("artist"), "UTF-8"));
            url.append("&title=").append(URLEncoder.encode(data.get("title"), "UTF-8"));
            for (int i = 1; i <= 18; i++) {
                String t = data.getOrDefault("track" + i, "");
                url.append("&track").append(i).append("=").append(URLEncoder.encode(t, "UTF-8"));
            }
            url.append("&template=jewel&size=a4&lang=west&force_saveas=yes&submit.x=0&submit.y=0");

            System.out.println("Отправляем HTTP запрос...");
            HttpURLConnection conn = (HttpURLConnection) new URL(url.toString()).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(dir + "/cd.pdf")) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            }
            System.out.println("PDF сохранён через HTTP: " + dir + "/cd.pdf");
        } catch (Exception e) {
            System.err.println("Ошибка HTTP скачивания: " + e.getMessage());
        }
    }
}