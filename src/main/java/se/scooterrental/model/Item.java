package se.scooterrental.model;

/**
 * Abstrakt basklass för uthyrningsutrustning (Item).
 * Uppdaterad med statistikfält.
 */
public abstract class Item {
    private final String itemId;
    private String name;
    private boolean isAvailable;
    private double currentRentalPrice;
    private int rentalCount; // NYTT: Statistik för "Mest populära"

    public Item(String itemId, String name, double currentRentalPrice) {
        this.itemId = itemId;
        this.name = name;
        this.isAvailable = true;
        this.currentRentalPrice = currentRentalPrice;
        this.rentalCount = 0;
    }

    // Getters
    public String getItemId() { return itemId; }
    public String getName() { return name; }
    public boolean isAvailable() { return isAvailable; }
    public double getCurrentRentalPrice() { return currentRentalPrice; }
    public int getRentalCount() { return rentalCount; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setAvailable(boolean isAvailable) { this.isAvailable = isAvailable; } // FIXAT: Enhetligt namn
    public void setIsAvailable(boolean isAvailable) { this.isAvailable = isAvailable; } // Behåll för bakåtkompatibilitet om json kräver
    public void setCurrentRentalPrice(double currentRentalPrice) { this.currentRentalPrice = currentRentalPrice; }

    /**
     * Ökar räknaren för hur många gånger denna hyrts ut.
     */
    public void incrementRentalCount() {
        this.rentalCount++;
    }

    public abstract String getUniqueInfo();

    @Override
    public String toString() {
        return String.format("ID: %s, Namn: %s, Pris: %.2f kr/h", itemId, name, currentRentalPrice);
    }
}