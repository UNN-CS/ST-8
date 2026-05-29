import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {

    public static void main(String[] args) {

        System.setProperty(
                "webdriver.chrome.driver",
                "C:\\Users\\aa\\Downloads\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");

        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);

        String downloadPath = System.getProperty("user.dir") + "\\result";

        Map<String, Object> prefs = new HashMap<>();

        prefs.put("download.default_directory", downloadPath);
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("plugins.always_open_pdf_externally", true);
        prefs.put("profile.default_content_settings.popups", 0);

        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--remote-allow-origins=*");

        WebDriver driver = new ChromeDriver(options);

        driver.manage().timeouts()
                .implicitlyWait(Duration.ofSeconds(30));

        try {
            driver.get("https://www.papercdcase.com");

            List<String> lines = Files.readAllLines(Paths.get("data/data.txt"));

            String artist = lines.get(0);
            String album = lines.get(1);
            WebElement artistField = driver.findElement(By.xpath(
                    "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input"));

            artistField.sendKeys(artist);
            WebElement titleField = driver.findElement(By.xpath(
                    "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input"));

            titleField.sendKeys(album);

            for (int i = 3; i < lines.size(); i++) {

                int trackNumber = i - 2;

                WebElement trackField = driver.findElement(By.xpath(
                        "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr["
                                + trackNumber +
                                "]/td[2]/input"));

                trackField.sendKeys(lines.get(i));
            }

            WebElement jewelCase = driver.findElement(By.xpath(
                    "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]/input[2]"));

            jewelCase.click();
            WebElement a4Radio = driver.findElement(By.xpath(
                    "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]/input[2]"));

            a4Radio.click();
            WebElement submitButton = driver.findElement(By.xpath(
                    "/html/body/table[2]/tbody/tr/td[1]/div/form/p/input"));

            submitButton.click();
            Thread.sleep(5000);
            boolean downloaded = false;
            for (int i = 0; i < 60; i++) {
                File folder = new File("result");
                File[] files = folder.listFiles();
                boolean hasCrdownload = false;
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().endsWith(".crdownload")) {
                            hasCrdownload = true;
                        }
                    }
                    if (!hasCrdownload) {
                        for (File file : files) {

                            if (file.getName().endsWith(".pdf")) {

                                File newFile = new File("result/cd.pdf");

                                if (newFile.exists()) {
                                    newFile.delete();
                                }

                                boolean renamed = file.renameTo(newFile);
                                if (renamed) {
                                    System.out.println("PDF saved: result/cd.pdf");
                                    downloaded = true;
                                }
                                break;
                            }
                        }
                    }
                }
                if (downloaded) {
                    break;
                }
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}
