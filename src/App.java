import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {

    public static void main(String[] args) throws Exception {
        List<String> lines = Files.readAllLines(Paths.get("data/data.txt"));

        String artist = "";
        String title = "";
        List<String> tracks = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            if (i == 0) {
                artist = line;
            } else if (i == 1) {
                title = line;
            } else if (tracks.size() < 16) {
                tracks.add(line);
            }
        }

        String downloadDir = new File("result").getAbsolutePath();
        new File(downloadDir).mkdirs();

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("plugins.always_open_pdf_externally", true);
        prefs.put("download.default_directory", downloadDir);
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);

        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", prefs);
        options.setAcceptInsecureCerts(true);

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get("http://www.papercdcase.com/index.php");

            WebElement artistField = driver.findElement(By.name("artist"));
            artistField.clear();
            artistField.sendKeys(artist);

            WebElement titleField = driver.findElement(By.name("title"));
            titleField.clear();
            titleField.sendKeys(title);

            for (int i = 0; i < tracks.size(); i++) {
                WebElement trackField = driver.findElement(By.name("track" + (i + 1)));
                trackField.sendKeys(tracks.get(i));
            }

            WebElement sizeA4 = driver.findElement(By.xpath("//input[@name='size' and @value='a4']"));
            if (!sizeA4.isSelected()) {
                sizeA4.click();
            }

            WebElement jewelCase = driver.findElement(By.xpath("//input[@name='template' and @value='jewel']"));
            if (!jewelCase.isSelected()) {
                jewelCase.click();
            }

            WebElement forceSaveAs = driver.findElement(By.name("force_saveas"));
            if (!forceSaveAs.isSelected()) {
                forceSaveAs.click();
            }

            WebElement btn = driver.findElement(By.name("submit"));
            btn.submit();

            Thread.sleep(7000);

            File resultDir = new File(downloadDir);
            File[] pdfs = resultDir.listFiles((d, name) -> name.endsWith(".pdf") && !name.equals("cd.pdf"));
            if (pdfs != null && pdfs.length > 0) {
                Path target = Paths.get(downloadDir, "cd.pdf");
                Files.move(pdfs[0].toPath(), target, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Saved: " + target.toAbsolutePath());
            } else {
                File cdPdf = new File(downloadDir, "cd.pdf");
                if (cdPdf.exists()) {
                    System.out.println("PDF already saved as cd.pdf: " + cdPdf.getAbsolutePath());
                } else {
                    System.err.println("PDF not found in: " + downloadDir);
                }
            }

        } finally {
            driver.quit();
        }
    }
}
