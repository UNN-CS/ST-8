import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import io.github.bonigarcia.wdm.WebDriverManager;

public class App {
    private static final String PAGE_URL = "https://www.papercdcase.com";
    private static final int MAX_TRACKS = 18;

    public static void main(String[] args) throws Exception {
        Path projectRoot = Path.of("").toAbsolutePath();
        Path dataFile = projectRoot.resolve("data").resolve("data.txt");
        Path resultDir = projectRoot.resolve("result");
        Path resultFile = resultDir.resolve("cd.pdf").toAbsolutePath();

        CoverData coverData = readCoverData(dataFile);
        prepareResultFile(resultDir, resultFile);

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--allow-running-insecure-content");
        options.addArguments("--window-size=1400,1000");

        WebDriver driver = new ChromeDriver(options);
        try {
            driver.get(PAGE_URL);

            fillTextField(driver, "artist", coverData.artist());
            fillTextField(driver, "title", coverData.album());

            for (int i = 0; i < coverData.tracks().size(); i++) {
                fillTextField(driver, "track" + (i + 1), coverData.tracks().get(i));
            }

            selectRadio(driver, "template", "jewel");
            selectRadio(driver, "size", "a4");
            setCheckbox(driver, "force_saveas", true);

            WebElement submitButton = driver.findElement(By.name("submit"));
            submitButton.submit();

            String pdfUrl = buildPdfUrl(coverData);
            downloadPdf(pdfUrl, resultFile);
            System.out.println("PDF saved to " + resultFile);
        } finally {
            driver.quit();
        }
    }

    private static void fillTextField(WebDriver driver, String fieldName, String value) {
        WebElement field = driver.findElement(By.name(fieldName));
        field.clear();
        field.sendKeys(value);
    }

    private static void selectRadio(WebDriver driver, String fieldName, String value) {
        WebElement radio = driver.findElement(By.cssSelector(
                "input[type='radio'][name='" + fieldName + "'][value='" + value + "']"
        ));
        if (!radio.isSelected()) {
            radio.click();
        }
    }

    private static void setCheckbox(WebDriver driver, String fieldName, boolean checked) {
        WebElement checkbox = driver.findElement(By.name(fieldName));
        if (checkbox.isSelected() != checked) {
            checkbox.click();
        }
    }

    private static void prepareResultFile(Path resultDir, Path resultFile) throws IOException {
        Files.createDirectories(resultDir);
        Files.deleteIfExists(resultFile);
    }

    private static CoverData readCoverData(Path dataFile) throws IOException {
        List<String> lines = Files.readAllLines(dataFile).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();

        if (lines.size() < 3) {
            throw new IllegalArgumentException("data.txt must contain artist, album and at least one track");
        }

        List<String> tracks = lines.subList(2, Math.min(lines.size(), MAX_TRACKS + 2));
        return new CoverData(lines.get(0), lines.get(1), tracks);
    }

    private static String buildPdfUrl(CoverData coverData) {
        List<String> parameters = new ArrayList<>();
        parameters.add("artist=" + encode(coverData.artist()));
        parameters.add("title=" + encode(coverData.album()));

        for (int i = 0; i < coverData.tracks().size(); i++) {
            parameters.add("track" + (i + 1) + "=" + encode(coverData.tracks().get(i)));
        }

        parameters.add("template=jewel");
        parameters.add("size=a4");
        parameters.add("lang=west");
        parameters.add("force_saveas=yes");
        parameters.add("submit=");

        return PAGE_URL + "/papercdcase.cgi/papercdcase.pdf?" + String.join("&", parameters);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static void downloadPdf(String pdfUrl, Path resultFile) throws Exception {
        SSLContext sslContext = createInsecureSslContext();
        HostnameVerifier verifier = (hostname, session) -> true;

        HttpsURLConnection connection = (HttpsURLConnection) new URL(pdfUrl).openConnection();
        connection.setSSLSocketFactory(sslContext.getSocketFactory());
        connection.setHostnameVerifier(verifier);
        connection.setRequestMethod("GET");
        connection.setConnectTimeout((int) Duration.ofSeconds(30).toMillis());
        connection.setReadTimeout((int) Duration.ofSeconds(60).toMillis());
        connection.connect();

        String contentType = connection.getContentType();
        if (contentType == null || !contentType.toLowerCase().contains("pdf")) {
            throw new IOException("Expected PDF response but got: " + contentType);
        }

        try (InputStream inputStream = connection.getInputStream();
             OutputStream outputStream = Files.newOutputStream(resultFile)) {
            inputStream.transferTo(outputStream);
        } finally {
            connection.disconnect();
        }
    }

    private static SSLContext createInsecureSslContext() throws Exception {
        TrustManager[] trustAllCertificates = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[0];
                    }
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCertificates, new java.security.SecureRandom());
        return sslContext;
    }

    private record CoverData(String artist, String album, List<String> tracks) {
    }
}
