import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {

    private static final String URL = "http://www.papercdcase.com/";
    private static final int MAX_TRACKS = 16;
    private static final Path PROJECT_DIR = Paths.get("").toAbsolutePath();
    private static final Path DATA_FILE = PROJECT_DIR.resolve("data").resolve("data.txt");
    private static final Path RESULT_DIR = PROJECT_DIR.resolve("result");
    private static final Path RESULT_PDF = RESULT_DIR.resolve("cd.pdf");

    public static void main(String[] args) throws IOException, InterruptedException {
        Files.createDirectories(RESULT_DIR);
        Files.deleteIfExists(RESULT_PDF);

        List<String> lines = readDataFile();
        if (lines.size() < 2) {
            throw new IllegalStateException("data.txt must contain at least 2 lines (artist and title)");
        }
        String artist = lines.get(0);
        String title = lines.get(1);
        List<String> tracks = lines.subList(2, Math.min(lines.size(), 2 + MAX_TRACKS));

        WebDriver driver = createDriver();
        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            driver.get(URL);

            WebElement artistField = driver.findElement(By.xpath("//input[@name='artist']"));
            artistField.sendKeys(artist);

            WebElement titleField = driver.findElement(By.xpath("//input[@name='title']"));
            titleField.sendKeys(title);

            for (int i = 0; i < tracks.size(); i++) {
                String trackName = "track" + (i + 1);
                WebElement trackField = driver.findElement(By.xpath("//input[@name='" + trackName + "']"));
                trackField.sendKeys(tracks.get(i));
            }

            WebElement jewelRadio = driver.findElement(
                    By.xpath("//input[@name='template' and @value='jewel']"));
            jewelRadio.click();

            WebElement a4Radio = driver.findElement(
                    By.xpath("//input[@name='size' and @value='a4']"));
            a4Radio.click();

            WebElement forceSaveAs = driver.findElement(
                    By.xpath("//input[@name='force_saveas' and @value='yes']"));
            if (!forceSaveAs.isSelected()) {
                forceSaveAs.click();
            }

            String pdfUrl = buildPdfUrl(driver);
            downloadPdf(pdfUrl, RESULT_PDF);

            System.out.println("PDF saved to: " + RESULT_PDF);
        } finally {
            driver.quit();
        }
    }

    private static WebDriver createDriver() {
        ChromeOptions options = new ChromeOptions();

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", RESULT_DIR.toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);

        options.addArguments("--disable-features=DownloadBubble,DownloadBubbleV2");
        options.addArguments("--headless=new");

        return new ChromeDriver(options);
    }

    private static List<String> readDataFile() throws IOException {
        List<String> result = new ArrayList<>();
        for (String line : Files.readAllLines(DATA_FILE)) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private static String buildPdfUrl(WebDriver driver) {
        Object result = ((JavascriptExecutor) driver).executeScript(
                "const form = document.querySelector(\"form[action*='papercdcase.cgi/papercdcase.pdf']\");" +
                        "if (!form) { return null; }" +
                        "const params = new URLSearchParams(new FormData(form));" +
                        "return form.action + '?' + params.toString();"
        );
        if (result == null) {
            throw new IllegalStateException("Cannot build PDF URL from form");
        }
        return result.toString();
    }

    private static void downloadPdf(String pdfUrl, Path targetFile) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(pdfUrl))
                .GET()
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to download PDF, HTTP status: " + response.statusCode());
        }
        if (response.body().length == 0) {
            throw new IOException("Downloaded PDF is empty");
        }
        Files.write(targetFile, response.body());
    }
}
