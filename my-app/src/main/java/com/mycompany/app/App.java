package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Fills http://www.papercdcase.com/ and saves the generated PDF to result/cd.pdf.
 */
public class App {
    private static final String SITE_URL = "http://www.papercdcase.com/";

    private static final int ARTIST_XPATH_INDEX = 0;
    private static final int TITLE_XPATH_INDEX = 1;
    private static final int FIRST_TRACK_XPATH_INDEX = 2;
    private static final int MAX_TRACKS = 16;
    private static final int FIRST_OPTION_XPATH_INDEX = 18;
    private static final int TYPE_JEWEL_XPATH_INDEX = 19;
    private static final int PAPER_A4_XPATH_INDEX = 21;
    private static final int LAST_OPTION_XPATH_INDEX = 21;
    private static final int SUBMIT_XPATH_INDEX = 22;

    private static final Duration PAGE_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(90);

    public static void main(String[] args) throws IOException {
        AppPaths paths = AppPaths.resolve();
        CdData cdData = CdDataReader.read(paths.tracksPath());
        XpathData xpaths = XpathDataReader.read(paths.xpathsPath());

        PdfGenerator generator = new PdfGenerator(paths);
        generator.generate(cdData, xpaths);
    }

    private record AppPaths(
            Path projectDirectory,
            Path tracksPath,
            Path xpathsPath,
            Path resultDirectory,
            Path resultPdf
    ) {
        private static AppPaths resolve() {
            Path projectDirectory = findProjectDirectory();
            Path tracksPath = projectDirectory.resolve("data").resolve("tracks.txt");
            Path xpathsPath = projectDirectory.resolve("data").resolve("xpaths.txt");
            Path resultDirectory = projectDirectory.resolve("result");
            Path resultPdf = resultDirectory.resolve("cd.pdf");

            return new AppPaths(projectDirectory, tracksPath, xpathsPath, resultDirectory, resultPdf);
        }

        private static Path findProjectDirectory() {
            Path currentDirectory = Paths.get("").toAbsolutePath();
            if (Files.exists(currentDirectory.resolve("data").resolve("tracks.txt"))) {
                return currentDirectory;
            }

            Path parentDirectory = currentDirectory.getParent();
            if (parentDirectory != null && Files.exists(parentDirectory.resolve("data").resolve("tracks.txt"))) {
                return parentDirectory;
            }

            throw new IllegalStateException("Cannot find data/tracks.txt from " + currentDirectory);
        }
    }

    private record CdData(String artist, String title, List<String> tracks) {
    }

    private static final class CdDataReader {
        private CdDataReader() {
        }

        private static CdData read(Path tracksPath) throws IOException {
            List<String> lines = Files.readAllLines(tracksPath, StandardCharsets.UTF_8);
            if (lines.size() < 2) {
                throw new IllegalArgumentException("tracks.txt must contain artist, title and optional tracks");
            }

            String artist = lines.get(0);
            String title = lines.get(1);
            List<String> tracks = new ArrayList<>(lines.subList(2, lines.size()));
            return new CdData(artist, title, tracks);
        }
    }

    private record XpathData(List<String> values) {
        private String artistInput() {
            return values.get(ARTIST_XPATH_INDEX);
        }

        private String titleInput() {
            return values.get(TITLE_XPATH_INDEX);
        }

        private String trackInput(int trackIndex) {
            return values.get(FIRST_TRACK_XPATH_INDEX + trackIndex);
        }

        private String jewelCaseOption() {
            return values.get(TYPE_JEWEL_XPATH_INDEX);
        }

        private String a4Option() {
            return values.get(PAPER_A4_XPATH_INDEX);
        }

        private String optionGroup(int optionIndex) {
            return values.get(optionIndex);
        }

        private String submitButton() {
            return values.get(SUBMIT_XPATH_INDEX);
        }
    }

    private static final class XpathDataReader {
        private XpathDataReader() {
        }

        private static XpathData read(Path xpathsPath) throws IOException {
            List<String> xpaths = Files.readAllLines(xpathsPath, StandardCharsets.UTF_8).stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .toList();

            if (xpaths.size() < SUBMIT_XPATH_INDEX + 1) {
                throw new IllegalArgumentException("xpaths.txt must contain at least 23 XPath lines");
            }

            return new XpathData(xpaths);
        }
    }

    private static final class PdfGenerator {
        private final AppPaths paths;

        private PdfGenerator(AppPaths paths) {
            this.paths = paths;
        }

        private void generate(CdData cdData, XpathData xpaths) throws IOException {
            prepareResultDirectory();

            WebDriver webDriver = BrowserFactory.create(paths.resultDirectory());
            try {
                WebDriverWait wait = new WebDriverWait(webDriver, PAGE_TIMEOUT);
                FormScenario formScenario = new FormScenario(webDriver, wait, xpaths);
                Set<String> filesBeforeSubmit = FileSystemSnapshot.currentFileNames(paths.resultDirectory());

                webDriver.get(SITE_URL);
                formScenario.fill(cdData);
                formScenario.submit();

                Path downloadedPdf = DownloadWaiter.waitForPdf(paths.resultDirectory(), filesBeforeSubmit);
                movePdfToResult(downloadedPdf);

                System.out.println("PDF saved to: " + paths.resultPdf());
            } finally {
                webDriver.quit();
            }
        }

        private void prepareResultDirectory() throws IOException {
            Files.createDirectories(paths.resultDirectory());
            Files.deleteIfExists(paths.resultPdf());
        }

        private void movePdfToResult(Path downloadedPdf) throws IOException {
            if (!downloadedPdf.equals(paths.resultPdf())) {
                Files.move(downloadedPdf, paths.resultPdf(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static final class BrowserFactory {
        private BrowserFactory() {
        }

        private static WebDriver create(Path downloadDirectory) {
            ChromeOptions options = new ChromeOptions();
            options.setAcceptInsecureCerts(true);
            options.addArguments("--ignore-certificate-errors");
            options.addArguments("--remote-allow-origins=*");
            options.setExperimentalOption("prefs", chromeDownloadPreferences(downloadDirectory));
            return new ChromeDriver(options);
        }

        private static Map<String, Object> chromeDownloadPreferences(Path downloadDirectory) {
            Map<String, Object> preferences = new HashMap<>();
            preferences.put("download.default_directory", downloadDirectory.toString());
            preferences.put("download.prompt_for_download", false);
            preferences.put("download.directory_upgrade", true);
            preferences.put("plugins.always_open_pdf_externally", true);
            preferences.put("profile.default_content_setting_values.automatic_downloads", 1);
            return preferences;
        }
    }

    private static final class FormScenario {
        private final WebDriver webDriver;
        private final WebDriverWait wait;
        private final XpathData xpaths;

        private FormScenario(WebDriver webDriver, WebDriverWait wait, XpathData xpaths) {
            this.webDriver = webDriver;
            this.wait = wait;
            this.xpaths = xpaths;
        }

        private void fill(CdData cdData) {
            fillTextInput(xpaths.artistInput(), cdData.artist());
            fillTextInput(xpaths.titleInput(), cdData.title());
            fillTracks(cdData.tracks());

            RadioSelector.select(webDriver, wait, xpaths, xpaths.jewelCaseOption(), "jewel", "jewel case", 1);
            RadioSelector.select(webDriver, wait, xpaths, xpaths.a4Option(), "a4", "a4", 1);
        }

        private void submit() {
            WebElement submitButton = wait.until(
                    ExpectedConditions.elementToBeClickable(By.xpath(xpaths.submitButton()))
            );
            submitButton.submit();
        }

        private void fillTextInput(String xpath, String value) {
            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));
            element.clear();
            element.sendKeys(value);
        }

        private void fillTracks(List<String> tracks) {
            int tracksToFill = Math.min(tracks.size(), MAX_TRACKS);
            for (int index = 0; index < tracksToFill; index++) {
                fillTextInput(xpaths.trackInput(index), tracks.get(index));
            }
        }
    }

    private static final class RadioSelector {
        private RadioSelector() {
        }

        private static void select(
                WebDriver webDriver,
                WebDriverWait wait,
                XpathData xpaths,
                String preferredXpath,
                String valueToken,
                String visibleTextToken,
                int fallbackIndex
        ) {
            WebElement preferredElement = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.xpath(preferredXpath))
            );
            if (clickMatchingRadio(webDriver, preferredElement, valueToken, visibleTextToken, fallbackIndex)) {
                return;
            }

            for (int index = FIRST_OPTION_XPATH_INDEX; index <= LAST_OPTION_XPATH_INDEX; index++) {
                WebElement optionElement = wait.until(
                        ExpectedConditions.presenceOfElementLocated(By.xpath(xpaths.optionGroup(index)))
                );
                if (clickMatchingRadio(webDriver, optionElement, valueToken, visibleTextToken, fallbackIndex)) {
                    return;
                }
            }

            throw new IllegalStateException("Cannot select radio option: " + visibleTextToken);
        }

        private static boolean clickMatchingRadio(
                WebDriver webDriver,
                WebElement element,
                String valueToken,
                String visibleTextToken,
                int fallbackIndex
        ) {
            List<WebElement> radios = collectRadios(element);
            for (WebElement radio : radios) {
                if (elementMatches(radio, valueToken, visibleTextToken)) {
                    clickIfNeeded(webDriver, radio);
                    return true;
                }
            }

            String elementText = normalize(element.getText());
            if (elementText.contains(normalize(visibleTextToken)) && radios.size() > fallbackIndex) {
                clickIfNeeded(webDriver, radios.get(fallbackIndex));
                return true;
            }

            return false;
        }

        private static List<WebElement> collectRadios(WebElement element) {
            List<WebElement> radios = new ArrayList<>();
            if ("input".equalsIgnoreCase(element.getTagName())) {
                radios.add(element);
            } else {
                radios.addAll(element.findElements(By.cssSelector("input[type='radio']")));
            }
            return radios;
        }

        private static boolean elementMatches(WebElement element, String valueToken, String visibleTextToken) {
            String needleByValue = normalize(valueToken);
            String needleByText = normalize(visibleTextToken);
            return normalize(element.getAttribute("value")).contains(needleByValue)
                    || normalize(element.getAttribute("id")).contains(needleByValue)
                    || normalize(element.getAttribute("name")).contains(needleByValue)
                    || normalize(element.getAttribute("outerHTML")).contains(needleByText);
        }

        private static String normalize(String value) {
            return value == null ? "" : value.toLowerCase(Locale.ROOT);
        }

        private static void clickIfNeeded(WebDriver webDriver, WebElement element) {
            if (!element.isSelected()) {
                try {
                    element.click();
                } catch (RuntimeException exception) {
                    ((JavascriptExecutor) webDriver).executeScript("arguments[0].click();", element);
                }
            }
        }
    }

    private static final class DownloadWaiter {
        private DownloadWaiter() {
        }

        private static Path waitForPdf(Path resultDirectory, Set<String> filesBeforeSubmit) throws IOException {
            long deadline = System.nanoTime() + DOWNLOAD_TIMEOUT.toNanos();
            while (System.nanoTime() < deadline) {
                Path downloadedPdf = findDownloadedPdf(resultDirectory, filesBeforeSubmit);
                if (downloadedPdf != null && isDownloadComplete(resultDirectory, downloadedPdf)) {
                    return downloadedPdf;
                }

                sleepBriefly();
            }

            throw new IllegalStateException("PDF was not downloaded to " + resultDirectory);
        }

        private static Path findDownloadedPdf(Path resultDirectory, Set<String> filesBeforeSubmit) throws IOException {
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(resultDirectory, "*.pdf")) {
                for (Path path : directoryStream) {
                    if (!filesBeforeSubmit.contains(path.getFileName().toString())) {
                        return path;
                    }
                }
            }

            return null;
        }

        private static boolean isDownloadComplete(Path resultDirectory, Path pdfPath) throws IOException {
            if (hasTemporaryChromeDownload(resultDirectory)) {
                return false;
            }

            long firstSize = Files.size(pdfPath);
            sleepBriefly();
            return Files.exists(pdfPath) && Files.size(pdfPath) == firstSize && firstSize > 0;
        }

        private static boolean hasTemporaryChromeDownload(Path resultDirectory) throws IOException {
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(resultDirectory, "*.crdownload")) {
                return directoryStream.iterator().hasNext();
            }
        }

        private static void sleepBriefly() {
            try {
                Thread.sleep(500);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for PDF download", exception);
            }
        }
    }

    private static final class FileSystemSnapshot {
        private FileSystemSnapshot() {
        }

        private static Set<String> currentFileNames(Path directory) throws IOException {
            Set<String> names = new HashSet<>();
            if (!Files.exists(directory)) {
                return names;
            }

            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
                for (Path path : directoryStream) {
                    names.add(path.getFileName().toString());
                }
            }

            return names;
        }
    }
}
