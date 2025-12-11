package se.scooterrental.model;

/**
 * Interface för att definiera prisstrategier (PricePolicy Strategy Pattern).
 * Uppfyller kravet: PricePolicy (interface) + konkreta strategier.
 */

public interface PricePolicy {

    /**
     * Beräknar priset för en uthyrning.
     * @param basePrice Baspriset (t.ex. Item.currentRentalPrice).
     * @param hours Antal timmar uthyrningen varade.
     * @return Det totala priset.
     */
    double calculatePrice(double basePrice, double hours);

    /**
     * Returnerar namnet på prispolicyn.
     * @return Namnet som String.
     */
    String getPolicyName();
}