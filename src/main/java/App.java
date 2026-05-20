import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class App {
    public static void main(String[] args) throws Exception {
        Path project = Paths.get("").toAbsolutePath();
        Path dataFile = project.resolve("data").resolve("data.txt");
        Path resultDir = project.resolve("result");
        Files.createDirectories(resultDir);

        List<String> lines = readDataFile(dataFile);
        if (lines.size() < 2) {
            System.err.println("data/data.txt должен содержать минимум: исполнитель (строка1) и название альбома (строка2)");
            return;
        }
        String artist = lines.get(0);
        String title = lines.get(1);
        List<String> tracks = lines.size() > 2 ? lines.subList(2, Math.min(lines.size(), 20)) : Collections.emptyList();

        String driverPath = System.getenv("CHROMEDRIVER_PATH");
        if (driverPath == null || driverPath.isEmpty()) {
            driverPath = project.resolve("drivers").resolve("chromedriver").toString();
        }
        System.setProperty("webdriver.chrome.driver", driverPath);

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", resultDir.toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);

        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", prefs);
        options.setAcceptInsecureCerts(true);

        WebDriver driver = new ChromeDriver(options);
        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
            driver.get("https://www.papercdcase.com");
            Thread.sleep(1500);

            // Попытки найти и заполнить поля. Возможно, потребуется откорректировать локаторы под реальную страницу.
            safeSendKeys(driver, new By[]{By.name("artist"), By.xpath("//input[contains(@id,'artist')]")}, artist);
            safeSendKeys(driver, new By[]{By.name("title"), By.xpath("//input[contains(@id,'title')]")}, title);

            // Заполнение треков (если присутствуют элементы). Подставьте корректные локаторы при необходимости.
            for (int i = 0; i < tracks.size(); i++) {
                String t = tracks.get(i);
                By[] trackLocators = new By[]{
                        By.xpath("//input[contains(@name,'track') and @data-index='" + i + "']"),
                        By.xpath("(//input[contains(@name,'track')])['" + (i + 1) + "']")
                };
                safeSendKeys(driver, trackLocators, t);
            }

            // Пример выбора переключателей (формат A4, Jewel Case) — откорректируйте локаторы под страницу
            safeClick(driver, new By[]{By.xpath("//input[@type='radio' and contains(@value,'a4') ]"), By.xpath("//label[contains(.,'A4')]/input")});
            safeClick(driver, new By[]{By.xpath("//input[@type='radio' and contains(@value,'jewel') ]"), By.xpath("//label[contains(.,'Jewel')]/input")});

            // Нажать кнопку генерации
            boolean submitted = safeSubmit(driver, new By[]{By.xpath("//input[@type='submit']"), By.xpath("//button[contains(.,'Generate')]")});
            if (!submitted) System.out.println("Кнопка отправки не найдена — проверьте локаторы в коде.");

            // Дождаться появление PDF в папке result
            Path pdf = waitForPdf(resultDir, Duration.ofSeconds(60));
            if (pdf != null) {
                Path target = resultDir.resolve("cd.pdf");
                Files.move(pdf, target, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("PDF сохранён: " + target.toAbsolutePath());
            } else {
                System.out.println("PDF не найден в папке result за отведённое время.");
            }

        } finally {
            driver.quit();
        }
    }

    private static List<String> readDataFile(Path p) throws IOException {
        if (!Files.exists(p)) {
            System.err.println("Файл данных не найден: " + p.toAbsolutePath());
            return Collections.emptyList();
        }
        List<String> raw = Files.readAllLines(p);
        List<String> out = new ArrayList<>();
        for (String l : raw) if (!l.trim().isEmpty()) out.add(l.trim());
        return out;
    }

    private static void safeSendKeys(WebDriver driver, By[] locators, String text) {
        for (By b : locators) {
            try {
                WebElement el = driver.findElement(b);
                el.clear();
                el.sendKeys(text);
                return;
            } catch (Exception ignored) {}
        }
    }

    private static void safeClick(WebDriver driver, By[] locators) {
        for (By b : locators) {
            try {
                WebElement el = driver.findElement(b);
                el.click();
                return;
            } catch (Exception ignored) {}
        }
    }

    private static boolean safeSubmit(WebDriver driver, By[] locators) {
        for (By b : locators) {
            try {
                WebElement el = driver.findElement(b);
                el.submit();
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    private static Path waitForPdf(Path dir, Duration timeout) throws InterruptedException {
        Instant start = Instant.now();
        while (Duration.between(start, Instant.now()).compareTo(timeout) < 0) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.pdf")) {
                for (Path p : ds) return p;
            } catch (IOException ignored) {}
            Thread.sleep(1000);
        }
        return null;
    }
}
