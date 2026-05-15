import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {

    public static void main(String[] args) throws Exception {
        Path baseDir = Paths.get(System.getProperty("user.dir"));
        Path resultDir = baseDir.resolve("result");
        Files.createDirectories(resultDir);

        List<String> lines = Files.readAllLines(baseDir.resolve("data").resolve("data.txt"));
        String artist = lines.get(0).trim();
        String title = lines.get(1).trim();

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", resultDir.toAbsolutePath().toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);

        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--allow-insecure-localhost");

        WebDriver webDriver = new ChromeDriver(options);
        webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));

        try {
            webDriver.get("http://www.papercdcase.com/");

            webDriver.findElement(By.xpath("//input[@name='artist']")).sendKeys(artist);
            webDriver.findElement(By.xpath("//input[@name='title']")).sendKeys(title);

            for (int i = 2; i < lines.size() && i < 18; i++) {
                String track = lines.get(i).trim();
                if (track.isEmpty()) {
                    continue;
                }
                int trackNum = i - 1;
                webDriver.findElement(By.xpath("//input[@name='track" + trackNum + "']")).sendKeys(track);
            }

            webDriver.findElement(By.xpath("//input[@name='size' and @value='a4']")).click();
            webDriver.findElement(By.xpath("//input[@name='template' and @value='jewel']")).click();
            webDriver.findElement(By.xpath("//input[@name='force_saveas']")).click();

            WebElement btn = webDriver.findElement(By.xpath("//input[@name='submit']"));
            btn.submit();

            Path downloaded = resultDir.resolve("papercdcase.pdf");
            Path target = resultDir.resolve("cd.pdf");
            long deadline = System.currentTimeMillis() + 120_000;

            while (System.currentTimeMillis() < deadline) {
                if (downloaded.toFile().exists() && downloaded.toFile().length() > 0) {
                    Files.move(downloaded, target, StandardCopyOption.REPLACE_EXISTING);
                    break;
                }
                Thread.sleep(500);
            }

            if (!target.toFile().exists() || target.toFile().length() == 0) {
                throw new IllegalStateException("PDF was not saved to result/cd.pdf");
            }
        } finally {
            webDriver.quit();
        }
    }
}
