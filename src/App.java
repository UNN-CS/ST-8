import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class App {
    private static final String BASE_URL = "http://www.papercdcase.com/";
    private static final Path DATA_FILE = Path.of("data", "data.txt");
    private static final Path RESULT_DIR = Path.of("result").toAbsolutePath();
    private static final Path RESULT_FILE = RESULT_DIR.resolve("cd.pdf");

    public static void main(String[] args) throws Exception {
        Files.createDirectories(RESULT_DIR);
        Files.deleteIfExists(RESULT_FILE);

        AlbumData albumData = AlbumData.read(DATA_FILE);

        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--window-size=1200,900");
        if (Boolean.parseBoolean(System.getProperty("headless", "true"))) {
            options.addArguments("--headless=new");
        }

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", RESULT_DIR.toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);

        WebDriver webDriver = new ChromeDriver(options);
        try {
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(30));
            openPage(webDriver, wait);

            textField(webDriver, 1, "artist").sendKeys(albumData.artist());
            textField(webDriver, 2, "title").sendKeys(albumData.title());

            List<String> tracks = albumData.tracks();
            for (int i = 0; i < Math.min(tracks.size(), 16); i++) {
                textField(webDriver, i + 3, "track" + (i + 1), "track" + i).sendKeys(tracks.get(i));
            }

            radio(webDriver, "template", "Jewel case", "jewel").click();
            radio(webDriver, "size", "A4", "a4").click();

            WebElement submit = firstExisting(webDriver,
                    By.xpath("//input[@type='submit']"),
                    By.xpath("//input[@type='image']"),
                    By.xpath("//button[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'create')]"),
                    By.xpath("//*[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'create cd case')]"));
            submit.submit();

            waitForPdfDownload();
            System.out.println("PDF saved to " + RESULT_FILE);
        } finally {
            webDriver.quit();
        }
    }

    private static void openPage(WebDriver webDriver, WebDriverWait wait) {
        webDriver.get(BASE_URL);
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input")));
        } catch (RuntimeException firstFailure) {
            webDriver.get("https://www.papercdcase.com/");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input")));
        }
    }

    private static WebElement textField(WebDriver webDriver, int position, String... names) {
        List<By> selectors = new ArrayList<>();
        for (String name : names) {
            selectors.add(By.name(name));
        }
        selectors.add(By.xpath("(//input[not(@type) or @type='text'])[" + position + "]"));
        return firstExisting(webDriver, selectors.toArray(By[]::new));
    }

    private static WebElement radio(WebDriver webDriver, String groupName, String labelText, String... values) {
        List<By> selectors = new ArrayList<>();
        for (String value : values) {
            selectors.add(By.cssSelector("input[type='radio'][name='" + groupName + "'][value='" + value + "']"));
        }
        selectors.add(By.xpath("//input[@type='radio' and @name='" + groupName + "'][following-sibling::text()[contains(., '" + labelText + "')]]"));
        selectors.add(By.xpath("//label[contains(normalize-space(.), '" + labelText + "')]//input[@type='radio']"));
        return firstExisting(webDriver, selectors.toArray(By[]::new));
    }

    private static WebElement firstExisting(WebDriver webDriver, By... selectors) {
        for (By selector : selectors) {
            List<WebElement> elements = webDriver.findElements(selector);
            for (WebElement element : elements) {
                if (element.isDisplayed() || element.getAttribute("type") != null) {
                    return element;
                }
            }
        }
        throw new IllegalStateException("Element not found. Tried: " + List.of(selectors));
    }

    private static void waitForPdfDownload() throws InterruptedException, IOException {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(60).toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (Files.exists(RESULT_FILE) && Files.size(RESULT_FILE) > 0) {
                return;
            }

            List<Path> pdfs;
            try (var files = Files.list(RESULT_DIR)) {
                pdfs = files
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                        .collect(Collectors.toList());
            }
            if (!pdfs.isEmpty()) {
                Files.move(pdfs.get(0), RESULT_FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return;
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException("PDF was not downloaded to " + RESULT_DIR);
    }

    private record AlbumData(String artist, String title, List<String> tracks) {
        private static AlbumData read(Path file) throws IOException {
            List<String> lines = Files.readAllLines(file).stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .toList();
            if (lines.size() < 3) {
                throw new IllegalArgumentException("data.txt must contain artist, title and at least one track");
            }
            return new AlbumData(lines.get(0), lines.get(1), lines.subList(2, Math.min(lines.size(), 18)));
        }
    }
}
