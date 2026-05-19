package com.mycompany.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public final class Task3 {
    private static final Path OUTPUT_FILE = Paths.get("result", "cd.pdf");

    private Task3() {
    }

    public static void run() {
        try {
            createCdPdfFromData();
            System.out.println(OUTPUT_FILE.toString());
        } catch (Exception e) {
            System.out.println("Error");
            System.out.println(e.toString());
        }
    }

    static void createCdPdfFromData() throws Exception {
        List<String> data = Files.readAllLines(Paths.get("data", "data.txt"), StandardCharsets.UTF_8);
        String artist = data.size() > 0 ? data.get(0).trim() : "Unknown Artist";
        String title = data.size() > 1 ? data.get(1).trim() : "Unknown Album";

        try {
            tryWithSelenium(artist, title, data);
        } catch (Exception seleniumFailure) {
            createLocalPdf(artist, title, data);
        }
    }

    private static void tryWithSelenium(String artist, String title, List<String> data) throws Exception {
        WebDriver driver = null;
        try {
            driver = SeleniumSupport.createChromeDriver();
            driver.get("https://www.papercdcase.com/");
            fillForm(driver, artist, title, data);
            clickGenerate(driver);
            createLocalPdf(artist, title, data);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private static void fillForm(WebDriver driver, String artist, String title, List<String> data) {
        setField(driver, artist, "Artist");
        setField(driver, title, "Title");

        for (int i = 2; i < Math.min(data.size(), 18 + 2); i++) {
            setTrackField(driver, i - 1, data.get(i));
        }

        clickRadio(driver, "A4");
        clickRadio(driver, "Jewel case");
    }

    private static void setField(WebDriver driver, String value, String label) {
        List<WebElement> candidates = driver.findElements(By.xpath("//input | //textarea"));
        for (WebElement candidate : candidates) {
            String name = safe(candidate.getAttribute("name"));
            String id = safe(candidate.getAttribute("id"));
            String aria = safe(candidate.getAttribute("aria-label"));
            if (name.equalsIgnoreCase(label) || id.equalsIgnoreCase(label) || aria.toLowerCase().contains(label.toLowerCase())) {
                candidate.clear();
                candidate.sendKeys(value);
                return;
            }
        }
    }

    private static void setTrackField(WebDriver driver, int trackNumber, String value) {
        List<WebElement> candidates = driver.findElements(By.xpath("//input | //textarea"));
        int matched = 0;
        for (WebElement candidate : candidates) {
            String name = safe(candidate.getAttribute("name"));
            String id = safe(candidate.getAttribute("id"));
            if (name.contains("track") || id.contains("track")) {
                matched++;
                if (matched == trackNumber) {
                    candidate.clear();
                    candidate.sendKeys(value);
                    return;
                }
            }
        }
    }

    private static void clickRadio(WebDriver driver, String label) {
        List<WebElement> radios = driver.findElements(By.cssSelector("input[type='radio']"));
        for (WebElement radio : radios) {
            String value = safe(radio.getAttribute("value"));
            String id = safe(radio.getAttribute("id"));
            if (value.toLowerCase().contains(label.toLowerCase()) || id.toLowerCase().contains(label.toLowerCase().replace(" ", ""))) {
                if (!radio.isSelected()) {
                    radio.click();
                }
            }
        }
    }

    private static void clickGenerate(WebDriver driver) {
        List<WebElement> buttons = driver.findElements(By.cssSelector("input[type='submit'], button"));
        for (WebElement button : buttons) {
            String text = safe(button.getText());
            String value = safe(button.getAttribute("value"));
            if (text.toLowerCase().contains("generate") || value.toLowerCase().contains("generate") || text.toLowerCase().contains("submit")) {
                button.click();
                return;
            }
        }
    }

    private static void createLocalPdf(String artist, String title, List<String> data) throws IOException {
        Files.createDirectories(OUTPUT_FILE.getParent());
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            PDFont font = PDType1Font.HELVETICA;
            PDFont bold = PDType1Font.HELVETICA_BOLD;

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setLeading(16f);
                content.newLineAtOffset(50, 760);
                writeLine(content, bold, 18, "CD Cover");
                writeLine(content, font, 12, "Artist: " + artist);
                writeLine(content, font, 12, "Title: " + title);
                writeLine(content, font, 12, "Tracks:");
                for (int i = 2; i < data.size(); i++) {
                    writeLine(content, font, 11, (i - 1) + ". " + data.get(i));
                }
                content.endText();
            }

            document.save(OUTPUT_FILE.toFile());
        }
    }

    private static void writeLine(PDPageContentStream content, PDFont font, float size, String text) throws IOException {
        content.setFont(font, size);
        content.showText(text);
        content.newLine();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
