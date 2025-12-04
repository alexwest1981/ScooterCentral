package se.scooterrental.persistence;

import se.scooterrental.model.Item;
import se.scooterrental.model.Member;
import se.scooterrental.model.Rental;
import se.scooterrental.model.PricePolicy;
import se.scooterrental.model.StandardPricePolicy; // Standardpolicy
import se.scooterrental.persistence.DataHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Hanterar affärslogik relaterad till uthyrningar (Boka, Avsluta, Summeringar).
 * Behöver MemberRegistry och Inventory för att fungera.
 */
public class RentalService {
    private List<Rental> activeAndFinishedRentals;
    private final MemberRegistry memberRegistry;
    private final Inventory inventory;

    public RentalService(¨MemberRegistry memberRegistry, Inventory inventory) {
        // Läser in alla rentals vid start
        this.activeAndFinishedRentals = DataHandler.loadRentals();
        this.memberRegistry = memberRegistry;
        this.inventory = inventory;
        // Säkerställer att ID_COUNTER för Rental är uppdaterad efter inläsning
        updateRentalIdCounter();
    }

    /**
     * Uppdatera ID-räknaren i Rental-klassen till högsta befintliga ID.
     */
    private void updateRentalIdCounter() {}

    /**
     * Bokar en ny uthyrning.
     * @param memberId Medlems-ID.
     * @param itemId Item-ID.
     * @param policy Den prispolicy som ska användas.
     * @return Den nya Rental-instansen, eller Optional.empty() vid fel.
     */
    public Optional<Rental> startRental(String memberId, String itemId, PricePolicy pricePolicy) {
        Optional<Member> memberOpt = memberRegistry.findMemberById(memberId);
        Optional<Item> itemOpt = inventory.findItemById(itemId);

        if (memberOpt.isEmpty()) {
            System.err.println("FEL: Medlem med ID " + memberId + "hittades inte.");
            return Optional.empty();
        }

        if (itemOpt.isEmpty() || !itemOpt.get().isAvailable()) {
            System.err.println("FEL: Item med ID " + itemId + "hittades inte eller är inte tillgänglig.");
            return Optional.empty();
        }

        Item item = itemOpt.get();

        // Skapa uthyrningen
        Rental newRental = new Rental(memberId, itemId, policy);
        activeAndFinishedRentals.add(newRental);

        // Uppdatera inventory
        item.setIsAvailable(false);
        inventory.updateItem(item);

        // Uppdatera member history
        memberOpt.get().addRentalToHistory(String.valueOf(newRental.getRentalId()));

        return Optional.of(newRental);
    }

    /**
     * Avslutar en pågående uthyrning.
     * @param rentalId ID för uthyrningen som ska avslutas.
     * @return Optional som innehåller slutpriset, eller Optional.empty() vid fel.
     */
    public Optional<Double> endRental(long rentalId) {
        Optional<Rental> rentalOpt = activeAndFinishedRentals.stream()
                .filter(r -> r.getRentalId() == rentalId)
                .filter(r -> r.getEndTime() == null) // Måste vara en aktiv uthyrning
                .findFirst();

        if (rentalOpt.isEmpty()) {
            System.err.println("FEL: Aktiv uthyrning med ID " + rentalId + " hittades inte.");
            return Optional.empty();
        }

        Rental rental = rentalOpt.get();

        // Hämta Item för att få baspriset
        Optional<Item> itemOpt = inventory.findItemById(rental.getItemId());
        if (itemOpt.isEmpty()) {
            System.err.println("VARNING: Uthyrd Item saknas i Inventory. Kan inte beräkna pris.");
            return Optional.empty();
        }

        Item item = itemOpt.get();
        double basePrice = item.getCurrentRentalPrice();

        try {
            // 1. Avsluta uthyrningen och beräkna pris
            double finalPrice = rental.endRental(basePrice);

            // 2. Uppdatera Inventory (sätt Item till tillgänglig)
            item.setAvailable(true);
            inventory.updateItem(item);

            // 3. Data är nu uppdaterad i både Rental-listan och Inventory
            return Optional.of(finalPrice);

        } catch (IllegalStateException e) {
            System.err.println("FEL: Uthyrning kunde inte avslutas: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Returnerar en lista med alla aktiva uthyrningar.
     * @return Lista med aktiva Rental-objekt.
     */
    public List<Rental> getActiveRentals() {
        return activeAndFinishedRentals.stream()
                .filter(r -> r.getEndTime() == null)
                .collect(Collectors.toList());
    }

    /**
     * Beräknar den totala intäkten från avslutade uthyrningar (Summeringar G-krav).
     * @return Den totala intäkten som double.
     */
    public double getTotalRevenue() {
        return activeAndFinishedRentals.stream()
                .filter(r -> r.getEndTime() != null)
                .mapToDouble(Rental::getFinalPrice)
                .sum();
    }

    /**
     * Returnerar en oföränderlig vy av alla uthyrningar.
     * @return Lista med alla Rental-objekt.
     */
    public List<Rental> getAllRentals() {
        return Collections.unmodifiableList(activeAndFinishedRentals);
    }

    /**
     * Sparar uthyrningshistoriken till fil.
     * @return true om sparning lyckades.
     */
    public boolean saveData() {
        return DataHandler.saveRentals(activeAndFinishedRentals);
    }
}
