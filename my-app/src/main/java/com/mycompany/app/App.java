package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class App {
    private static final String BASE_URL = "https://www.papercdcase.com";
    private static final String DATA_FILE = "data/data.txt";
    private static final String RESULT_PATH = "C:/SoftwareTesting/ST-8/result";
    private static final String OUTPUT_PDF = RESULT_PATH + "/cd.pdf";

    private static final String XPATH_ARTIST = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input";
    private static final String XPATH_TITLE = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input";

    private static final String[] XPATH_TRACKS = {
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[1]/td[2]/input",
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[2]/td[2]/input",
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[3]/td[2]/input",
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[4]/td[2]/input",
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[5]/td[2]/input",
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[6]/td[2]/input",
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[7]/td[2]/input",
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[8]/td[2]/input",
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[1]/td[2]/input",
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[2]/td[2]/input",
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[3]/td[2]/input",
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[4]/td[2]/input",
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[5]/td[2]/input",
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[6]/td[2]/input",
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[7]/td[2]/input",
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[8]/td[2]/input"
    };

    private static final String XPATH_TYPE_JEWEL = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]";
    private static final String XPATH_PAPER_A4 = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]";
    private static final String XPATH_FONT_WESTERN = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[6]/td[2]/input[1]";
    private static final String XPATH_SUBMIT = "/html/body/table[2]/tbody/tr/td[1]/div/form/p/input";

    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "D:/Dwlnds/chromedriver-win64/chromedriver-win64/chromedriver.exe");

        ChromeOptions options = new ChromeOptions();
        options.setBinary("D:/Dwlnds/chrome-win64/chrome-win64/chrome.exe");
        options.setAcceptInsecureCerts(true);
        options.addArguments("--start-maximized");

        WebDriver driver = new ChromeDriver(options);

        try {
            System.out.println("Чтение данных...");
            Map<String, String> data = readDataFile(DATA_FILE);

            System.out.println("Открытие страницы...");
            driver.get(BASE_URL);
            Thread.sleep(2000);

            System.out.println("✍Заполнение формы...");
            driver.findElement(By.xpath(XPATH_ARTIST)).sendKeys(data.get("Artist"));
            driver.findElement(By.xpath(XPATH_TITLE)).sendKeys(data.get("Title"));

            for (int i = 0; i < XPATH_TRACKS.length; i++) {
                String trackKey = String.format("Track%02d", i + 1);
                if (data.containsKey(trackKey)) {
                    driver.findElement(By.xpath(XPATH_TRACKS[i])).sendKeys(data.get(trackKey));
                }
            }

            System.out.println("Выбор параметров...");
            driver.findElement(By.xpath(XPATH_TYPE_JEWEL)).click();
            driver.findElement(By.xpath(XPATH_PAPER_A4)).click();
            driver.findElement(By.xpath(XPATH_FONT_WESTERN)).click();

            System.out.println("Поиск кнопки...");
            WebElement submitBtn = driver.findElement(By.xpath(XPATH_SUBMIT));
            System.out.println("Кнопка найдена!");

            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", submitBtn);
            Thread.sleep(500);

            System.out.println("Отправка формы...");
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitBtn);

            System.out.println("Ожидание генерации PDF...");
            Thread.sleep(10000);

            String currentUrl = driver.getCurrentUrl();
            System.out.println("URL: " + currentUrl);

            if (currentUrl.contains("papercdcase.pdf")) {
                downloadFile(currentUrl, OUTPUT_PDF);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("Закрытие браузера...");
            driver.quit();
        }
    }

    private static void downloadFile(String urlStr, String outputPath) {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            URL url = new URL(urlStr);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            System.out.println("Код ответа: " + conn.getResponseCode());

            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(outputPath)) {
                byte[] buffer = new byte[4096];
                int len;
                long total = 0;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                    total += len;
                }
                if (total > 100) {
                    System.out.println("PDF сохранён: " + outputPath + " (" + total + " байт)");
                } else {
                    System.out.println("Файл маленький: " + total + " байт");
                }
            }
            conn.disconnect();

            HttpsURLConnection.setDefaultSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());

        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
        }
    }

    private static Map<String, String> readDataFile(String path) throws IOException {
        Map<String, String> data = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || !line.contains(":")) continue;
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    data.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return data;
    }
}