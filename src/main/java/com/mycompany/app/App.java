package com.mycompany.app;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class App {

	private static String artist = "";
	private static String title = "";
	private static List<String> tracks = new ArrayList<>();

	public static void main(String[] args) {

		System.setProperty(
				"webdriver.chrome.driver",
				"C:\\Users\\shken\\ST-8\\chromedriver-win64\\chromedriver.exe");

		loadDataFromFile("data/data.txt");

		ChromeOptions options = new ChromeOptions();
		options.addArguments("--ignore-certificate-errors");
		options.addArguments("--allow-insecure-localhost");

		String downloadPath = Paths.get("result").toAbsolutePath().toString();
		java.util.HashMap<String, Object> prefs = new java.util.HashMap<>();
		prefs.put("download.default_directory", downloadPath);
		prefs.put("download.prompt_for_download", false);
		prefs.put("download.directory_upgrade", true);
		prefs.put("plugins.always_open_pdf_externally", true);
		options.setExperimentalOption("prefs", prefs);

		WebDriver webDriver = new ChromeDriver(options);

		try {
			webDriver.get("http://www.papercdcase.com");
			Thread.sleep(3000);

			// Artist
			WebElement artistField = webDriver.findElement(
					By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input"));
			artistField.clear();
			artistField.sendKeys(artist);

			// Title
			WebElement titleField = webDriver.findElement(
					By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input"));
			titleField.clear();
			titleField.sendKeys(title);

			// Tracks
			String column1Base = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[ROW_NUMBER]/td[2]/input";
			String column2Base = "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[ROW_NUMBER]/td[2]/input";

			int trackIndex = 0;

			for (int row = 1; row <= 8 && trackIndex < tracks.size(); row++) {
				String trackXPath = column1Base.replace("ROW_NUMBER", String.valueOf(row));
				WebElement trackField = webDriver.findElement(By.xpath(trackXPath));
				trackField.clear();
				trackField.sendKeys(tracks.get(trackIndex));
				trackIndex++;
			}

			for (int row = 1; row <= 8 && trackIndex < tracks.size(); row++) {
				String trackXPath = column2Base.replace("ROW_NUMBER", String.valueOf(row));
				WebElement trackField = webDriver.findElement(By.xpath(trackXPath));
				trackField.clear();
				trackField.sendKeys(tracks.get(trackIndex));
				trackIndex++;
			}

			// Type - Jewel Case
			WebElement typeCell = webDriver.findElement(
					By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[4]/td[2]"));
			List<WebElement> radioButtons = typeCell.findElements(By.xpath(".//input[@type='radio']"));
			for (WebElement radio : radioButtons) {
				if ("jewel".equals(radio.getAttribute("value"))) {
					radio.click();
					break;
				}
			}

			// Paper - A4
			WebElement paperCell = webDriver.findElement(
					By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[5]/td[2]"));
			List<WebElement> paperRadios = paperCell.findElements(By.xpath(".//input[@type='radio']"));
			for (WebElement radio : paperRadios) {
				if ("a4".equals(radio.getAttribute("value"))) {
					radio.click();
					break;
				}
			}

			// Generation button
			WebElement generateBtn = webDriver.findElement(
					By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input"));
			generateBtn.click();

			Thread.sleep(8000);

			Path resultDir = Paths.get("result");
			try (Stream<Path> stream = Files.list(resultDir)) {
				Optional<Path> pdfFile = stream.filter(p -> p.toString().endsWith(".pdf")).findFirst();
				if (pdfFile.isPresent()) {
					Path source = pdfFile.get();
					Path target = resultDir.resolve("cd.pdf");
					if (!source.equals(target)) {
						Files.move(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}

		} catch (Exception e) {
			System.out.println("Error");
			e.printStackTrace();
		} finally {
			webDriver.quit();
		}
	}

	private static void loadDataFromFile(String filePath) {
		try {
			List<String> lines = Files.readAllLines(Paths.get(filePath));
			boolean readingTracks = false;

			for (String line : lines) {
				if (line.startsWith("Artist:")) {
					artist = line.substring(8).trim();
				} else if (line.startsWith("Title:")) {
					title = line.substring(7).trim();
				} else if (line.startsWith("Tracks:")) {
					readingTracks = true;
				} else if (readingTracks && line.trim().matches("^\\d+\\..*")) {
					String track = line.replaceFirst("^\\d+\\.\\s*", "").trim();
					tracks.add(track);
				}
			}
		} catch (Exception e) {
			System.out.println("Ошибка загрузки data.txt");
			e.printStackTrace();
		}
	}
}
