import org.json.simple.*;
import org.json.simple.parser.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import java.nio.file.*;

public class App {
    public static void main(String[] args) throws Exception {
        System.setProperty("webdriver.chrome.driver", "chromedriver");
        
        JSONObject data = (JSONObject) new JSONParser().parse(
            Files.readString(Paths.get("data/data.txt")));
        
        WebDriver driver = new ChromeDriver();
        try {
            driver.get("http://www.papercdcase.com/index.php");
            
            // Заполнение формы
            driver.findElement(By.name("artist")).sendKeys((String)data.get("artist"));
            driver.findElement(By.name("title")).sendKeys((String)data.get("title"));
            
            JSONArray tracks = (JSONArray)data.get("tracks");
            for (int i = 0; i < tracks.size(); i++) {
                driver.findElement(By.name("track" + (i+1)))
                    .sendKeys((String)tracks.get(i));
            }
            
            // Выбор опций
            driver.findElement(By.cssSelector("input[name='case'][value='jewel']")).click();
            driver.findElement(By.cssSelector("input[name='paper'][value='a4']")).click();
            
            // Генерация PDF
            driver.findElement(By.cssSelector("input[type='submit']")).click();
            
            Thread.sleep(5000); // Ожидание генерации
            
        } finally {
            driver.quit();
        }
    }
}