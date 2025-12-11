package se.scooterrental.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Representerar en uthyrningstransaktion.
 * Uppdaterad med robust tids-parsing för att fixa "0 kr"-felet på gamla data.
 */
public class Rental {
    private String id;
    private String memberId;
    private String itemId;
    private PricePolicy pricePolicy;
    private String startTime;
    private String endTime;
    private boolean isActive;
    private double totalCost;

    // Formatterare med sekunder (Standard för nya)
    private static final DateTimeFormatter FORMATTER_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // Formatterare utan sekunder (Fallback för gamla data)
    private static final DateTimeFormatter FORMATTER_MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public Rental(String id, String memberId, String itemId, PricePolicy pricePolicy) {
        this.id = id;
        this.memberId = memberId;
        this.itemId = itemId;
        this.pricePolicy = pricePolicy;
        this.startTime = LocalDateTime.now().format(FORMATTER_SECONDS);
        this.isActive = true;
        this.totalCost = 0.0;
    }

    public String getRentalId() { return id; }
    public String getId() { return id; }
    public String getMemberId() { return memberId; }
    public String getItemId() { return itemId; }
    public PricePolicy getPricePolicy() { return pricePolicy; }
    public String getStartTime() { return startTime; }
    public boolean isActive() { return isActive; }
    public double getTotalCost() { return totalCost; }

    /**
     * Beräknar kostnaden baserat på exakta sekunder (Taxameter-stil).
     */
    public double getCurrentCost(double basePricePerHour) {
        if (!isActive && totalCost > 0) {
            return totalCost;
        }

        try {
            // Använder smart parsing som klarar både gamla och nya tider
            LocalDateTime start = parseDateTime(startTime);
            LocalDateTime end = isActive ? LocalDateTime.now() : parseDateTime(endTime);

            // Räkna exakta sekunder
            long seconds = Duration.between(start, end).toSeconds();

            if (seconds < 0) seconds = 0;

            // Konvertera sekunder till exakta timmar (t.ex. 0.00027 timmar)
            double hours = seconds / 3600.0;

            if (pricePolicy != null) {
                return pricePolicy.calculatePrice(basePricePerHour, hours);
            }

            // Fallback om policy saknas
            return basePricePerHour * hours;

        } catch (Exception e) {
            // Om detta sker returneras 0, vilket var problemet förut.
            // Nu med parseDateTime borde detta inte ske.
            return 0.0;
        }
    }

    /**
     * Hjälpmetod: Försöker läsa sekunder, annars faller tillbaka på minuter.
     */
    private LocalDateTime parseDateTime(String timeStr) {
        if (timeStr == null) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(timeStr, FORMATTER_SECONDS);
        } catch (DateTimeParseException e) {
            // Fallback för gamla data som saknar sekunder (t.ex. "2025-12-03 17:16")
            return LocalDateTime.parse(timeStr, FORMATTER_MINUTES);
        }
    }

    public void endRental(double finalCost) {
        this.isActive = false;
        this.endTime = LocalDateTime.now().format(FORMATTER_SECONDS);
        this.totalCost = finalCost;
    }

    public void setTotalCost(double cost) {
        this.totalCost = cost;
    }
}