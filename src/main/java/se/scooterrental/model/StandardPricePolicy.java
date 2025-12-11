package se.scooterrental.model;

/**
 * Konkret prisstrategi: Standard Policy.
 */
public class StandardPricePolicy implements PricePolicy {
    private static final double HOURLY_RATE_MULTIPLIER = 1.0;

    @Override
    public double calculatePrice(double basePrice, double hours) {
        // Standardpriset är baspriset multiplicerat med antalet timmar.
        return basePrice * hours * HOURLY_RATE_MULTIPLIER;
    }

    @Override
    public String getPolicyName() {
        return "Standard";
    }
}