package se.scooterrental.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Hanterar applikationskonfiguration.
 * UPPDATERAD: Stöd för Dark Mode och Admin Password.
 */
public class ConfigHandler {

    private static final String CONFIG_DIR = "config";
    private static final String CONFIG_FILENAME = "config.json";
    private static final String CONFIG_PATH = CONFIG_DIR + File.separator + CONFIG_FILENAME;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Cachade värden
    private static String cachedAdminPassword = "admin";
    private static boolean cachedDarkMode = false; // Standard: Ljust läge

    public static boolean verifyAdminPassword(String inputPassword) {
        ensureConfigLoaded();
        return cachedAdminPassword != null && cachedAdminPassword.equals(inputPassword);
    }

    public static boolean setAdminPassword(String newPassword) {
        if (newPassword == null || newPassword.trim().isEmpty()) return false;
        ensureConfigLoaded();
        cachedAdminPassword = newPassword;
        return saveConfig();
    }

    // --- NYA METODER FÖR DARK MODE ---

    public static boolean isDarkMode() {
        ensureConfigLoaded();
        return cachedDarkMode;
    }

    public static void setDarkMode(boolean enabled) {
        ensureConfigLoaded();
        if (cachedDarkMode != enabled) {
            cachedDarkMode = enabled;
            saveConfig();
        }
    }

    // ---------------------------------

    private static void ensureConfigLoaded() {
        // Enkel kontroll: Vi antar att om vi har ett lösenord är config laddad.
        // För mer robusthet kan vi ha en boolean isLoaded.
        // Här kör vi reload om vi bara har defaults, eller litar på statiska variabler.
        // För enkelhetens skull i detta scope: Vi laddar alltid om filen inte lästs (vilket vi gör vid start).
        // Men för att undvika onödig I/O varje anrop kan vi använda en flagga.
        // Här gör vi det enkelt: Läs filen om adminPassword är default "admin" (riskabelt om man bytt till "admin")
        // eller använd en specifik init-metod.
        // Bäst här: Försök läsa filen.

        File file = new File(CONFIG_PATH);
        if (!file.exists()) {
            createDefaultConfig();
            return;
        }

        // Vi läser bara in om vi inte gjort det tidigare.
        // I en riktig app skulle vi ha en "initialized" flagga.
        // Här litar vi på att cachedAdminPassword är satt vid start.
        // Men för att `isDarkMode` ska funka direkt läser vi filen nu.

        try (FileReader reader = new FileReader(CONFIG_PATH)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);

            if (json != null) {
                if (json.has("adminPassword")) {
                    cachedAdminPassword = json.get("adminPassword").getAsString();
                }
                if (json.has("darkMode")) {
                    cachedDarkMode = json.get("darkMode").getAsBoolean();
                }
            }
        } catch (IOException e) {
            System.err.println("FEL: Kunde inte läsa config: " + e.getMessage());
        }
    }

    private static void createDefaultConfig() {
        try {
            Path dirPath = Paths.get(CONFIG_DIR);
            if (!Files.exists(dirPath)) Files.createDirectories(dirPath);

            try (FileWriter writer = new FileWriter(CONFIG_PATH)) {
                JsonObject json = new JsonObject();
                json.addProperty("adminPassword", cachedAdminPassword);
                json.addProperty("darkMode", cachedDarkMode);

                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            System.err.println("FEL: Kunde inte skapa config: " + e.getMessage());
        }
    }

    private static boolean saveConfig() {
        try {
            Path dirPath = Paths.get(CONFIG_DIR);
            if (!Files.exists(dirPath)) Files.createDirectories(dirPath);

            try (FileWriter writer = new FileWriter(CONFIG_PATH)) {
                JsonObject json = new JsonObject();
                json.addProperty("adminPassword", cachedAdminPassword);
                json.addProperty("darkMode", cachedDarkMode);

                GSON.toJson(json, writer);
                return true;
            }
        } catch (IOException e) {
            System.err.println("FEL: Kunde inte spara config: " + e.getMessage());
            return false;
        }
    }
}