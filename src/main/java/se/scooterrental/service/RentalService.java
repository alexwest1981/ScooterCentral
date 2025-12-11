package se.scooterrental.service;

import se.scooterrental.model.Item;
import se.scooterrental.model.Member;
import se.scooterrental.model.Rental;
import se.scooterrental.model.PricePolicy;
import se.scooterrental.persistence.DataHandler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class RentalService {
    private MemberRegistry memberRegistry;
    private Inventory inventory;
    private List<Rental> rentals;
    private AtomicLong nextId;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter OLD_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public RentalService(MemberRegistry memberRegistry, Inventory inventory) {
        this.memberRegistry = memberRegistry;
        this.inventory = inventory;
        this.rentals = DataHandler.loadRentals();
        if (this.rentals == null) {
            this.rentals = new java.util.ArrayList<>();
        }
        initializeNextId();
    }

    private void initializeNextId() {
        long maxId = rentals.stream()
                .map(Rental::getId)
                .filter(id -> id != null && id.matches("\\d+"))
                .mapToLong(Long::parseLong)
                .max()
                .orElse(1000L);
        this.nextId = new AtomicLong(maxId + 1);
    }

    private String generateId() {
        return String.valueOf(nextId.getAndIncrement());
    }

    public boolean rentItem(String memberId, String itemId, PricePolicy policy) {
        Optional<Member> memberOpt = memberRegistry.findMemberById(memberId);
        Optional<Item> itemOpt = inventory.findItemById(itemId);

        if (memberOpt.isPresent() && itemOpt.isPresent()) {
            Item item = itemOpt.get();
            Member member = memberOpt.get();

            if (item.isAvailable()) {
                Rental rental = new Rental(generateId(), memberId, itemId, policy);
                rentals.add(rental);

                item.setAvailable(false);
                item.incrementRentalCount();
                inventory.updateItem(item);

                member.addRentalToHistory(rental.getId());
                memberRegistry.updateMember(member);

                return saveData();
            }
        }
        return false;
    }

    public boolean rentItem(String memberId, String itemId) {
        return rentItem(memberId, itemId, null);
    }

    public Optional<Double> endRental(String rentalId) {
        Optional<Rental> rentalOpt = rentals.stream()
                .filter(r -> r.getId() != null && r.getId().equals(rentalId) && r.isActive())
                .findFirst();

        if (rentalOpt.isPresent()) {
            Rental rental = rentalOpt.get();
            Optional<Item> itemOpt = inventory.findItemById(rental.getItemId());

            double finalPrice = 0.0;
            if (itemOpt.isPresent()) {
                finalPrice = rental.getCurrentCost(itemOpt.get().getCurrentRentalPrice());
            }

            rental.endRental(finalPrice);

            if (itemOpt.isPresent()) {
                Item item = itemOpt.get();
                item.setAvailable(true);
                inventory.updateItem(item);
            }

            saveData();
            return Optional.of(finalPrice);
        }
        return Optional.empty();
    }

    public List<Rental> getActiveRentals() {
        return rentals.stream()
                .filter(r -> r.getId() != null && r.isActive())
                .collect(Collectors.toList());
    }

    // NY METOD: Hämtar all historik (inklusive avslutade)
    public List<Rental> getRentalsHistory() {
        return new ArrayList<>(rentals);
    }

    public List<Rental> getRentalsForMember(String memberId) {
        return rentals.stream()
                .filter(r -> r.getMemberId().equals(memberId))
                .collect(Collectors.toList());
    }

    public double getTotalRevenue() {
        return rentals.stream()
                .filter(r -> !r.isActive())
                .mapToDouble(Rental::getTotalCost)
                .sum();
    }

    public Map<LocalDate, Double> getRevenueData(String period) {
        LocalDate now = LocalDate.now();
        LocalDate startDate;

        switch (period) {
            case "1 Vecka": startDate = now.minusWeeks(1); break;
            case "1 Månad": startDate = now.minusMonths(1); break;
            case "1 År":    startDate = now.minusYears(1); break;
            case "1 Dag":   startDate = now; break;
            default:        startDate = now.minusWeeks(1); break;
        }

        Map<LocalDate, Double> revenueMap = new HashMap<>();

        if (period.equals("1 Dag")) {
            revenueMap.put(now, 0.0);
        } else {
            for (LocalDate date = startDate; !date.isAfter(now); date = date.plusDays(1)) {
                revenueMap.put(date, 0.0);
            }
        }

        List<Rental> completedRentals = rentals.stream()
                .filter(r -> !r.isActive() && r.getId() != null)
                .collect(Collectors.toList());

        for (Rental r : completedRentals) {
            LocalDateTime rentalDate = parseDateTime(r.getStartTime());

            if (rentalDate != null) {
                LocalDate dateKey = rentalDate.toLocalDate();

                if ((dateKey.isEqual(startDate) || dateKey.isAfter(startDate)) &&
                        (dateKey.isEqual(now) || dateKey.isBefore(now))) {

                    revenueMap.merge(dateKey, r.getTotalCost(), Double::sum);
                }
            }
        }
        return revenueMap;
    }

    public LocalDateTime parseDateTime(String timeStr) {
        if (timeStr == null) return null;
        try {
            return LocalDateTime.parse(timeStr, DATE_FORMATTER);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(timeStr, OLD_DATE_FORMATTER);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public Optional<Member> getMemberById(String memberId) {
        return memberRegistry.findMemberById(memberId);
    }

    public boolean saveData() {
        return DataHandler.saveRentals(rentals);
    }
}