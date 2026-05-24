import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class App {
    private static final String BASE_URL = "http://www.papercdcase.com";

    /*
     * XPath-адреса ключевых элементов формы:
     * Artist:  //input[@name='artist']
     * Title:   //input[@name='title']
     * Tracks:  //input[@name='track1'] ... //input[@name='track16']
     * Type:    //input[@name='template' and @value='jewel']
     * Paper:   //input[@name='size' and @value='a4']
     * Submit:  //input[@name='submit']
     */

    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", getChromeDriverPath());
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--ignore-certificate-errors");
        WebDriver webDriver = new ChromeDriver(options);
        try {
            webDriver.get(BASE_URL);

            List<String> dataLines = Files.readAllLines(Paths.get("data/data.txt")).stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList());

            String artist = dataLines.get(0);
            String title = dataLines.get(1);
            List<String> tracks = dataLines.subList(2, dataLines.size());

            webDriver.findElement(By.xpath("//input[@name='artist']")).sendKeys(artist);
            webDriver.findElement(By.xpath("//input[@name='title']")).sendKeys(title);

            for (int i = 0; i < tracks.size() && i < 16; ++i) {
                webDriver.findElement(By.xpath("//input[@name='track" + (i + 1) + "']"))
                        .sendKeys(tracks.get(i));
            }

            webDriver.findElement(By.xpath("//input[@name='size' and @value='a4']")).click();
            webDriver.findElement(By.xpath("//input[@name='template' and @value='jewel']")).click();

            WebElement submitButton = webDriver.findElement(By.xpath("//input[@name='submit']"));
            submitButton.submit();

            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(120));
            wait.until(driver -> driver.getCurrentUrl().contains(".pdf"));

            String pdfUrl = webDriver.getCurrentUrl();
            if (pdfUrl.startsWith("https://")) {
                pdfUrl = "http://" + pdfUrl.substring("https://".length());
            }
            Path outputFile = Paths.get("result/cd.pdf");
            try (InputStream inputStream = new URL(pdfUrl).openStream()) {
                Files.copy(inputStream, outputFile, StandardCopyOption.REPLACE_EXISTING);
            }

            System.out.println("PDF saved to: " + outputFile.toAbsolutePath());
        } catch (Exception e) {
            System.out.println("Error");
            System.out.println(e.toString());
        } finally {
            webDriver.quit();
        }
    }

    private static String getChromeDriverPath() {
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        if (os.contains("mac")) {
            if (arch.contains("aarch64") || arch.contains("arm")) {
                return projectRoot.resolve("drivers/chromedriver-mac-arm64/chromedriver").toString();
            }
            return projectRoot.resolve("drivers/chromedriver-mac-x64/chromedriver").toString();
        }
        if (os.contains("linux")) {
            return projectRoot.resolve("drivers/chromedriver-linux64/chromedriver").toString();
        }
        return projectRoot.resolve("drivers/chromedriver.exe").toString();
    }
}
