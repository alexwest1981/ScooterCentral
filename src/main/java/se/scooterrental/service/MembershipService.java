package se.scooterrental.service;

import se.scooterrental.model.Member;
import java.util.List;
import java.util.Optional;

/**
 * Hanterar affärslogik relaterad till medlemskap.
 * Uppdaterad för att hantera firstName/lastName och nya metodnamn.
 */
public class MembershipService {
    private final MemberRegistry registry;

    public MembershipService(MemberRegistry registry) {
        this.registry = registry;
    }

    /**
     * Lägger till en ny medlem med automatiskt genererat ID.
     * Splittar namnet automatiskt till för- och efternamn.
     */
    public Optional<Member> registerNewMember(String fullName, String phone, Member.MemberStatus status) {
        String newId = registry.generateNewId();

        // Enkel logik för att dela upp namnet
        String firstName = fullName;
        String lastName = "";
        int spaceIndex = fullName.indexOf(' ');
        if (spaceIndex != -1) {
            firstName = fullName.substring(0, spaceIndex);
            lastName = fullName.substring(spaceIndex + 1);
        } else {
            lastName = "Okänd"; // Fallback om inget efternamn anges
        }

        // Generera en dummy-email eller lämna tom
        String email = firstName.toLowerCase() + "." + lastName.toLowerCase().replaceAll(" ", "") + "@scooterrental.se";

        try {
            // Anropa den uppdaterade konstruktorn i Member
            Member newMember = new Member(newId, firstName, lastName, phone, email, status);

            if (registry.addMember(newMember)) {
                registry.saveData(); // Spara direkt vid registrering
                return Optional.of(newMember);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("FEL vid registrering: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Uppdaterar en befintlig medlems information.
     */
    public boolean updateMemberDetails(String id, String fullName, String phone, Member.MemberStatus status) {
        Optional<Member> existingMemberOpt = registry.findMemberById(id);

        if (existingMemberOpt.isPresent()) {
            Member member = existingMemberOpt.get();

            // Dela upp namnet igen
            String firstName = fullName;
            String lastName = "";
            int spaceIndex = fullName.indexOf(' ');
            if (spaceIndex != -1) {
                firstName = fullName.substring(0, spaceIndex);
                lastName = fullName.substring(spaceIndex + 1);
            } else {
                lastName = member.getLastName(); // Behåll gammalt om nytt format är konstigt, eller sätt "-"
            }

            member.setFirstName(firstName);
            member.setLastName(lastName);
            member.setPhone(phone);
            member.setStatus(status);

            registry.saveData(); // Spara ändringar
            return true;
        }

        return false;
    }

    /**
     * Hämtar alla medlemmar.
     */
    public List<Member> getAllMembers() {
        // Fix: Använder nu getMembers() istället för getAllMembers()
        return registry.getMembers();
    }

    /**
     * Söker medlemmar baserat på namn eller ID.
     */
    public List<Member> searchMembers(String query) {
        Optional<Member> memberById = registry.findMemberById(query);
        if (memberById.isPresent()) {
            return List.of(memberById.get());
        }
        return registry.searchMembersByName(query);
    }
}