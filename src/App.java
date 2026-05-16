import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class App {
    private static final String FORM_URL = "http://www.papercdcase.com/";
    private static final Path DATA_PATH = Path.of("data", "data.txt");
    private static final Path RESULT_PATH = Path.of("result", "cd.pdf");
    private static final int MAX_SITE_TRACKS = 16;

    public static void main(String[] args) throws Exception {
        CoverData coverData = readCoverData(DATA_PATH);

        ChromeOptions options = new ChromeOptions();
        // Keep browser visible for easier debugging.
        options.addArguments("--start-maximized");

        WebDriver driver = new ChromeDriver(options);
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(40));
            driver.get(FORM_URL);

            WebElement artistField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("artist")));
            WebElement titleField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("title")));
            WebElement jewelCaseRadio = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("input[name='template'][value='jewel']")));
            WebElement a4Radio = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("input[name='size'][value='a4']")));
            WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("input[type='image']")));

            artistField.clear();
            artistField.sendKeys(coverData.artist());
            titleField.clear();
            titleField.sendKeys(coverData.title());
            fillTrackFields(driver, coverData.tracks());

            if (!jewelCaseRadio.isSelected()) {
                jewelCaseRadio.click();
            }
            if (!a4Radio.isSelected()) {
                a4Radio.click();
            }

            submitButton.submit();
            wait.until(ExpectedConditions.urlContains("papercdcase.cgi/papercdcase.pdf"));
            savePdf(driver.getCurrentUrl(), RESULT_PATH);
            System.out.println("PDF saved to: " + RESULT_PATH.toAbsolutePath());
        } finally {
            driver.quit();
        }
    }

    private static void savePdf(String pdfUrl, Path targetPath) throws IOException, InterruptedException {
        Files.createDirectories(targetPath.getParent());

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(pdfUrl))
                .GET()
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 400) {
            throw new IOException("Failed to download PDF, HTTP status: " + response.statusCode());
        }

        Files.write(targetPath, response.body());
    }

    private static void fillTrackFields(WebDriver driver, List<String> tracks) {
        int limit = Math.min(tracks.size(), MAX_SITE_TRACKS);
        for (int i = 0; i < limit; i++) {
            WebElement trackField = driver.findElement(By.name("track" + (i + 1)));
            trackField.clear();
            trackField.sendKeys(tracks.get(i));
        }
    }

    private static CoverData readCoverData(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        String artist = "";
        String title = "";
        List<String> tracks = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("artist=")) {
                artist = line.substring("artist=".length()).trim();
            } else if (line.startsWith("title=")) {
                title = line.substring("title=".length()).trim();
            } else if (line.startsWith("track=")) {
                String track = line.substring("track=".length()).trim();
                if (!track.isEmpty()) {
                    tracks.add(track);
                }
            }
        }

        if (artist.isEmpty() || title.isEmpty() || tracks.isEmpty()) {
            throw new IllegalArgumentException("data.txt must include artist=, title= and at least one track=");
        }
        if (tracks.size() > 18) {
            throw new IllegalArgumentException("data.txt contains more than 18 tracks (task requirement)");
        }

        return new CoverData(artist, title, tracks);
    }

    private record CoverData(String artist, String title, List<String> tracks) {
    }
}
