package se.scooterrental.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Representerar en medlem i uthyrningsklubben.
 */
public class Member {
    private String memberId; // FIX: Inte längre final, så vi kan redigera ID
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private MemberStatus status;
    private List<String> rentalHistory;

    public enum MemberStatus {
        STANDARD,
        PREMIUM,
        STUDENT
    }

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9 -]{7,15}$");

    /**
     * Konstruktor för nya medlemmar.
     */
    public Member(String memberId, String firstName, String lastName, String phone, String email, MemberStatus status) {
        if (memberId == null || memberId.trim().isEmpty()) {
            throw new IllegalArgumentException("ID får inte vara tomt.");
        }
        this.memberId = memberId;

        setFirstName(firstName);
        setLastName(lastName);
        setPhone(phone);
        this.email = (email != null) ? email : "";
        this.status = status;
        this.rentalHistory = new ArrayList<>();
    }

    // --- Getters ---

    public String getMemberId() { return memberId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public MemberStatus getStatus() { return status; }

    public List<String> getRentalHistory() {
        return new ArrayList<>(this.rentalHistory);
    }

    // --- Setters med validering ---

    // NY METOD: För att kunna byta ID
    public void setMemberId(String memberId) {
        if (memberId == null || memberId.trim().isEmpty()) {
            throw new IllegalArgumentException("ID får inte vara tomt.");
        }
        this.memberId = memberId;
    }

    public void setFirstName(String firstName) {
        if (firstName == null || firstName.trim().isEmpty()) throw new IllegalArgumentException("Förnamn krävs.");
        this.firstName = firstName.trim();
    }

    public void setLastName(String lastName) {
        if (lastName == null || lastName.trim().isEmpty()) throw new IllegalArgumentException("Efternamn krävs.");
        this.lastName = lastName.trim();
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) throw new IllegalArgumentException("Telefonnummer krävs.");
        if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new IllegalArgumentException("Ogiltigt telefonnummer.");
        }
        this.phone = phone.trim();
    }

    public void setStatus(MemberStatus status) {
        this.status = status;
    }

    public void addRentalToHistory(String rentalRef) {
        this.rentalHistory.add(rentalRef);
    }

    public String getName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return String.format("%s %s (ID: %s) [%s]", firstName, lastName, memberId, status);
    }
}