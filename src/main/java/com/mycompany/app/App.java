package com.mycompany.app;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hello world!
 */
public class App {
    static final String RESULT_DIR = System.getProperty("user.dir") + File.separator + "result";
    static final String RESULT_FILE = System.getProperty("cd.pdf");

    // XPath-адреса
    static final String XPATH_ARTIST     = "//input[@name='artist']";
    static final String XPATH_TITLE      = "//input[@name='title']";
    static final String XPATH_JEWEL      = "//input[@name='template'][@value='jewel']"; // Type: Jewel case
    static final String XPATH_A4         = "//input[@name='size'][@value='a4']";       // Paper: A4
    static final String XPATH_FORCE_SAVE = "//input[@name='force_saveas']";
    static final String XPATH_SUBMIT     = "//input[@name='submit']";
    static final String XPATH_TRACK_BASE = "//input[starts-with(@name,'track')]";

    public static void main(String[] args) {
        new File(RESULT_DIR).mkdirs();

        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", RESULT_DIR);
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);
        options.setBinary("D:\\chrome-win64\\chrome.exe");
        options.addArguments("--disable-features=InsecureDownloadWarnings");
        options.addArguments("--allow-running-insecure-content");
        options.setAcceptInsecureCerts(true);

        System.setProperty("webdriver.chrome.driver", "D:\\chromedriver-win64\\chromedriver.exe");
        WebDriver driver = new ChromeDriver(options);
        try {
            driver.get("http://www.papercdcase.com/");

            saveXpathAddress();

            DataRecord data = readDataFromFile("data/NBSPLV.txt");

            deleteOldPdfs(RESULT_DIR);

            fillAndSubmitForm(driver, data.artist, data.album, data.tracks);

            System.out.println("Ожидание загрузки PDF...");
            Thread.sleep(5000);
            saveDownloadedPdf(RESULT_DIR);
            System.out.println("Готово! Обложка сохранена в result/cd.pdf");
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    private record DataRecord (
            String artist,
            String album,
            List<String>tracks
    ){}

    private static DataRecord readDataFromFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("Файл " + filePath + " не найден.");
            return null;
        }
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            String artist = lines.size() > 0 ? lines.get(0) : "";
            String album  = lines.size() > 1 ? lines.get(1) : "";
            List<String> tracks = new ArrayList<>();
            for (int i = 2; i < lines.size() && tracks.size() < 18; i++) {
                tracks.add(lines.get(i));
            }
            return new DataRecord(artist, album, tracks);
        } catch (IOException e) {
            System.err.println("Ошибка чтения файла: " + e.getMessage());
            return null;
        }
    }

    private static void saveXpathAddress() throws FileNotFoundException {
        PrintWriter pw = new PrintWriter("data/xpath_addresses.txt");
        pw.println("Artist: " + XPATH_ARTIST);
        pw.println("Title: " + XPATH_TITLE);
        pw.println("Tracks (общий шаблон): " + XPATH_TRACK_BASE);
        pw.println("Type (Jewel Case): " + XPATH_JEWEL);
        pw.println("Paper (A4): " + XPATH_A4);
        pw.println("Force Save-as: " + XPATH_FORCE_SAVE);
        pw.println("Submit button: " + XPATH_SUBMIT);
        pw.flush();
        pw.close();
        System.out.println("Файл xpath_addresses.txt записан в папку data.");
    }

    private static void fillAndSubmitForm(WebDriver driver, String artist, String album, List<String> tracks) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(3000));

        WebElement artistField = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@name='artist']")));
        artistField.sendKeys(artist != null ? artist : "");

        // Title
        WebElement titleField = driver.findElement(By.xpath("//input[@name='title']"));
        titleField.sendKeys(album != null ? album : "");

        for (int i = 1; i <= tracks.size() && i <= 16; i++) {
            try {
                WebElement trackField = driver.findElement(By.xpath("//input[@name='track" + i + "']"));
                trackField.clear();
                if (i <= tracks.size()) {
                    trackField.sendKeys(tracks.get(i - 1));
                }
            } catch (NoSuchElementException e) {
                System.err.println("Поле track" + i + " не найдено.");
            }
        }
        WebElement jewelRadio = driver.findElement(By.xpath(XPATH_JEWEL));
        if (!jewelRadio.isSelected()) jewelRadio.click();

        WebElement a4Radio = driver.findElement(By.xpath(XPATH_A4));
        if (!a4Radio.isSelected()) a4Radio.click();

        WebElement forceSaveCheckbox = driver.findElement(By.xpath(XPATH_FORCE_SAVE));
        if (!forceSaveCheckbox.isSelected()) forceSaveCheckbox.click();

        driver.findElement(By.xpath(XPATH_SUBMIT)).submit();
    }

    private static void saveDownloadedPdf(String dirPath) throws Exception {
        File dir = new File(dirPath);
        File[] pdfs = dir.listFiles((d, name) -> name.equals("papercdcase.pdf"));
        if (pdfs == null || pdfs.length == 0) {
            throw new InvalidPathException(RESULT_DIR, "PDF-файл не найден в папке");
        }
        File downloaded = pdfs[0];
        File target = new File(dir, "cd.pdf");
        if (target.exists()) target.delete();
        if (!downloaded.renameTo(target)) {
            throw new Exception("Не удалось переименовать файл в cd.pdf");
        }
    }

    private static void deleteOldPdfs(String resultDir) {
        File dir = new File(resultDir);
        File[] pdfs = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));
        if (pdfs != null) {
            for (File f : pdfs) f.delete();
        }
    }
}
