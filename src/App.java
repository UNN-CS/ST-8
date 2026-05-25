import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        String projectPath = System.getProperty("user.dir");
        String downloadPath = projectPath + File.separator + "result";
        String dataPath = projectPath + File.separator + "data" + File.separator + "README.md";

        new File(downloadPath).mkdirs();

        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);
        options.addArguments("--remote-allow-origins=*");

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadPath);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.manage().window().maximize();
            System.out.println("Открываю сайт...");
            driver.get("http://www.papercdcase.com");

            List<String> lines = Files.readAllLines(Paths.get(dataPath));
            Thread.sleep(5000);

            List<WebElement> textFields = driver.findElements(By.cssSelector("input[type='text']"));
            System.out.println("Найдено текстовых полей: " + textFields.size());

            if (textFields.size() > 0) {
                System.out.println("Заполняю Artist и Title...");
                textFields.get(0).sendKeys(lines.get(0));
                textFields.get(1).sendKeys(lines.get(1));

                System.out.println("Заполняю треки...");
                for (int i = 2; i < lines.size() && i < 20; i++) {
                    int fieldIndex = i;
                    if (fieldIndex < textFields.size()) {
                        textFields.get(fieldIndex).sendKeys(lines.get(i));
                    }
                }
            }

            System.out.println("Выбираю тип обложки...");
            try {
                WebElement jewelRadio = driver.findElement(By.cssSelector("input[value='jewel']"));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", jewelRadio);
            } catch (Exception e) {
                System.out.println("Не удалось кликнуть на Jewel Case, продолжаем...");
            }

            System.out.println("Отправляю форму...");
            try {
                textFields.get(0).submit();
                System.out.println("Форма отправлена через метод submit()");
            } catch (Exception e) {
                System.out.println("Метод submit() не сработал, пробую найти кнопку через JavaScript...");
                ((JavascriptExecutor) driver).executeScript(
                        "document.forms[0].submit();"
                );
            }

            System.out.println("Ожидание скачивания (15 секунд)...");
            Thread.sleep(15000);

            File dir = new File(downloadPath);
            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));

            if (files != null && files.length > 0) {
                File downloaded = files[0];
                File target = new File(downloadPath + File.separator + "cd.pdf");
                if (target.exists()) target.delete();

                if (downloaded.renameTo(target)) {
                    System.out.println("УСПЕХ! Файл готов: " + target.getAbsolutePath());
                } else {
                    System.out.println("Файл скачан, но не удалось переименовать. Ищите его в папке result.");
                }
            } else {
                System.err.println("ОШИБКА: PDF-файл не появился в папке " + downloadPath);
                System.out.println("Посмотрите в открытое окно браузера: нет ли там ошибок на самом сайте?");
            }

        } catch (Exception e) {
            System.err.println("ОШИБКА: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}