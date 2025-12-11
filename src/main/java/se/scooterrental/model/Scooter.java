package se.scooterrental.model;

/**
 * Konkret subklass till Item, representerar en uthyrningsskoter (Snöskoter).
 * Innehåller attribut relevanta för snöskotrar som motorstorlek och startsystem.
 */
public class Scooter extends Item {
    private int engineDisplacement; // Motorstorlek i cc (kubik), t.ex. 600, 850
    private String licensePlate;
    private boolean hasElectricStart; // True om den har elstart, false om kickstart

    /**
     * Konstruktor för Scooter.
     * @param itemId Unikt ID.
     * @param name Namn/modell.
     * @param currentRentalPrice Aktuellt pris per timme.
     * @param licensePlate Registreringsskylt.
     * @param engineDisplacement Motorstorlek i kubik (cc).
     * @param hasElectricStart Om skotern har elstart.
     */
    public Scooter(String itemId, String name, double currentRentalPrice, String licensePlate, int engineDisplacement, boolean hasElectricStart) {
        super(itemId, name, currentRentalPrice);
        this.licensePlate = licensePlate;
        setEngineDisplacement(engineDisplacement); // Använd setter för validering
        this.hasElectricStart = hasElectricStart;
    }

    // --- Getters ---

    public int getEngineDisplacement() {
        return engineDisplacement;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public boolean hasElectricStart() {
        return hasElectricStart;
    }

    // --- Setters ---

    public void setEngineDisplacement(int engineDisplacement) {
        if (engineDisplacement <= 0) {
            // Robust felhantering (del av VG-krav)
            throw new IllegalArgumentException("Motorstorlek måste vara större än 0 cc.");
        }
        this.engineDisplacement = engineDisplacement;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public void setHasElectricStart(boolean hasElectricStart) {
        this.hasElectricStart = hasElectricStart;
    }

    @Override
    public String getUniqueInfo() {
        String startType = hasElectricStart ? "Elstart" : "Kickstart";
        return String.format("Skylt: %s, Motor: %d cc, Start: %s", licensePlate, engineDisplacement, startType);
    }

    @Override
    public String toString() {
        return String.format("%s, %s", super.toString(), getUniqueInfo());
    }
}