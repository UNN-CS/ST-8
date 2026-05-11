package ru.unn.cs.st8;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Page object for the basic cover form (localizers match common XPath: {@code //input[@name='…']}). */
public final class PapercasePage {

  /** XPath analogue: {@code //input[@name='artist']}. */
  public static final By ARTIST = By.name("artist");

  /** XPath analogue: {@code //input[@name='title']}. */
  public static final By TITLE = By.name("title");

  /** XPath analogue: {@code //input[@name='template'][@value='jewel']} (Jewel case). */
  public static final By TYPE_JEWEL_CASE = By.cssSelector("input[name='template'][value='jewel']");

  /** XPath analogue: {@code //input[@name='size'][@value='a4']} (A4 paper). */
  public static final By PAPER_A4 = By.cssSelector("input[name='size'][value='a4']");

  /**
   * Image submit control; XPath analogue: {@code //input[@name='submit'][@type='image']}.
   */
  public static final By SUBMIT = By.cssSelector("input[name='submit'][type='image']");

  private static final By FORCE_SAVE_AS = By.cssSelector("input[name='force_saveas'][value='yes']");

  private final WebDriver driver;
  private final WebDriverWait wait;

  public PapercasePage(WebDriver driver, Duration waitTimeout) {
    this.driver = driver;
    this.wait = new WebDriverWait(driver, waitTimeout);
  }

  private static By trackField(int trackNumberOneBased) {
    return By.name("track" + trackNumberOneBased);
  }

  private static void type(WebElement element, String value) {
    element.clear();
    element.sendKeys(value);
  }

  /**
   * Fills artist, title, optional {@code track1..16}, selects Jewel case + A4, enables “Force Save-as”.
   */
  public void fillCoverData(CoverData data) {
    wait.until(ExpectedConditions.visibilityOfElementLocated(ARTIST));
    type(driver.findElement(ARTIST), data.artist());
    type(driver.findElement(TITLE), data.title());

    for (int i = 1; i <= CoverData.MAX_TRACKS_FIELDS; i++) {
      String value = i <= data.tracks().size() ? data.tracks().get(i - 1) : "";
      type(driver.findElement(trackField(i)), value);
    }

    driver.findElement(TYPE_JEWEL_CASE).click();
    driver.findElement(PAPER_A4).click();

    WebElement force = wait.until(ExpectedConditions.elementToBeClickable(FORCE_SAVE_AS));
    if (!force.isSelected()) {
      force.click();
    }
  }

  /**
   * Submits the enclosing form via {@link WebElement#submit()} on the image control (assignment pattern).
   * Navigation to a PDF may not complete cleanly in Chromium; callers may additionally fetch PDF via HTTP.
   */
  public void submitGeneratingButton() {
    WebElement submitBtn = wait.until(ExpectedConditions.elementToBeClickable(SUBMIT));
    submitBtn.submit();
  }
}
