import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class App {

    private static final String BASE = "http://www.papercdcase.com/";
    private static final int MAX_TRACKS = 18;

    public static void main(String[] args) throws Exception {
        Path root = Paths.get("").toAbsolutePath();
        Path dataFile = root.resolve("data").resolve("data.txt");
        Path resultDir = root.resolve("result");
        Path outPdf = resultDir.resolve("cd.pdf");

        Files.createDirectories(resultDir);

        CoverData data = readCoverData(dataFile);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");

        WebDriver driver = new ChromeDriver(options);
        try {
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(120));
            driver.get(BASE);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            wait.until(d -> d.findElement(By.name("artist")).isDisplayed());

            driver.findElement(By.name("artist")).clear();
            driver.findElement(By.name("artist")).sendKeys(data.artist);

            driver.findElement(By.name("title")).clear();
            driver.findElement(By.name("title")).sendKeys(data.title);

            for (int i = 0; i < data.tracks.size(); i++) {
                String name = "track" + (i + 1);
                var fields = driver.findElements(By.name(name));
                if (fields.isEmpty()) {
                    break;
                }
                WebElement field = fields.get(0);
                field.clear();
                field.sendKeys(data.tracks.get(i));
            }

            driver.findElement(By.cssSelector("input[name='template'][value='jewel']")).click();
            driver.findElement(By.cssSelector("input[name='size'][value='a4']")).click();

            WebElement btn = driver.findElement(By.name("submit"));
            btn.submit();

            WebDriverWait pdfWait = new WebDriverWait(driver, Duration.ofSeconds(90));
            pdfWait.until(
                    d -> {
                        String u = d.getCurrentUrl();
                        return u.contains("papercdcase") && u.toLowerCase().contains("pdf");
                    });

            String pdfUrl = driver.getCurrentUrl();
            savePdfFromUrl(pdfUrl, outPdf);
        } finally {
            driver.quit();
        }
    }

    private static void savePdfFromUrl(String pdfUrl, Path outPdf) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(pdfUrl))
                .timeout(Duration.ofSeconds(120))
                .header("User-Agent", "Mozilla/5.0 (compatible; ST-8 Selenium lab)")
                .GET()
                .build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode() + " for PDF URL");
        }
        Files.write(outPdf, response.body());
    }

    private static CoverData readCoverData(Path dataFile) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String line : Files.readAllLines(dataFile, StandardCharsets.UTF_8)) {
            String t = line.trim();
            if (!t.isEmpty()) {
                lines.add(t);
            }
        }
        if (lines.size() < 2) {
            throw new IOException("data.txt: need artist (line 1), title (line 2), then tracks");
        }
        String artist = lines.get(0);
        String title = lines.get(1);
        List<String> tracks = new ArrayList<>();
        for (int i = 2; i < lines.size(); i++) {
            if (tracks.size() >= MAX_TRACKS) {
                break;
            }
            tracks.add(lines.get(i));
        }
        return new CoverData(artist, title, tracks);
    }

    private static final class CoverData {
        final String artist;
        final String title;
        final List<String> tracks;

        CoverData(String artist, String title, List<String> tracks) {
            this.artist = artist;
            this.title = title;
            this.tracks = tracks;
        }
    }
}
