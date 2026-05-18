import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {

    private static final String BASE_URL = "http://www.papercdcase.com/";
    private static final int MAX_TRACKS = 16;
    private static final int XPATH_COUNT = 23;
    private static final int IDX_FIRST_TRACK = 2;
    private static final int IDX_TYPE_CELL = 18;
    private static final int IDX_PAPER_CELL = 19;
    private static final int IDX_FONT_CELL = 20;
    private static final int IDX_FORCE_CELL = 21;
    private static final int IDX_SUBMIT = 22;

    public static void main(String[] args) throws Exception {
        Path projectRoot = Path.of(System.getProperty("user.dir"));
        Path dataFile = projectRoot.resolve("data").resolve("data.txt");
        Path xpathFile = projectRoot.resolve("data").resolve("xpaths.txt");
        Path resultDir = projectRoot.resolve("result");
        Files.createDirectories(resultDir);
        removeOldDownloads(resultDir);

        List<String> xpaths = readXpaths(xpathFile);
        FormData formData = readFormData(Files.readAllLines(dataFile, StandardCharsets.UTF_8));

        Path downloadDir = resultDir.toAbsolutePath().normalize();
        WebDriver webDriver = createDriver(downloadDir);
        try {
            webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
            webDriver.get(BASE_URL);

            webDriver.findElement(By.xpath(xpaths.get(0))).sendKeys(formData.artist);
            webDriver.findElement(By.xpath(xpaths.get(1))).sendKeys(formData.title);
            for (int i = 0; i < formData.tracks.size(); i++) {
                webDriver.findElement(By.xpath(xpaths.get(IDX_FIRST_TRACK + i)))
                        .sendKeys(formData.tracks.get(i));
            }

            selectRadio(webDriver, xpaths.get(IDX_TYPE_CELL),
                    ".//input[@name='template' and @value='jewel']");
            selectRadio(webDriver, xpaths.get(IDX_PAPER_CELL),
                    ".//input[@name='size' and @value='a4']");
            selectRadio(webDriver, xpaths.get(IDX_FONT_CELL),
                    ".//input[@name='lang' and @value='west']");
            selectRadio(webDriver, xpaths.get(IDX_FORCE_CELL),
                    ".//input[@name='force_saveas' and @value='yes']");

            WebElement btn = webDriver.findElement(By.xpath(xpaths.get(IDX_SUBMIT)));
            btn.submit();

            Path downloaded = waitForStablePdf(downloadDir);
            Path target = downloadDir.resolve("cd.pdf");
            Files.deleteIfExists(target);
            Files.move(downloaded, target);
            System.out.println("Saved: " + target);
        } finally {
            webDriver.quit();
        }
    }

    private static WebDriver createDriver(Path downloadDir) {
        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadDir.toString());
        prefs.put("plugins.always_open_pdf_externally", true);
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--disable-popup-blocking");
        return new ChromeDriver(options);
    }

    private static void selectRadio(WebDriver driver, String cellXpath, String inputXpath) {
        WebElement cell = driver.findElement(By.xpath(cellXpath));
        cell.findElement(By.xpath(inputXpath)).click();
    }

    private static List<String> readXpaths(Path file) throws IOException {
        List<String> xpaths = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String trimmed = line.strip();
            if (!trimmed.isEmpty()) {
                xpaths.add(trimmed);
            }
        }
        if (xpaths.size() != XPATH_COUNT) {
            throw new IllegalStateException(
                    "data/xpaths.txt: expected " + XPATH_COUNT + " lines, got " + xpaths.size());
        }
        return xpaths;
    }

    private static FormData readFormData(List<String> rawLines) {
        List<String> lines = new ArrayList<>();
        for (String line : rawLines) {
            String trimmed = line.stripTrailing();
            if (!trimmed.isBlank()) {
                lines.add(trimmed);
            }
        }
        if (lines.size() < 2) {
            throw new IllegalArgumentException("data/data.txt: need at least artist and title.");
        }
        String artist = lines.get(0);
        String title = lines.get(1);
        List<String> tracks = new ArrayList<>();
        for (int i = 2; i < lines.size() && tracks.size() < MAX_TRACKS; i++) {
            tracks.add(lines.get(i));
        }
        return new FormData(artist, title, tracks);
    }

    private static void removeOldDownloads(Path dir) throws IOException {
        try (var stream = Files.list(dir)) {
            for (Path path : stream.toList()) {
                String name = path.getFileName().toString().toLowerCase();
                if (name.endsWith(".pdf") || name.endsWith(".crdownload")) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static Path waitForStablePdf(Path dir) throws IOException, InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(120));
        while (Instant.now().isBefore(deadline)) {
            try (var stream = Files.list(dir)) {
                for (Path path : stream.toList()) {
                    String name = path.getFileName().toString().toLowerCase();
                    if (name.endsWith(".crdownload")) {
                        if (isStable(path) && Files.size(path) > 1000) {
                            Path finished = dir.resolve(name.replace(".crdownload", ""));
                            if (Files.exists(finished)) {
                                return finished;
                            }
                            return finalizeDownload(path, finished);
                        }
                        continue;
                    }
                    if (name.endsWith(".pdf") && isStable(path)) {
                        return path;
                    }
                }
            }
            Thread.sleep(300);
        }
        throw new IllegalStateException("PDF was not downloaded to " + dir);
    }

    private static boolean isStable(Path path) throws IOException, InterruptedException {
        long sizeBefore = Files.size(path);
        Thread.sleep(400);
        long sizeAfter = Files.size(path);
        return sizeBefore == sizeAfter && sizeBefore > 0;
    }

    private static Path finalizeDownload(Path partial, Path target) throws IOException {
        Files.copy(partial, target, StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(partial);
        return target;
    }

    private record FormData(String artist, String title, List<String> tracks) {}
}
