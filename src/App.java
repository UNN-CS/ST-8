import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {

    public static void main(String[] args) throws Exception {
        List<String> lines = Files.readAllLines(Paths.get("data/data.txt"));
        String artist = lines.get(0).trim();
        String title = lines.get(1).trim();
        List<String> tracks = new ArrayList<>();
        for (int i = 2; i < lines.size() && tracks.size() < 16; i++) {
            String line = lines.get(i).trim();
            if (!line.isEmpty()) {
                tracks.add(line);
            }
        }

        String downloadPath = new File("result").getAbsolutePath();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadPath);
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("plugins.always_open_pdf_externally", true);

        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--ignore-certificate-errors");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        try {
            driver.get("http://www.papercdcase.com/index.php");

            // Artist
            WebElement artistField = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@name='artist']")));
            artistField.clear();
            artistField.sendKeys(artist);

            // Title
            WebElement titleField = driver.findElement(By.xpath("//input[@name='title']"));
            titleField.clear();
            titleField.sendKeys(title);

            // Tracks: track1..track16
            for (int i = 0; i < tracks.size(); i++) {
                String trackName = "track" + (i + 1);
                WebElement trackField = driver.findElement(By.xpath("//input[@name='" + trackName + "']"));
                trackField.sendKeys(tracks.get(i));
            }

            // Paper: A4 (name=size, value=a4)
            WebElement a4Radio = driver.findElement(By.xpath("//input[@name='size' and @value='a4']"));
            if (!a4Radio.isSelected()) {
                a4Radio.click();
            }

            // Type: Jewel Case (name=template, value=jewel)
            WebElement jewelRadio = driver.findElement(By.xpath("//input[@name='template' and @value='jewel']"));
            if (!jewelRadio.isSelected()) {
                jewelRadio.click();
            }

            // Submit (type=image)
            WebElement submitBtn = driver.findElement(By.xpath("//input[@type='image']"));
            submitBtn.submit();

            // Wait for PDF download to finish (no .crdownload files remain)
            File resultDir = new File("result");
            long deadline = System.currentTimeMillis() + 30_000;
            while (System.currentTimeMillis() < deadline) {
                File[] inProgress = resultDir.listFiles((d, n) -> n.endsWith(".crdownload"));
                File[] pdfs = resultDir.listFiles((d, n) -> n.endsWith(".pdf") && !n.equals("cd.pdf"));
                if ((inProgress == null || inProgress.length == 0) && pdfs != null && pdfs.length > 0) {
                    break;
                }
                Thread.sleep(500);
            }

            // Rename the downloaded PDF to cd.pdf
            File[] pdfs = resultDir.listFiles((d, n) -> n.endsWith(".pdf") && !n.equals("cd.pdf"));
            if (pdfs != null && pdfs.length > 0) {
                Arrays.sort(pdfs, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                Files.move(pdfs[0].toPath(), Paths.get("result/cd.pdf"), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Saved: result/cd.pdf");
            } else {
                System.out.println("PDF not found in result/ — check if download succeeded.");
            }
        } finally {
            driver.quit();
        }
    }
}
