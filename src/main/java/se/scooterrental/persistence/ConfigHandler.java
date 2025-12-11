package se.scooterrental.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Hanterar applikationskonfiguration, exempelvis admin-lösenord.
 * Läser från config.json.
 * * UPPDATERAD: Använder JsonObject direkt för att undvika problem med Java Module System
 * och InaccessibleObjectException vid reflection.
 */
public class ConfigHandler {

    private static final String CONFIG_FILE = "config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static String cachedAdminPassword; // Vi sparar bara lösenordet direkt som sträng

    /**
     * Verifierar om det angivna lösenordet stämmer med konfigurationen.
     * @param inputPassword Lösenordet som användaren matat in.
     * @return true om det matchar, annars false.
     */
    public static boolean verifyAdminPassword(String inputPassword) {
        ensureConfigLoaded();
        return cachedAdminPassword != null && cachedAdminPassword.equals(inputPassword);
    }

    /**
     * Laddar konfigurationen. Om filen saknas skapas en ny med standardvärden.
     */
    private static void ensureConfigLoaded() {
        if (cachedAdminPassword != null) return; // Redan laddad

        Path path = Path.of(CONFIG_FILE);
        if (!Files.exists(path)) {
            createDefaultConfig();
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            // Vi läser in som ett generellt JsonObject istället för en specifik klass
            // Detta undviker reflection-problem med moduler.
            JsonObject json = GSON.fromJson(reader, JsonObject.class);

            if (json != null && json.has("adminPassword")) {
                cachedAdminPassword = json.get("adminPassword").getAsString();
            } else {
                // Fallback om fältet saknas i filen
                cachedAdminPassword = "admin";
            }

        } catch (IOException e) {
            System.err.println("FEL: Kunde inte läsa konfigurationsfilen: " + e.getMessage());
            cachedAdminPassword = "admin"; // Säkerhetsfallback
        }
    }

    /**
     * Skapar en standard config.json om den saknas.
     */
    private static void createDefaultConfig() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            // Skapa JSON manuellt med JsonObject
            JsonObject json = new JsonObject();
            json.addProperty("adminPassword", "admin");

            GSON.toJson(json, writer);
            System.out.println("INFO: Skapade ny config.json med standardlösenord.");

            // Sätt cachen direkt också
            cachedAdminPassword = "admin";
        } catch (IOException e) {
            System.err.println("FEL: Kunde inte skapa standardkonfiguration: " + e.getMessage());
        }
    }
}