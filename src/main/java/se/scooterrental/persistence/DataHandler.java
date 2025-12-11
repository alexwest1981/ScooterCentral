package se.scooterrental.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import se.scooterrental.model.Member;
import se.scooterrental.model.Item;
import se.scooterrental.model.Scooter;
import se.scooterrental.model.Sled;
import se.scooterrental.model.Rental;
import se.scooterrental.model.PricePolicy;
import se.scooterrental.model.StandardPricePolicy;
import se.scooterrental.model.StudentPricePolicy;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Hanterar inläsning och sparning av applikationens data (medlemmar, items och uthyrningar)
 * till JSON-filer. Använder Gson-biblioteket.
 * FIXAT: Använder manuella adaptrar för polymorfism (Item & PricePolicy) och java.time.LocalDateTime.
 */
public class DataHandler {

    // Använd standard ISO-format för datum/tid
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // --- Adapter för LocalDateTime ---
    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.format(DATE_TIME_FORMATTER));
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return LocalDateTime.parse(json.getAsString(), DATE_TIME_FORMATTER);
        }
    }

    // --- Manuell Adapter för Polymorfism (Item) ---
    private static class ItemTypeAdapter implements JsonDeserializer<Item> {
        private static final String CLASS_TYPE = "itemType";

        @Override
        public Item deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            JsonElement typeElement = jsonObject.get(CLASS_TYPE);

            if (typeElement == null) {
                // Om filen saknar typinformation
                throw new JsonParseException("Saknar 'itemType' fält för polymorf deserialisering.");
            }

            String type = typeElement.getAsString();

            // Baserat på typ-strängen, använd rätt subklass för deserialisering
            switch (type) {
                case "Scooter":
                    return context.deserialize(jsonObject, Scooter.class);
                case "Sled":
                    return context.deserialize(jsonObject, Sled.class);
                default:
                    throw new JsonParseException("Okänd Item-typ: " + type);
            }
        }
    }

    // --- Manuell Adapter för Polymorfism (PricePolicy) - FIXAT! ---
    private static class PricePolicyTypeAdapter implements JsonSerializer<PricePolicy>, JsonDeserializer<PricePolicy> {
        private static final String CLASS_TYPE = "policyType";

        @Override
        public JsonElement serialize(PricePolicy src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject result = new JsonObject();
            // Avgör vilken typ av policy det är och spara som en sträng
            result.addProperty(CLASS_TYPE, src instanceof StandardPricePolicy ? "Standard" : "Student");
            return result;
        }

        @Override
        public PricePolicy deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            JsonElement typeElement = jsonObject.get(CLASS_TYPE);

            // Om typinformation saknas (gamla filer eller felaktig data), kör fallback till Standard
            if (typeElement == null) {
                return new StandardPricePolicy();
            }

            String type = typeElement.getAsString();

            switch (type) {
                case "Student":
                    return new StudentPricePolicy();
                case "Standard":
                default:
                    return new StandardPricePolicy();
            }
        }
    }

    // Konfigurerar GSON med alla manuella adaptrar
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Item.class, new ItemTypeAdapter())
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .registerTypeAdapter(PricePolicy.class, new PricePolicyTypeAdapter()) // FIX: Registrerad adapter för PricePolicy
            .create();

    private static final String MEMBER_FILE = "members.json";
    private static final String ITEM_FILE = "items.json";
    private static final String RENTAL_FILE = "rentals.json";

    // --- Läs-metoder ---

    /**
     * Läser in lista av medlemmar från JSON-fil.
     * @return Lista av Member-objekt. Tom lista vid fel/ingen fil.
     */
    public static List<Member> loadMembers() {
        return loadList(MEMBER_FILE, new TypeToken<List<Member>>() {}.getType());
    }

    /**
     * Läser in lista av Items (Scooters/Sleds) från JSON-fil.
     * @return Lista av Item-objekt. Tom lista vid fel/ingen fil.
     */
    public static List<Item> loadItems() {
        return loadList(ITEM_FILE, new TypeToken<List<Item>>() {}.getType());
    }

    /**
     * Läser in lista av uthyrningar från JSON-fil.
     * @return Lista av Rental-objekt. Tom lista vid fel/ingen fil.
     */
    public static List<Rental> loadRentals() {
        return loadList(RENTAL_FILE, new TypeToken<List<Rental>>() {}.getType());
    }

    /**
     * Generisk metod för att läsa in en lista från en JSON-fil.
     * @param filename Filnamnet.
     * @param type Typinformation för Gson.
     * @param <T> Typen av lista.
     * @return Den inlästa listan.
     */
    private static <T> List<T> loadList(String filename, Type type) {
        Path path = Path.of(filename);
        if (!Files.exists(path)) {
            System.out.println("INFO: Filen " + filename + " hittades inte. Startar med tom lista.");
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(filename)) {
            List<T> list = GSON.fromJson(reader, type);
            return list != null ? list : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("FEL: Kunde inte läsa från filen " + filename + ". " + e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("FEL: Ett okänt fel inträffade vid JSON-inläsning av " + filename + ". " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // --- Spara-metoder ---

    /**
     * Sparar lista av medlemmar till JSON-fil.
     * @param members Listan med Member-objekt att spara.
     * @return true om sparning lyckades, annars false.
     */
    public static boolean saveMembers(List<Member> members) {
        return saveObject(members, MEMBER_FILE);
    }

    /**
     * Sparar lista av Items (Scooters/Sleds) till JSON-fil.
     * @param items Listan med Item-objekt att spara.
     * @return true om sparning lyckades, annars false.
     */
    public static boolean saveItems(List<Item> items) {
        return saveObject(items, ITEM_FILE);
    }

    /**
     * Sparar lista av uthyrningar till JSON-fil.
     * @param rentals Listan med Rental-objekt att spara.
     * @return true om sparning lyckades, annars false.
     */
    public static boolean saveRentals(List<Rental> rentals) {
        return saveObject(rentals, RENTAL_FILE);
    }

    /**
     * Generisk metod för att spara ett objekt till en JSON-fil.
     * @param object Objektet att spara.
     * @param filename Filnamnet.
     * @param <T> Typen av objektet.
     * @return true om sparning lyckades, annars false.
     */
    private static <T> boolean saveObject(T object, String filename) {
        try (FileWriter writer = new FileWriter(filename)) {

            // Anpassad serialisering: Tvinga in "itemType" fältet för Items
            if (object instanceof List) {
                List<?> list = (List<?>) object;
                if (!list.isEmpty() && list.get(0) instanceof Item) {
                    List<JsonElement> jsonItems = new ArrayList<>();
                    for (Object itemObj : list) {
                        JsonObject jsonItem = GSON.toJsonTree(itemObj).getAsJsonObject();
                        if (itemObj instanceof Scooter) {
                            jsonItem.addProperty("itemType", "Scooter");
                        } else if (itemObj instanceof Sled) {
                            jsonItem.addProperty("itemType", "Sled");
                        }
                        jsonItems.add(jsonItem);
                    }
                    GSON.toJson(jsonItems, writer);
                    return true;
                }
            }

            // Standard serialisering för Member och Rental (GSON hanterar PricePolicy via adaptern)
            GSON.toJson(object, writer);
            return true;
        } catch (IOException e) {
            System.err.println("FEL: Kunde inte spara till filen " + filename + ". " + e.getMessage());
            return false; // Returnera false vid fel
        }
    }
}