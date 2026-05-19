package com.mycompany.app;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

final class SeleniumSupport {
    private SeleniumSupport() {
    }

    static ChromeDriver createChromeDriver() {
        String driverPath = System.getenv("CHROME_DRIVER_PATH");
        if (driverPath != null && !driverPath.trim().isEmpty()) {
            System.setProperty("webdriver.chrome.driver", driverPath.trim());
        }

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        if (Boolean.getBoolean("selenium.headless") || "true".equalsIgnoreCase(System.getenv("CI"))) {
            options.addArguments("--headless=new");
        }
        return new ChromeDriver(options);
    }
}
