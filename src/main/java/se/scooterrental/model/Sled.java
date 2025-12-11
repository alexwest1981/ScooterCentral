package se.scooterrental.model;

/**
 * Konkret subklass till Item, representerar en uthyrningssläde.
 * Lägg till unika attribut, t.ex. typ av släde eller maxvikt.
 */
public class Sled extends Item {
    private String type; // T.ex. Pulka, Kälke, Spark
    private int maxWeightKg;

    /**
     * Konstruktor för Sled.
     * FIXAT: Matchar anropet från ItemView: (id, name, price, type, maxWeightKg)
     * @param itemId Unikt ID.
     * @param name Namn/modell.
     * @param currentRentalPrice Aktuellt pris per timme.
     * @param type Typ av släde.
     * @param maxWeightKg Maximal belastningsvikt.
     */
    public Sled(String itemId, String name, double currentRentalPrice, String type, int maxWeightKg) {
        super(itemId, name, currentRentalPrice);
        this.type = type;
        // Använd setter för att validera vikten
        setMaxWeightKg(maxWeightKg);
    }

    // --- Getters ---

    public String getType() {
        return type;
    }

    public int getMaxWeightKg() {
        return maxWeightKg;
    }

    // --- Setters ---

    public void setType(String type) {
        this.type = type;
    }

    public void setMaxWeightKg(int maxWeightKg) {
        if (maxWeightKg <= 0) {
            // Robust felhantering (del av VG-krav)
            throw new IllegalArgumentException("Maxvikt måste vara större än noll.");
        }
        this.maxWeightKg = maxWeightKg;
    }

    @Override
    public String getUniqueInfo() {
        return String.format("Typ: %s, Maxvikt: %d kg", type, maxWeightKg);
    }

    @Override
    public String toString() {
        return String.format("%s, %s", super.toString(), getUniqueInfo());
    }
}