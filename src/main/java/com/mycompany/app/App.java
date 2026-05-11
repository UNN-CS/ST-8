package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Selenium script for http://www.papercdcase.com/ (basic form).
 * Data: {@code data/data.txt} - line 1 artist, line 2 title, following lines = track names
 * (up to 16 fields on the basic page).
 */
public class App {

    private static final String BASE_URL = "http://www.papercdcase.com/";
    private static final int MAX_TRACKS_ON_BASIC_FORM = 16;

    public static void main(String[] args) throws Exception {
        String driverPath = System.getenv("CHROME_DRIVER_PATH");
        if (driverPath != null && !driverPath.isEmpty()) {
            System.setProperty("webdriver.chrome.driver", driverPath);
        }

        Path projectRoot = Path.of("").toAbsolutePath();
        Path dataFile = projectRoot.resolve("data").resolve("data.txt");
        Path resultDir = projectRoot.resolve("result").toAbsolutePath().normalize();
        Files.createDirectories(resultDir);

        CdCaseData data = CdCaseData.load(dataFile);

        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", resultDir.toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);
        if (Boolean.parseBoolean(System.getenv().getOrDefault("HEADLESS_CHROME", "false"))) {
            options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage",
                    "--disable-gpu", "--window-size=1280,1024");
        }
        options.addArguments("--ignore-certificate-errors");

        WebDriver driver = new ChromeDriver(options);
        try {
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(120));
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

            driver.get(BASE_URL);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(90));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.name("artist")));

            WebElement artistField = driver.findElement(By.name("artist"));
            WebElement titleField = driver.findElement(By.name("title"));
            artistField.clear();
            artistField.sendKeys(data.artist);
            titleField.clear();
            titleField.sendKeys(data.title);

            for (int i = 0; i < data.tracks.size() && i < MAX_TRACKS_ON_BASIC_FORM; i++) {
                WebElement track = driver.findElement(By.name("track" + (i + 1)));
                track.clear();
                track.sendKeys(data.tracks.get(i));
            }

            WebElement jewel = driver.findElement(By.cssSelector("input[name='template'][value='jewel']"));
            jewel.click();

            WebElement a4 = driver.findElement(By.cssSelector("input[name='size'][value='a4']"));
            a4.click();

            List<WebElement> forceSave = driver.findElements(By.name("force_saveas"));
            if (!forceSave.isEmpty() && !forceSave.get(0).isSelected()) {
                forceSave.get(0).click();
            }

            long before = System.currentTimeMillis();
            WebElement submitBtn = driver.findElement(By.cssSelector("input[name='submit']"));
            submitBtn.submit();

            Path pdf = waitForNewPdf(resultDir, before, 90);
            Path target = resultDir.resolve("cd.pdf");
            Files.move(pdf, target, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Saved: " + target.toAbsolutePath());
        } finally {
            driver.quit();
        }
    }

    static Path waitForNewPdf(Path dir, long afterMillis, int timeoutSec) throws InterruptedException, IOException {
        long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try (Stream<Path> stream = Files.list(dir)) {
                Path newest = stream
                        .filter(p -> {
                            String n = p.getFileName().toString();
                            return n.endsWith(".pdf") && !n.endsWith(".crdownload");
                        })
                        .filter(p -> {
                            try {
                                return Files.getLastModifiedTime(p).toMillis() >= afterMillis - 2000;
                            } catch (IOException e) {
                                return false;
                            }
                        })
                        .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                        .orElse(null);
                if (newest != null) {
                    return newest;
                }
            }
            Thread.sleep(400);
        }
        throw new IllegalStateException("No PDF appeared in " + dir.toAbsolutePath());
    }

    static final class CdCaseData {
        final String artist;
        final String title;
        final List<String> tracks;

        CdCaseData(String artist, String title, List<String> tracks) {
            this.artist = artist;
            this.title = title;
            this.tracks = tracks;
        }

        static CdCaseData load(Path path) throws IOException {
            List<String> lines = Files.readAllLines(path).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            if (lines.size() < 2) {
                throw new IllegalArgumentException("data.txt needs at least artist and title lines");
            }
            String artist = lines.get(0);
            String title = lines.get(1);
            List<String> tracks = new ArrayList<>();
            for (int i = 2; i < lines.size() && tracks.size() < MAX_TRACKS_ON_BASIC_FORM; i++) {
                tracks.add(lines.get(i));
            }
            return new CdCaseData(artist, title, tracks);
        }
    }
}
