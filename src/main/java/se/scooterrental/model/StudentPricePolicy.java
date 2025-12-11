package se.scooterrental.model;

/**
 * Konkret prisstrategi: Student Policy med rabatt.
 */
public class StudentPricePolicy implements PricePolicy {
    private static final double DISCOUNT_RATE = 0.8; // 20% rabatt

    @Override
    public double calculatePrice(double basePrice, double hours) {
        // Student får 20% rabatt på totalpriset.
        return basePrice * hours * DISCOUNT_RATE;
    }

    @Override
    public String getPolicyName() {
        return "Student (20% rabatt)";
    }
}