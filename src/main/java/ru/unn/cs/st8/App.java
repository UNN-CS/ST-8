package ru.unn.cs.st8;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Fills papercdcase.com basic form from {@code data/data.txt} and saves the generated PDF as
 * {@code result/cd.pdf}.
 */
public final class App {

  private static final List<String> HOME_URL_TRIES =
      List.of("http://www.papercdcase.com/", "https://www.papercdcase.com/", "http://papercdcase.com/");

  private App() {}

  public static void main(String[] args) throws Exception {
    Path projectRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    Path dataPath = argDataPath(projectRoot, args);
    Path resultDir = projectRoot.resolve("result");

    if (!Files.isRegularFile(dataPath)) {
      System.err.println("Input file not found: " + dataPath);
      System.err.println(
          "Hint: run from the project directory (Maven: mvn exec:java -Dexec.workingdirectory=" + projectRoot + ")");
      System.exit(1);
      return;
    }

    CoverData data = CoverDataLoader.load(dataPath);
    ChromeOptions options = chromeOptions(resultDir);

    ChromeDriver driver = new ChromeDriver(options);
    Duration defaultPageLoadTimeout = Duration.ofMinutes(2);
    driver.manage().timeouts().pageLoadTimeout(defaultPageLoadTimeout);
    try {
      URI siteEntry = openHome(driver, Duration.ofSeconds(45));
      removeExistingPdfs(resultDir);

      PapercasePage page = new PapercasePage(driver, Duration.ofSeconds(35));
      page.fillCoverData(data);

      Duration shortPdfNav = Duration.ofSeconds(12);
      driver.manage().timeouts().pageLoadTimeout(shortPdfNav);
      try {
        page.submitGeneratingButton();
      } catch (Exception e) {
        // PDF navigation often hangs or stalls load events under Chromium/CDP — file is written via CGI HTTP below.
      } finally {
        driver.manage().timeouts().pageLoadTimeout(defaultPageLoadTimeout);
      }

      Path pdfOut = resultDir.resolve(PapercasePdfDownloader.OUTPUT_FILENAME);
      PapercasePdfDownloader.downloadPdf(siteEntry, data, pdfOut);
      System.out.println(pdfOut.normalize());
    } finally {
      driver.quit();
    }
  }

  private static Path argDataPath(Path projectRoot, String[] args) {
    if (args != null && args.length > 0 && !args[0].isBlank()) {
      Path p = Paths.get(args[0]);
      return p.isAbsolute() ? p.normalize() : projectRoot.resolve(p).normalize();
    }
    return projectRoot.resolve("data/data.txt").normalize();
  }

  private static void removeExistingPdfs(Path resultDir) throws IOException {
    Files.createDirectories(resultDir);
    try (var entries = Files.list(resultDir)) {
      for (Path p : entries.toList()) {
        if (!Files.isRegularFile(p)) {
          continue;
        }
        String name = p.getFileName().toString().toLowerCase();
        if (name.endsWith(".pdf")) {
          Files.deleteIfExists(p);
        }
      }
    }
  }

  private static ChromeOptions chromeOptions(Path resultDir) throws IOException {
    Files.createDirectories(resultDir);
    String folder = resultDir.toAbsolutePath().normalize().toString().replace('\\', '/');
    Map<String, Object> prefs = new HashMap<>();
    prefs.put("download.default_directory", folder);
    prefs.put("download.prompt_for_download", false);
    prefs.put("download.directory_upgrade", true);
    prefs.put("plugins.always_open_pdf_externally", true);

    ChromeOptions options = new ChromeOptions();
    options.setExperimentalOption("prefs", prefs);
    options.addArguments("--ignore-certificate-errors");
    options.addArguments("--disable-popup-blocking");
    options.setCapability("webSocketUrl", false);
    return options;
  }

  /** Tries HTTP first, then HTTPS, until the Artist field appears. */
  private static URI openHome(WebDriver driver, Duration implicitWaitBudget) throws Exception {
    WebDriverWait wait = new WebDriverWait(driver, implicitWaitBudget);
    Exception last = null;
    for (String url : HOME_URL_TRIES) {
      try {
        driver.get(url);
        wait.until(ExpectedConditions.visibilityOfElementLocated(PapercasePage.ARTIST));
        return URI.create(driver.getCurrentUrl());
      } catch (Exception e) {
        last = e;
      }
    }
    IllegalStateException ex = new IllegalStateException("Unable to load papercase homepage.");
    if (last != null) {
      ex.initCause(last);
    }
    throw ex;
  }
}
