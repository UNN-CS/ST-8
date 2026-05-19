package com.mycompany.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public final class Task2 {
    private static final Pattern IP_PATTERN = Pattern.compile("\"ip\"\\s*:\\s*\"([^\"]+)\"");

    private Task2() {
    }

    public static String run() {
        try {
            String json = downloadWithBrowser();
            String ip = extractIp(json);
            System.out.println(ip);
            return ip;
        } catch (Exception e) {
            System.out.println("Error");
            System.out.println(e.toString());
            return "";
        }
    }

    private static String downloadWithBrowser() throws Exception {
        WebDriver driver = null;
        try {
            driver = SeleniumSupport.createChromeDriver();
            driver.get("https://api.ipify.org/?format=json");
            java.util.List<WebElement> pres = driver.findElements(By.tagName("pre"));
            WebElement pre = pres.isEmpty() ? null : pres.get(0);
            if (pre != null && pre.getText() != null && !pre.getText().trim().isEmpty()) {
                return pre.getText().trim();
            }
            WebElement body = driver.findElement(By.tagName("body"));
            String text = body.getText().trim();
            if (!text.isEmpty()) {
                return text;
            }
        } catch (Exception ignored) {
            return download("https://api.ipify.org/?format=json");
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
        return download("https://api.ipify.org/?format=json");
    }

    private static String download(String link) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(link).openConnection();
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    static String extractIp(String json) {
        Matcher matcher = IP_PATTERN.matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }
}
