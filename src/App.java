import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {

    public static void main(String[] args) throws Exception {
        List<String> data = readData("data/data.txt");
        String artist = data.get(0);
        String title = data.get(1);
        List<String> tracks = data.subList(2, Math.min(data.size(), 20));

        String downloadPath = new File("result").getAbsolutePath();

        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadPath);
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);

        File driverFile = new File("chromedriver.exe");
        if (driverFile.exists()) {
            System.setProperty("webdriver.chrome.driver", driverFile.getAbsolutePath());
        }

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get("http://www.papercdcase.com/index.php");

            driver.findElement(By.xpath("//input[@name='artist']")).sendKeys(artist);
            driver.findElement(By.xpath("//input[@name='title']")).sendKeys(title);

            for (int i = 0; i < Math.min(tracks.size(), 18); i++) {
                WebElement trackField = driver.findElement(
                    By.xpath("//input[@name='track" + (i + 1) + "']"));
                trackField.sendKeys(tracks.get(i));
            }

            driver.findElement(By.xpath("//input[@value='a4']")).click();
            driver.findElement(By.xpath("//input[@value='jewel']")).click();

            ((JavascriptExecutor) driver).executeScript("document.forms[0].submit();");

            Thread.sleep(5000);

            renameDownloadedPdf();
        } finally {
            driver.quit();
        }
    }

    private static void renameDownloadedPdf() {
        File resultDir = new File("result");
        File[] pdfs = resultDir.listFiles((dir, name) ->
            name.endsWith(".pdf") && !name.equals("cd.pdf"));
        if (pdfs != null && pdfs.length > 0) {
            pdfs[0].renameTo(new File("result/cd.pdf"));
        }
    }

    private static List<String> readData(String path) throws Exception {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }
}
