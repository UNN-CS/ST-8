import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class App {
    private static final List<String> BASE_URLS = Arrays.asList(
            "http://www.papercdcase.com/",
            "https://www.papercdcase.com/"
    );
    private static final Path DATA_FILE = Paths.get("data", "data.txt");
    private static final Path RESULT_DIR = Paths.get("result");
    private static final Path RESULT_FILE = RESULT_DIR.resolve("cd.pdf");
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(45);
    private static final int MAX_TRACKS = 18;

    public static void main(String[] args) throws IOException {
        CdData cdData = CdData.fromFile(DATA_FILE);
        Files.createDirectories(RESULT_DIR);
        Files.deleteIfExists(RESULT_FILE);

        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);
        options.setPageLoadStrategy(PageLoadStrategy.NONE);
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--disable-popup-blocking");

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", RESULT_DIR.toAbsolutePath().toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);

        WebDriver webDriver = new ChromeDriver(options);
        try {
            WebDriverWait wait = new WebDriverWait(webDriver, WAIT_TIMEOUT);
            webDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(10));
            openAvailablePage(webDriver, wait);

            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input, textarea")));
            typeIntoField(webDriver, "artist", cdData.getArtist());
            typeIntoField(webDriver, "title", cdData.getTitle());
            fillTracks(webDriver, cdData.getTracks());
            selectRadio(webDriver, Arrays.asList(
                    By.cssSelector("input[name='type'][value='jewel']"),
                    By.xpath("//input[@type='radio' and contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'jewel')]"),
                    By.xpath("//td[contains(normalize-space(.),'Jewel case')]//input[@type='radio']")
            ));
            selectRadio(webDriver, Arrays.asList(
                    By.cssSelector("input[name='paper'][value='a4']"),
                    By.xpath("//input[@type='radio' and translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='a4']"),
                    By.xpath("//td[contains(normalize-space(.),'A4')]//input[@type='radio']")
            ));
            clickForceSaveAsIfPresent(webDriver);

            submitForm(webDriver);

            Path downloadedPdf = waitForDownloadedPdf(wait);
            Files.move(downloadedPdf, RESULT_FILE);
            System.out.println("PDF saved to " + RESULT_FILE.toAbsolutePath());
        } finally {
            webDriver.quit();
        }
    }

    private static void openAvailablePage(WebDriver webDriver, WebDriverWait wait) {
        RuntimeException lastException = null;
        for (String url : BASE_URLS) {
            System.out.println("Opening " + url);
            try {
                webDriver.get(url);
            } catch (WebDriverException exception) {
                lastException = exception;
                stopLoading(webDriver);
                System.out.println("Failed to open " + url + ": " + exception.getMessage());
            }

            if (hasFormFields(webDriver, wait)) {
                return;
            }
        }

        throw new IllegalStateException("Unable to open papercdcase.com with HTTP or HTTPS", lastException);
    }

    private static boolean hasFormFields(WebDriver webDriver, WebDriverWait wait) {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input, textarea")));
            return true;
        } catch (WebDriverException exception) {
            return false;
        }
    }

    private static void stopLoading(WebDriver webDriver) {
        if (webDriver instanceof JavascriptExecutor) {
            try {
                ((JavascriptExecutor) webDriver).executeScript("window.stop();");
            } catch (WebDriverException ignored) {
                // The browser can already be on an error page.
            }
        }
    }

    private static void typeIntoField(WebDriver webDriver, String fieldName, String value) {
        WebElement element = findFirst(webDriver, Arrays.asList(
                By.name(fieldName),
                By.id(fieldName),
                By.cssSelector("input[name='" + fieldName + "']"),
                By.xpath("//td[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'"
                        + fieldName + "')]/following::input[1]")
        ));
        element.clear();
        element.sendKeys(value);
    }

    private static void fillTracks(WebDriver webDriver, List<String> tracks) {
        List<WebElement> trackInputs = findTrackInputs(webDriver);
        if (trackInputs.size() >= tracks.size()) {
            for (int index = 0; index < tracks.size(); index++) {
                WebElement input = trackInputs.get(index);
                input.clear();
                input.sendKeys(tracks.get(index));
            }
            return;
        }

        WebElement tracksArea = findFirst(webDriver, Arrays.asList(
                By.name("tracks"),
                By.id("tracks"),
                By.cssSelector("textarea")
        ));
        tracksArea.clear();
        tracksArea.sendKeys(String.join(System.lineSeparator(), tracks));
    }

    private static List<WebElement> findTrackInputs(WebDriver webDriver) {
        List<WebElement> inputs = new ArrayList<>();
        for (int number = 1; number <= MAX_TRACKS; number++) {
            Optional<WebElement> input = findOptional(webDriver, Arrays.asList(
                    By.name("track" + number),
                    By.id("track" + number),
                    By.cssSelector("input[name='track" + number + "']"),
                    By.xpath("//td[normalize-space()='" + number + "']/following::input[1]")
            ));
            input.ifPresent(inputs::add);
        }
        return inputs;
    }

    private static void selectRadio(WebDriver webDriver, List<By> selectors) {
        WebElement radio = findFirst(webDriver, selectors);
        if (!radio.isSelected()) {
            radio.click();
        }
    }

    private static void clickForceSaveAsIfPresent(WebDriver webDriver) {
        findOptional(webDriver, Arrays.asList(
                By.cssSelector("input[type='checkbox'][name*='force']"),
                By.xpath("//input[@type='checkbox' and contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'force')]"),
                By.xpath("//td[contains(normalize-space(.),'Force Save-as')]//input[@type='checkbox']")
        )).filter(checkbox -> !checkbox.isSelected()).ifPresent(WebElement::click);
    }

    private static void submitForm(WebDriver webDriver) {
        Optional<WebElement> button = findOptional(webDriver, Arrays.asList(
                By.cssSelector("input[type='submit']"),
                By.cssSelector("input[type='image']"),
                By.cssSelector("button[type='submit']"),
                By.xpath("//input[@type='button' or @type='submit' or @type='image']"),
                By.xpath("//button")
        ));

        if (button.isPresent()) {
            button.get().submit();
            return;
        }

        WebElement field = findFirst(webDriver, Arrays.asList(
                By.name("artist"),
                By.name("title"),
                By.cssSelector("input, textarea")
        ));
        field.submit();
    }

    private static WebElement findFirst(WebDriver webDriver, List<By> selectors) {
        return findOptional(webDriver, selectors)
                .orElseThrow(() -> new IllegalStateException("Unable to find element by selectors: " + selectors));
    }

    private static Optional<WebElement> findOptional(WebDriver webDriver, List<By> selectors) {
        for (By selector : selectors) {
            List<WebElement> elements = webDriver.findElements(selector);
            if (!elements.isEmpty()) {
                return Optional.of(elements.get(0));
            }
        }
        return Optional.empty();
    }

    private static Path waitForDownloadedPdf(WebDriverWait wait) {
        ExpectedCondition<Path> pdfIsDownloaded = driver -> {
            if (driver instanceof JavascriptExecutor) {
                ((JavascriptExecutor) driver).executeScript("return document.readyState");
            }

            try (Stream<Path> files = Files.list(RESULT_DIR)) {
                return files
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                        .filter(path -> !path.getFileName().toString().endsWith(".crdownload"))
                        .max(Comparator.comparing(path -> path.toFile().lastModified()))
                        .orElse(null);
            } catch (IOException exception) {
                return null;
            }
        };

        Path downloadedPdf = wait.until(pdfIsDownloaded);
        if (downloadedPdf == null) {
            throw new IllegalStateException("PDF file was not downloaded");
        }
        return downloadedPdf;
    }

    private static class CdData {
        private final String artist;
        private final String title;
        private final List<String> tracks;

        private CdData(String artist, String title, List<String> tracks) {
            this.artist = artist;
            this.title = title;
            this.tracks = tracks;
        }

        private String getArtist() {
            return artist;
        }

        private String getTitle() {
            return title;
        }

        private List<String> getTracks() {
            return tracks;
        }

        private static CdData fromFile(Path path) throws IOException {
            List<String> lines = Files.readAllLines(path);
            String artist = "";
            String title = "";
            List<String> tracks = new ArrayList<>();
            boolean readingTracks = false;

            for (String line : lines) {
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) {
                    continue;
                }

                String lowerLine = trimmedLine.toLowerCase(Locale.ROOT);
                if (lowerLine.startsWith("artist:")) {
                    artist = trimmedLine.substring("artist:".length()).trim();
                    readingTracks = false;
                } else if (lowerLine.startsWith("title:")) {
                    title = trimmedLine.substring("title:".length()).trim();
                    readingTracks = false;
                } else if (lowerLine.startsWith("tracks:")) {
                    readingTracks = true;
                } else if (readingTracks) {
                    tracks.add(trimmedLine.replaceFirst("^\\d+[.)]\\s*", ""));
                }
            }

            if (artist.trim().isEmpty() || title.trim().isEmpty() || tracks.isEmpty()) {
                throw new IllegalArgumentException("data.txt must contain Artist, Title and Tracks sections");
            }
            if (tracks.size() > MAX_TRACKS) {
                throw new IllegalArgumentException("The track list must contain no more than " + MAX_TRACKS + " tracks");
            }

            return new CdData(artist, title, new ArrayList<>(tracks));
        }
    }
}
