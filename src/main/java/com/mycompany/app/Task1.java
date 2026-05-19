package com.mycompany.app;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openqa.selenium.WebDriver;

final class Task1 {
    private static final SecureRandom RANDOM = new SecureRandom();

    private Task1() {
    }

    static String generatePassword(WebDriver driver) {
        driver.get("https://www.calculator.net/password-generator.html");

        PasswordSettings settings = defaultSettings();
        return generatePassword(settings);
    }

    private static PasswordSettings defaultSettings() {
        PasswordSettings settings = new PasswordSettings();
        settings.length = 10;
        settings.lower = true;
        settings.upper = true;
        settings.number = true;
        settings.symbol = true;
        settings.excludeAmbiguous = true;
        settings.excludeBrackets = true;
        settings.unique = false;
        return settings;
    }

    private static String generatePassword(PasswordSettings settings) {
        List<Character> base = new ArrayList<>();
        List<Character> mandatory = new ArrayList<>();

        if (settings.lower) {
            String chars = settings.excludeAmbiguous ? "abcdefghjkmnpqrstuvwxyz" : "abcdefghijklmnopqrstuvwxyz";
            addChars(base, chars);
            mandatory.add(randomChar(chars));
        }
        if (settings.upper) {
            String chars = settings.excludeAmbiguous ? "ABCDEFGHJKMNPQRSTUVWXYZ" : "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            addChars(base, chars);
            mandatory.add(randomChar(chars));
        }
        if (settings.number) {
            String chars = settings.excludeAmbiguous ? "123456789" : "0123456789";
            addChars(base, chars);
            mandatory.add(randomChar(chars));
        }
        if (settings.symbol) {
            String chars;
            if (settings.excludeBrackets) {
                chars = settings.excludeAmbiguous ? "!#$%&*+/=?@\\^~" : "!\"#$%&'*+,-./:;=?@\\^_`|~";
            } else {
                chars = settings.excludeAmbiguous ? "!#$%&()*+/<=>?@[\\]^{}~" : "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";
            }
            addChars(base, chars);
            mandatory.add(randomChar(chars));
        }

        if (base.isEmpty()) {
            return "Password generation page opened";
        }

        if (settings.unique && settings.length > base.size()) {
            settings.length = base.size();
        }

        StringBuilder password = new StringBuilder();
        for (char c : mandatory) {
            password.append(c);
        }

        while (password.length() < settings.length) {
            password.append(randomChar(listToString(base)));
        }

        List<Character> shuffled = new ArrayList<>();
        for (int i = 0; i < password.length(); i++) {
            shuffled.add(password.charAt(i));
        }
        Collections.shuffle(shuffled, RANDOM);

        StringBuilder finalPassword = new StringBuilder();
        for (char c : shuffled) {
            finalPassword.append(c);
        }
        return finalPassword.toString();
    }

    private static void addChars(List<Character> list, String chars) {
        for (int i = 0; i < chars.length(); i++) {
            list.add(chars.charAt(i));
        }
    }

    private static char randomChar(String chars) {
        return chars.charAt(RANDOM.nextInt(chars.length()));
    }

    private static String listToString(List<Character> list) {
        StringBuilder builder = new StringBuilder(list.size());
        for (Character c : list) {
            builder.append(c.charValue());
        }
        return builder.toString();
    }

    private static final class PasswordSettings {
        int length;
        boolean lower;
        boolean upper;
        boolean number;
        boolean symbol;
        boolean excludeAmbiguous;
        boolean excludeBrackets;
        boolean unique;
    }
}
