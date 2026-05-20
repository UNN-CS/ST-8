import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {
    private static final String BASE_URL = "http://www.papercdcase.com/";
    private static final String DATA_FILE = "data/data.txt";
    private static final String RESULT_PDF = "result/cd.pdf";

    public static void main(String[] args) {
        WebDriver webDriver = createChromeDriver();
        try {
            List<String> data = loadData(DATA_FILE);
            String artist = data.get(0);
            String title = data.get(1);
            List<String> tracks = data.subList(2, data.size());

            openBasePage(webDriver);

            webDriver.findElement(By.name("artist")).sendKeys(artist);
            webDriver.findElement(By.name("title")).sendKeys(title);

            for (int i = 0; i < tracks.size() && i < 16; i++) {
                webDriver.findElement(By.name("track" + (i + 1))).sendKeys(tracks.get(i));
            }

            webDriver.findElement(By.cssSelector("input[name='template'][value='jewel']")).click();
            webDriver.findElement(By.cssSelector("input[name='size'][value='a4']")).click();
            webDriver.findElement(By.cssSelector("input[name='force_saveas']")).click();

            WebElement btn = webDriver.findElement(By.name("submit"));
            btn.submit();

            String pdfUrl = waitForPdfUrl(webDriver);
            savePdf(pdfUrl, RESULT_PDF);
            System.out.println("PDF saved to " + new File(RESULT_PDF).getAbsolutePath());
        } catch (Exception e) {
            System.out.println("Error");
            System.out.println(e.toString());
        } finally {
            webDriver.quit();
        }
    }

    private static WebDriver createChromeDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--allow-insecure-localhost");
        options.addArguments("--disable-features=HttpsUpgrades,HttpsFirstMode");
        options.setAcceptInsecureCerts(true);

        Path downloadDir = Paths.get("result").toAbsolutePath();
        Map<String, Object> prefs = new HashMap<String, Object>();
        prefs.put("download.default_directory", downloadDir.toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);

        return new ChromeDriver(options);
    }

    private static String waitForPdfUrl(WebDriver webDriver) throws Exception {
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
        try {
            wait.until(driver -> driver.getCurrentUrl().contains("papercdcase.pdf"));
            return webDriver.getCurrentUrl();
        } catch (Exception ignored) {
            return buildPdfUrlFromForm(webDriver);
        }
    }

    private static String buildPdfUrlFromForm(WebDriver webDriver) throws Exception {
        StringBuilder query = new StringBuilder();
        appendParam(query, "artist", webDriver.findElement(By.name("artist")).getAttribute("value"));
        appendParam(query, "title", webDriver.findElement(By.name("title")).getAttribute("value"));

        for (int i = 1; i <= 16; i++) {
            String track = webDriver.findElement(By.name("track" + i)).getAttribute("value");
            if (track != null && !track.trim().isEmpty()) {
                appendParam(query, "track" + i, track);
            }
        }

        appendParam(query, "template", "jewel");
        appendParam(query, "size", "a4");
        appendParam(query, "force_saveas", "yes");

        return "http://www.papercdcase.com/papercdcase.cgi/papercdcase.pdf?" + query;
    }

    private static void appendParam(StringBuilder query, String name, String value) throws Exception {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (query.length() > 0) {
            query.append('&');
        }
        query.append(URLEncoder.encode(name, "UTF-8"));
        query.append('=');
        query.append(URLEncoder.encode(value, "UTF-8"));
    }

    private static void openBasePage(WebDriver webDriver) {
        webDriver.get(BASE_URL);
        if (webDriver.getCurrentUrl().startsWith("https://")) {
            webDriver.get(BASE_URL);
        }
    }

    private static List<String> loadData(String path) throws Exception {
        List<String> lines = new ArrayList<String>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        }
        if (lines.size() < 2) {
            throw new IllegalArgumentException("data.txt must contain artist, title and optional tracks");
        }
        return lines;
    }

    private static void savePdf(String pdfUrl, String outputPath) throws Exception {
        Path resultDir = Paths.get("result");
        Files.createDirectories(resultDir);

        HttpURLConnection connection = (HttpURLConnection) new URL(pdfUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);

        try (InputStream input = connection.getInputStream();
             FileOutputStream output = new FileOutputStream(outputPath)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        } finally {
            connection.disconnect();
        }
    }
}
