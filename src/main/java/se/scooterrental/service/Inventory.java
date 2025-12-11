package se.scooterrental.service;

import se.scooterrental.model.Item;
import se.scooterrental.model.Scooter;
import se.scooterrental.model.Sled;
import se.scooterrental.persistence.DataHandler;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class Inventory {
    private List<Item> items;
    private AtomicLong nextId;

    public Inventory() {
        this.items = DataHandler.loadItems();
        if (this.items == null) {
            this.items = new java.util.ArrayList<>();
        }
        initializeNextId();
    }

    private void initializeNextId() {
        long maxId = items.stream()
                .map(Item::getItemId)
                .filter(id -> id.matches("\\d+"))
                .mapToLong(Long::parseLong)
                .max()
                .orElse(999L);
        this.nextId = new AtomicLong(maxId + 1);
    }

    public String generateNewId() {
        return String.valueOf(nextId.getAndIncrement());
    }

    public boolean addItem(Item item) {
        if (items.stream().anyMatch(i -> i.getItemId().equals(item.getItemId()))) {
            return false;
        }
        return items.add(item);
    }

    public boolean updateItem(Item updatedItem) {
        Optional<Item> existingItemOpt = findItemById(updatedItem.getItemId());
        if (existingItemOpt.isPresent()) {
            Item existingItem = existingItemOpt.get();
            existingItem.setName(updatedItem.getName());
            existingItem.setCurrentRentalPrice(updatedItem.getCurrentRentalPrice());
            existingItem.setAvailable(updatedItem.isAvailable());

            if (existingItem instanceof Scooter && updatedItem instanceof Scooter) {
                ((Scooter) existingItem).setEngineDisplacement(((Scooter) updatedItem).getEngineDisplacement());
                ((Scooter) existingItem).setLicensePlate(((Scooter) updatedItem).getLicensePlate());
                ((Scooter) existingItem).setHasElectricStart(((Scooter) updatedItem).hasElectricStart());
            } else if (existingItem instanceof Sled && updatedItem instanceof Sled) {
                ((Sled) existingItem).setType(((Sled) updatedItem).getType());
                ((Sled) existingItem).setMaxWeightKg(((Sled) updatedItem).getMaxWeightKg());
            }
            return true;
        }
        return false;
    }

    public Optional<Item> findItemById(String itemId) {
        return items.stream().filter(i -> i.getItemId().equals(itemId)).findFirst();
    }

    public List<Item> getAllItems() {
        return Collections.unmodifiableList(items);
    }

    public List<Item> getAvailableItems() {
        return items.stream()
                .filter(Item::isAvailable)
                .collect(Collectors.toList());
    }

    public boolean saveData() {
        return DataHandler.saveItems(items);
    }

    // --- SÖKNING OCH STATISTIK ---

    public List<Item> searchItems(String query, String typeFilter, boolean onlyAvailable) {
        String lowerQuery = query.toLowerCase();

        return items.stream()
                .filter(item -> {
                    if ("Alla".equals(typeFilter)) return true;
                    if ("Scooter".equals(typeFilter)) return item instanceof Scooter;
                    if ("Sled".equals(typeFilter)) return item instanceof Sled;
                    return true;
                })
                .filter(item -> !onlyAvailable || item.isAvailable())
                .filter(item -> {
                    if (lowerQuery.isEmpty()) return true;
                    if (item.getName().toLowerCase().contains(lowerQuery)) return true;
                    if (item instanceof Scooter) {
                        Scooter s = (Scooter) item;
                        return String.valueOf(s.getEngineDisplacement()).contains(lowerQuery) ||
                                s.getLicensePlate().toLowerCase().contains(lowerQuery);
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    public List<Item> getMostPopularItems(int limit) {
        return items.stream()
                .sorted(Comparator.comparingInt(Item::getRentalCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Statistik för Dashboard
    public long getTotalCount() { return items.size(); }
    public long getAvailableCount() { return items.stream().filter(Item::isAvailable).count(); }
    public long getRentedCount() { return items.stream().filter(i -> !i.isAvailable()).count(); }

    public Map<String, Long> getModelPopularity() {
        return items.stream()
                .collect(Collectors.groupingBy(
                        Item::getName,
                        Collectors.summingLong(Item::getRentalCount)
                ));
    }
}