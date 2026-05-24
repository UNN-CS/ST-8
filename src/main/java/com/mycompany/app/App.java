package com.mycompany.app;

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

public class App {
    private static final String SITE_URL = "http://www.papercdcase.com";
    private static final String DRIVER_PATH =
            "/Users/madw3y/Downloads/chromedriver-mac-arm64/chromedriver";
    private static final String DATA_FILE = "data/data.txt";
    private static final String RESULT_FILE = "cd.pdf";

    public static void main(String[] args) throws Exception {
        String downloadPath = createResultDirectory();

        WebDriver driver = createDriver(downloadPath);

        openWebsite(driver);

        List<String> lines = readDataFile();

        fillForm(driver, lines);

        selectOptions(driver);

        generatePdf(driver);

        File pdfFile = waitForPdf(downloadPath);

        renamePdf(pdfFile, downloadPath);

        driver.quit();
    }

    private static String createResultDirectory() {
        String projectPath = System.getProperty("user.dir");

        String downloadPath = projectPath + "/result";

        File resultDir = new File(downloadPath);

        if (!resultDir.exists()) {
            resultDir.mkdirs();
        }

        System.out.println("Download path: " + downloadPath);

        return downloadPath;
    }

    private static WebDriver createDriver(String downloadPath) {

        ChromeOptions options = new ChromeOptions();

        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--allow-running-insecure-content");

        options.addArguments(
                "--unsafely-treat-insecure-origin-as-secure=http://www.papercdcase.com"
        );

        HashMap<String, Object> prefs = new HashMap<>();

        prefs.put("download.default_directory", downloadPath);
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("plugins.always_open_pdf_externally", true);
        prefs.put("safebrowsing.enabled", true);

        options.setExperimentalOption("prefs", prefs);

        System.setProperty(
                "webdriver.chrome.driver",
                DRIVER_PATH
        );

        WebDriver driver = new ChromeDriver(options);

        driver.manage()
                .timeouts()
                .implicitlyWait(Duration.ofSeconds(10));

        return driver;
    }

    private static void openWebsite(WebDriver driver) {
        driver.get(SITE_URL);
    }

    private static List<String> readDataFile() throws Exception {
        return Files.readAllLines(
                Paths.get(DATA_FILE)
        );
    }

    private static void fillForm(WebDriver driver, List<String> lines) {
        String artist = lines.get(0);
        String album = lines.get(1);

        fillArtist(driver, artist);
        fillAlbum(driver, album);
        fillTracks(driver, lines);
    }

    private static void fillArtist(WebDriver driver, String artist) {
        WebElement artistField = driver.findElement(
                By.xpath("//input[@name='artist']")
        );

        artistField.sendKeys(artist);
    }

    private static void fillAlbum(WebDriver driver, String album) {
        WebElement titleField = driver.findElement(
                By.xpath("//input[@name='title']")
        );

        titleField.sendKeys(album);
    }

    private static void fillTracks(WebDriver driver, List<String> lines) {
        int trackNumber = 1;

        for (int i = 2; i < lines.size() && trackNumber <= 16; i++) {
            WebElement trackField = driver.findElement(
                    By.xpath(
                            "//input[@name='track"
                                    + trackNumber
                                    + "']"
                    )
            );

            trackField.sendKeys(lines.get(i));
            trackNumber++;
        }
    }

    private static void selectOptions(WebDriver driver) {
        selectJewelCase(driver);
        selectA4(driver);
        enableForceSaveAs(driver);
    }

    private static void selectJewelCase(WebDriver driver) {
        WebElement jewelRadio = driver.findElement(
                By.xpath(
                        "//input[@name='template' and @value='jewel']"
                )
        );

        jewelRadio.click();
    }

    private static void selectA4(WebDriver driver) {
        WebElement a4Radio = driver.findElement(
                By.xpath(
                        "//input[@name='size' and @value='a4']"
                )
        );

        a4Radio.click();
    }

    private static void enableForceSaveAs(WebDriver driver) {
        WebElement saveAsCheckbox = driver.findElement(
                By.xpath("//input[@name='force_saveas']")
        );

        saveAsCheckbox.click();
    }

    private static void generatePdf(WebDriver driver) {
        WebElement submitButton = driver.findElement(
                By.xpath("//input[@name='submit']")
        );

        submitButton.click();
    }

    private static File waitForPdf(String downloadPath) throws Exception {
        File resultDir = new File(downloadPath);
        File downloadedPdf = null;

        int waitTime = 0;
        while (waitTime < 60) {
            File[] files = resultDir.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (
                            file.getName().endsWith(".pdf")
                                    &&
                                    !file.getName().equals(RESULT_FILE)
                    ) {
                        downloadedPdf = file;
                        break;
                    }
                }
            }

            if (downloadedPdf != null) {
                break;
            }

            Thread.sleep(1000);

            waitTime++;
        }

        return downloadedPdf;
    }

    private static void renamePdf(File downloadedPdf, String downloadPath) {
        if (downloadedPdf == null) {
            System.out.println("PDF файл не найден");
            return;
        }

        File newFile = new File(
                downloadPath + "/" + RESULT_FILE
        );

        if (newFile.exists()) {
            newFile.delete();
        }

        boolean renamed = downloadedPdf.renameTo(newFile);

        if (renamed) {
            System.out.println(
                    "PDF успешно сохранен: result/cd.pdf"
            );

        } else {
            System.out.println(
                    "Ошибка переименования файла"
            );
        }
    }
}
