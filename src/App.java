import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class App {

    private static final String SITE_URL = "http://www.papercdcase.com";
    private static final int TRACK_LIMIT = 17;

    public static void main(String[] args) throws IOException {
        Path root = Paths.get(".").toAbsolutePath().normalize();
        Path dataPath = root.resolve("data").resolve("data.txt");
        Path outputDir = root.resolve("result");
        Path targetPdf = outputDir.resolve("cd.pdf");

        Files.createDirectories(outputDir);
        Album album = loadAlbum(dataPath);

        ChromeOptions chromeOptions = buildChromeOptions(outputDir);
        WebDriver driver = new ChromeDriver(chromeOptions);

        try {
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(45));
            driver.get(SITE_URL);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.name("artist")));

            typeInto(driver, "artist", album.artist);
            typeInto(driver, "title", album.title);

            for (int index = 0; index < album.tracks.size(); index++) {
                typeInto(driver, "track" + (index + 1), album.tracks.get(index));
            }

            selectRadio(driver, "size", "a4");
            selectRadio(driver, "template", "jewel");

            WebElement submitButton = driver.findElement(By.name("submit"));
            submitButton.submit();

            Path downloaded = awaitPdf(outputDir, driver, Duration.ofSeconds(90));
            Files.move(downloaded, targetPdf, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Cover saved: " + targetPdf);
        } finally {
            driver.quit();
        }
    }

    private static Album loadAlbum(Path file) throws IOException {
        List<String> rows = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    rows.add(line);
                }
            }
        }

        if (rows.size() < 2) {
            throw new IllegalStateException("data.txt: need artist on line 1 and album title on line 2");
        }

        String artist = rows.get(0);
        String title = rows.get(1);
        int trackCount = Math.min(rows.size() - 2, TRACK_LIMIT);
        List<String> tracks = rows.subList(2, 2 + trackCount);

        return new Album(artist, title, tracks);
    }

    private static ChromeOptions buildChromeOptions(Path downloadFolder) {
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadFolder.toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);

        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--remote-allow-origins=*");
        return options;
    }

    private static void typeInto(WebDriver driver, String fieldName, String text) {
        WebElement field = driver.findElement(By.name(fieldName));
        field.clear();
        field.sendKeys(text);
    }

    private static void selectRadio(WebDriver driver, String groupName, String value) {
        String selector = "input[name='" + groupName + "'][value='" + value + "']";
        WebElement radio = driver.findElement(By.cssSelector(selector));
        if (!radio.isSelected()) {
            radio.click();
        }
    }

    private static Path awaitPdf(Path folder, WebDriver driver, Duration timeout) throws IOException {
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        wait.pollingEvery(Duration.ofMillis(750));

        return wait.until(webDriver -> {
            try (var stream = Files.list(folder)) {
                return stream
                        .filter(path -> {
                            String name = path.getFileName().toString();
                            return name.endsWith(".pdf") && !name.endsWith(".crdownload");
                        })
                        .filter(path -> {
                            try {
                                return Files.size(path) > 0;
                            } catch (IOException ex) {
                                return false;
                            }
                        })
                        .findFirst()
                        .orElse(null);
            } catch (IOException ex) {
                return null;
            }
        });
    }

    private static final class Album {
        private final String artist;
        private final String title;
        private final List<String> tracks;

        private Album(String artist, String title, List<String> tracks) {
            this.artist = artist;
            this.title = title;
            this.tracks = tracks;
        }
    }
}
