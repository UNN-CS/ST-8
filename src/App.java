import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class App {
    private static final String PAGE_URL = "http://www.papercdcase.com/";
    private static final Path DATA_FILE = Paths.get("data", "data.txt");
    private static final Path RESULT_DIR = Paths.get("result");
    private static final Path RESULT_FILE = RESULT_DIR.resolve("cd.pdf");
    private static final int MAX_TRACKS = 18;

    public static void main(String[] args) throws Exception {
        CdData cdData = generateDataFile(DATA_FILE);
        Files.createDirectories(RESULT_DIR);
        Files.deleteIfExists(RESULT_FILE);

        WebDriver webDriver = createChromeDriver();
        try {
            webDriver.get(PAGE_URL);

            fillForm(webDriver, cdData);
            selectRadio(webDriver, "jewel");
            selectRadio(webDriver, "a4");
            enableForceSaveAs(webDriver);

            Set<Path> oldPdfFiles = listPdfFiles(RESULT_DIR);
            WebElement button = findElement(webDriver,
                    By.xpath("//form//input[@type='image']"),
                    By.xpath("//form//input[@type='submit']"),
                    By.xpath("//form//button[@type='submit']")
            );
            try {
                button.submit();
            } catch (TimeoutException ignored) {
                // On the legacy site Chrome can time out while waiting for the PDF
                // navigation to finish. The submit action has already been sent,
                // so keep the assignment flow and wait for the downloaded file.
            }

            Path generatedPdf = waitForPdf(RESULT_DIR, oldPdfFiles);
            Files.copy(generatedPdf, RESULT_FILE, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("PDF saved to " + RESULT_FILE.toAbsolutePath());
        } finally {
            webDriver.quit();
        }
    }

    private static CdData generateDataFile(Path file) throws IOException {
        CdData cdData = new CdData(
                "Daft Punk",
                "Discovery",
                Arrays.asList(
                        "One More Time",
                        "Aerodynamic",
                        "Digital Love",
                        "Harder Better Faster Stronger",
                        "Crescendolls",
                        "Nightvision",
                        "Superheroes",
                        "High Life",
                        "Something About Us",
                        "Voyager",
                        "Veridis Quo",
                        "Short Circuit",
                        "Face to Face",
                        "Too Long"
                )
        );

        Files.createDirectories(file.getParent());

        List<String> lines = new ArrayList<>();
        lines.add(cdData.artist);
        lines.add(cdData.title);
        lines.addAll(cdData.tracks);
        Files.write(file, lines, StandardCharsets.UTF_8);

        return CdData.read(file);
    }

    private static WebDriver createChromeDriver() {
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", RESULT_DIR.toAbsolutePath().toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("plugins.always_open_pdf_externally", true);

        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--ignore-certificate-errors");

        ChromeDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(15));
        return driver;
    }

    private static void fillForm(WebDriver webDriver, CdData cdData) {
        WebElement artistInput = findElement(webDriver,
                By.xpath("//input[@name='artist']"),
                By.xpath("//*[normalize-space()='Artist']/following::input[1]")
        );
        artistInput.clear();
        artistInput.sendKeys(cdData.artist);

        WebElement titleInput = findElement(webDriver,
                By.xpath("//input[@name='title']"),
                By.xpath("//*[normalize-space()='Title']/following::input[1]")
        );
        titleInput.clear();
        titleInput.sendKeys(cdData.title);

        for (int i = 0; i < cdData.tracks.size(); i++) {
            WebElement trackInput = findTrackInput(webDriver, i + 1);
            trackInput.clear();
            trackInput.sendKeys(cdData.tracks.get(i));
        }
    }

    private static WebElement findTrackInput(WebDriver webDriver, int number) {
        return findElement(webDriver,
                By.xpath("//input[@name='track" + number + "']"),
                By.xpath("//input[@id='track" + number + "']"),
                By.xpath("(//*[normalize-space()='Tracks']/following::input[@type='text' or not(@type)])[" + number + "]"),
                By.xpath("(//*[contains(normalize-space(), 'Tracks')]/following::input[@type='text' or not(@type)])[" + number + "]")
        );
    }

    private static void selectRadio(WebDriver webDriver, String valuePart) {
        String value = valuePart.toLowerCase(Locale.ROOT);
        WebElement radio = findElement(webDriver,
                By.xpath("//input[@type='radio' and contains(translate(@value, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + value + "')]"),
                By.xpath("//label[contains(translate(normalize-space(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + value + "')]//input[@type='radio']"),
                By.xpath("//label[contains(translate(normalize-space(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + value + "')]/preceding::input[@type='radio'][1]")
        );
        if (!radio.isSelected()) {
            radio.click();
        }
    }

    private static void enableForceSaveAs(WebDriver webDriver) {
        try {
            WebElement checkbox = findElement(webDriver,
                    By.xpath("//input[@type='checkbox' and contains(translate(@name, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'save')]"),
                    By.xpath("//input[@type='checkbox' and contains(translate(@value, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'save')]"),
                    By.xpath("//*[contains(normalize-space(), 'Force Save-as')]/following::input[@type='checkbox'][1]"),
                    By.xpath("//*[contains(normalize-space(), 'Force file to be saved')]/preceding::input[@type='checkbox'][1]")
            );
            if (!checkbox.isSelected()) {
                checkbox.click();
            }
        } catch (NoSuchElementException ignored) {
            // The assignment requires saving result/cd.pdf; if the checkbox is not found,
            // the browser download preferences still try to save the generated PDF.
        }
    }

    private static WebElement findElement(WebDriver webDriver, By... locators) {
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(20));
        NoSuchElementException lastFailure = null;

        for (By locator : locators) {
            try {
                return wait.until(ExpectedConditions.elementToBeClickable(locator));
            } catch (NoSuchElementException failure) {
                lastFailure = failure;
            } catch (TimeoutException ignored) {
                // Try the next locator.
            }
        }

        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new NoSuchElementException("Element was not found");
    }

    private static Set<Path> listPdfFiles(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return new HashSet<>();
        }
        try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                    .collect(Collectors.toSet());
        }
    }

    private static Path waitForPdf(Path dir, Set<Path> oldPdfFiles) throws IOException, InterruptedException {
        long deadline = System.currentTimeMillis() + 60_000L;
        while (System.currentTimeMillis() < deadline) {
            Set<Path> currentPdfFiles = listPdfFiles(dir);
            currentPdfFiles.removeAll(oldPdfFiles);
            for (Path file : currentPdfFiles) {
                if (Files.exists(file) && Files.size(file) > 0 && !hasTemporaryDownload(file)) {
                    return file;
                }
            }
            Thread.sleep(500L);
        }
        throw new IOException("PDF file was not downloaded to " + dir.toAbsolutePath());
    }

    private static boolean hasTemporaryDownload(Path pdfFile) {
        String fileName = pdfFile.getFileName().toString();
        Path crdownload = pdfFile.resolveSibling(fileName + ".crdownload");
        Path tmp = pdfFile.resolveSibling(fileName + ".tmp");
        return Files.exists(crdownload) || Files.exists(tmp);
    }

    private static final class CdData {
        private final String artist;
        private final String title;
        private final List<String> tracks;

        private CdData(String artist, String title, List<String> tracks) {
            this.artist = artist;
            this.title = title;
            this.tracks = tracks;
        }

        private static CdData read(Path file) throws IOException {
            List<String> lines = Files.readAllLines(file).stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList());

            if (lines.size() < 3) {
                throw new IOException("Expected artist, title and at least one track in " + file);
            }

            List<String> tracks = lines.subList(2, Math.min(lines.size(), 2 + MAX_TRACKS));
            return new CdData(lines.get(0), lines.get(1), new ArrayList<>(tracks));
        }
    }
}
