import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
        clearOldPdfs(resultDir);

        List<String> xpaths = loadXpaths(xpathFile);
        List<String> rawLines = Files.readAllLines(dataFile, StandardCharsets.UTF_8);
        FormData data = parseData(rawLines);

        Path downloadDir = resultDir.toAbsolutePath().normalize();
        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadDir.toString());
        prefs.put("plugins.always_open_pdf_externally", true);
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--disable-popup-blocking");

        WebDriver driver = new ChromeDriver(options);
        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
            driver.get(BASE_URL);

            driver.findElement(By.xpath(xpaths.get(0))).sendKeys(data.artist);
            driver.findElement(By.xpath(xpaths.get(1))).sendKeys(data.title);

            for (int i = 0; i < data.tracks.size(); i++) {
                driver.findElement(By.xpath(xpaths.get(IDX_FIRST_TRACK + i))).sendKeys(data.tracks.get(i));
            }

            WebElement typeCell = driver.findElement(By.xpath(xpaths.get(IDX_TYPE_CELL)));
            typeCell.findElement(By.xpath(".//input[@name='template' and @value='jewel']")).click();

            WebElement paperCell = driver.findElement(By.xpath(xpaths.get(IDX_PAPER_CELL)));
            paperCell.findElement(By.xpath(".//input[@name='size' and @value='a4']")).click();

            WebElement fontCell = driver.findElement(By.xpath(xpaths.get(IDX_FONT_CELL)));
            fontCell.findElement(By.xpath(".//input[@name='lang' and @value='west']")).click();

            WebElement forceCell = driver.findElement(By.xpath(xpaths.get(IDX_FORCE_CELL)));
            forceCell.findElement(By.xpath(".//input[@name='force_saveas' and @value='yes']")).click();

            driver.findElement(By.xpath(xpaths.get(IDX_SUBMIT))).click();

            Path pdfOut = waitForPdf(downloadDir);
            Path target = downloadDir.resolve("cd.pdf");
            Files.deleteIfExists(target);
            Files.move(pdfOut, target);
            System.out.println("Saved: " + target);
        } finally {
            driver.quit();
        }
    }

    private static List<String> loadXpaths(Path file) throws IOException {
        List<String> out = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String s = line.strip();
            if (s.isEmpty()) {
                continue;
            }
            out.add(s);
        }
        if (out.size() != XPATH_COUNT) {
            throw new IllegalStateException(
                    "data/xpaths.txt: expected " + XPATH_COUNT + " XPath lines, got " + out.size());
        }
        return out;
    }

    private static FormData parseData(List<String> rawLines) {
        List<String> lines = new ArrayList<>();
        for (String line : rawLines) {
            String t = line.stripTrailing();
            if (!t.isBlank()) {
                lines.add(t);
            }
        }
        if (lines.size() < 2) {
            throw new IllegalArgumentException("data.txt: нужны минимум 2 строки (Artist, Title).");
        }
        String artist = lines.get(0);
        String title = lines.get(1);
        List<String> tracks = new ArrayList<>();
        for (int i = 2; i < lines.size() && tracks.size() < MAX_TRACKS; i++) {
            tracks.add(lines.get(i));
        }
        return new FormData(artist, title, tracks);
    }

    private static void clearOldPdfs(Path dir) throws IOException {
        try (var stream = Files.list(dir)) {
            for (Path p : stream.toList()) {
                String n = p.getFileName().toString().toLowerCase();
                if (n.endsWith(".pdf") || n.endsWith(".crdownload")) {
                    Files.deleteIfExists(p);
                }
            }
        }
    }

    private static Path waitForPdf(Path dir) throws IOException, InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(120));
        while (Instant.now().isBefore(deadline)) {
            try (var stream = Files.list(dir)) {
                for (Path p : stream.toList()) {
                    String n = p.getFileName().toString().toLowerCase();
                    if (!n.endsWith(".pdf") || n.endsWith(".crdownload")) {
                        continue;
                    }
                    if (isStableFile(p)) {
                        return p;
                    }
                }
            }
            Thread.sleep(300);
        }
        throw new IllegalStateException("PDF не появился в " + dir + " за отведённое время.");
    }

    private static boolean isStableFile(Path p) throws IOException, InterruptedException {
        long a = Files.size(p);
        Thread.sleep(400);
        long b = Files.size(p);
        return a == b && a > 0;
    }

    private record FormData(String artist, String title, List<String> tracks) {}
}
