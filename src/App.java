import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openqa.selenium.By;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Selenium automation for papercdcase.com basic form.
 *
 * <p>Example XPath locators (for manual copy in the browser XPath extension):
 * <ul>
 *   <li>Artist — {@code //input[@name='artist']}</li>
 *   <li>Title — {@code //input[@name='title']}</li>
 *   <li>All track fields — {@code //input[starts-with(@name,'track')]}</li>
 *   <li>Type “Jewel case” — {@code //input[@name='template' and @value='jewel']}</li>
 *   <li>Paper A4 — {@code //input[@name='size' and @value='a4']}</li>
 *   <li>Create button (image submit) — {@code //input[@name='submit' and @type='image']}</li>
 * </ul>
 */
public final class App {

    private static final String BASE_URL = "http://www.papercdcase.com/";
    private static final int MAX_TRACK_FIELDS = 16;

    public static void main(String[] args) throws Exception {
        Path projectRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        Path dataFile = projectRoot.resolve("data").resolve("data.txt");
        Path resultDir = projectRoot.resolve("result");
        Files.createDirectories(resultDir);

        CoverData cover = CoverData.load(dataFile);

        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", resultDir.toAbsolutePath().toString().replace('\\', '/'));
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--ignore-certificate-errors");
        options.setAcceptInsecureCerts(true);
        options.setPageLoadStrategy(PageLoadStrategy.NONE);

        Path driverPath = resolveChromeDriver();
        System.setProperty("webdriver.chrome.driver", driverPath.toString());

        WebDriver driver = new ChromeDriver(options);
        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(120));
            driver.get(BASE_URL);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            wait.until(d -> d.findElement(By.name("artist")).isDisplayed());

            WebElement artistField = driver.findElement(By.name("artist"));
            WebElement titleField = driver.findElement(By.name("title"));
            artistField.clear();
            artistField.sendKeys(cover.artist);
            titleField.clear();
            titleField.sendKeys(cover.title);

            List<String> tracks = cover.tracks;
            for (int i = 0; i < Math.min(tracks.size(), MAX_TRACK_FIELDS); i++) {
                String name = "track" + (i + 1);
                WebElement trackInput = driver.findElement(By.name(name));
                trackInput.clear();
                trackInput.sendKeys(tracks.get(i));
            }

            driver.findElement(By.xpath("//input[@name='template' and @value='jewel']")).click();
            driver.findElement(By.xpath("//input[@name='size' and @value='a4']")).click();

            WebElement form = driver.findElement(By.cssSelector("form[action*='papercdcase']"));
            URI pdfUri = pdfRequestUriFromForm(form);

            WebElement btn = driver.findElement(By.name("submit"));
            btn.submit();

            Path target = resultDir.resolve("cd.pdf");
            Optional<Path> downloaded = pollNewestPdf(resultDir, Duration.ofSeconds(25));
            if (downloaded.isPresent()) {
                Files.copy(downloaded.get(), target, StandardCopyOption.REPLACE_EXISTING);
                if (!downloaded.get().equals(target)) {
                    Files.deleteIfExists(downloaded.get());
                }
            } else {
                fetchPdfWithCookies(pdfUri, driver, target);
            }
        } finally {
            driver.quit();
        }
    }

    private static Path resolveChromeDriver() {
        List<Path> candidates = List.of(
                Paths.get("C:\\chromedriver\\chromedriver.exe"),
                Paths.get("C:\\chromedriver.exe"));
        String env = System.getenv("CHROMEDRIVER_PATH");
        if (env != null && !env.isBlank()) {
            candidates = new ArrayList<>(candidates);
            candidates.add(0, Paths.get(env));
        }
        for (Path p : candidates) {
            if (Files.isRegularFile(p)) {
                return p.toAbsolutePath();
            }
        }
        throw new IllegalStateException(
                "ChromeDriver not found. Expected C:\\chromedriver\\chromedriver.exe or C:\\chromedriver.exe "
                        + "(or set CHROMEDRIVER_PATH).");
    }

    private static Optional<Path> pollNewestPdf(Path dir, Duration timeout)
            throws InterruptedException, IOException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            Optional<Path> pdf = newestCompletePdf(dir);
            if (pdf.isPresent() && isStableFile(pdf.get())) {
                return pdf;
            }
            Thread.sleep(400);
        }
        return Optional.empty();
    }

    private static URI pdfRequestUriFromForm(WebElement form) {
        String action = form.getDomAttribute("action");
        if (action == null || action.isBlank()) {
            throw new IllegalStateException("Form action is missing.");
        }
        URI target = URI.create(BASE_URL).resolve(action);
        String query = buildQueryFromForm(form);
        return URI.create(target + "?" + query);
    }

    private static String buildQueryFromForm(WebElement form) {
        List<WebElement> inputs = form.findElements(By.tagName("input"));
        StringJoiner joiner = new StringJoiner("&");
        for (WebElement input : inputs) {
            String type = Optional.ofNullable(input.getDomAttribute("type")).orElse("text").toLowerCase(Locale.ROOT);
            String name = input.getDomAttribute("name");
            if (name == null || name.isBlank()) {
                continue;
            }
            if ("submit".equals(type) || "button".equals(type) || "image".equals(type)) {
                continue;
            }
            if ("radio".equals(type) || "checkbox".equals(type)) {
                if (!input.isSelected()) {
                    continue;
                }
            }
            String rawValue;
            if ("radio".equals(type) || "checkbox".equals(type)) {
                rawValue = Optional.ofNullable(input.getDomAttribute("value")).orElse("on");
            } else {
                rawValue = Optional.ofNullable(input.getAttribute("value")).orElse("");
            }
            joiner.add(urlEncode(name) + "=" + urlEncode(rawValue));
        }
        return joiner.toString();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static void fetchPdfWithCookies(URI pdfUri, WebDriver driver, Path target)
            throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest.Builder rb = HttpRequest.newBuilder()
                .uri(pdfUri)
                .timeout(Duration.ofSeconds(120))
                .GET();
        String cookieHeader = driver.manage().getCookies().stream()
                .map(c -> c.getName() + "=" + c.getValue())
                .collect(Collectors.joining("; "));
        if (!cookieHeader.isEmpty()) {
            rb.header("Cookie", cookieHeader);
        }
        HttpResponse<byte[]> resp = client.send(rb.build(), HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("HTTP " + resp.statusCode() + " when fetching PDF: " + pdfUri);
        }
        byte[] body = resp.body();
        if (body.length < 4 || body[0] != '%' || body[1] != 'P' || body[2] != 'D' || body[3] != 'F') {
            throw new IllegalStateException("Response is not a PDF (" + body.length + " bytes) from: " + pdfUri);
        }
        Files.write(target, body, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static boolean isStableFile(Path file) throws InterruptedException, IOException {
        long s1 = Files.size(file);
        Thread.sleep(400);
        long s2 = Files.size(file);
        return s1 == s2 && s1 > 0;
    }

    private static Optional<Path> newestCompletePdf(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return Optional.empty();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString().toLowerCase();
                        return n.endsWith(".pdf") && !n.endsWith(".crdownload");
                    })
                    .max(Comparator.comparingLong(p -> lastModifiedSafe(p)));
        }
    }

    private static long lastModifiedSafe(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private record CoverData(String artist, String title, List<String> tracks) {
        static CoverData load(Path file) throws IOException {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                    .map(String::trim)
                    .collect(Collectors.toList());
            if (lines.size() < 2) {
                throw new IOException("data.txt must contain artist (line 1), title (line 2), then tracks.");
            }
            String artist = lines.get(0);
            String title = lines.get(1);
            List<String> tracks = lines.stream().skip(2).filter(s -> !s.isEmpty()).collect(Collectors.toList());
            return new CoverData(artist, title, tracks);
        }
    }
}
