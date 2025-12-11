package se.scooterrental.service;

import se.scooterrental.model.Member;
import se.scooterrental.persistence.DataHandler;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Hanterar medlemsdatabasen (MemberRegistry).
 * Fixad: Innehåller getMembers() för att matcha MemberView.
 */
public class MemberRegistry {
    private List<Member> members;
    private AtomicLong nextId;

    public MemberRegistry() {
        this.members = DataHandler.loadMembers();
        if (this.members == null) {
            this.members = new java.util.ArrayList<>();
        }
        initializeNextId();
    }

    private void initializeNextId() {
        long maxId = members.stream()
                .map(Member::getMemberId) // Matchar Member.java
                .filter(id -> id != null && id.matches("\\d+"))
                .mapToLong(Long::parseLong)
                .max()
                .orElse(1000L);
        this.nextId = new AtomicLong(maxId + 1);
    }

    public String generateNewId() {
        return String.valueOf(nextId.getAndIncrement());
    }

    /**
     * Returnerar listan över medlemmar.
     * VIKTIGT: Metodnamnet är nu getMembers() för att lösa kompileringsfelet.
     */
    public List<Member> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public boolean addMember(Member member) {
        if (members.stream().anyMatch(m -> m.getMemberId().equals(member.getMemberId()))) {
            return false;
        }
        boolean added = members.add(member);
        if (added) {
            saveData();
        }
        return added;
    }

    public boolean updateMember(Member updatedMember) {
        Optional<Member> existingMemberOpt = findMemberById(updatedMember.getMemberId());
        if (existingMemberOpt.isPresent()) {
            Member existingMember = existingMemberOpt.get();
            // Uppdaterar fälten
            existingMember.setFirstName(updatedMember.getFirstName());
            existingMember.setLastName(updatedMember.getLastName());
            existingMember.setEmail(updatedMember.getEmail());
            existingMember.setPhone(updatedMember.getPhone());
            existingMember.setStatus(updatedMember.getStatus());
            saveData();
            return true;
        }
        return false;
    }

    /**
     * Tar bort en medlem.
     * @param member Medlemmen som ska tas bort.
     * @return true om medlemmen togs bort, annars false.
     */
    public boolean removeMember(Member member) {
        boolean removed = members.remove(member);
        if (removed) {
            saveData();
        }
        return removed;
    }

    public Optional<Member> findMemberById(String id) {
        return members.stream()
                .filter(m -> m.getMemberId().equalsIgnoreCase(id))
                .findFirst();
    }

    public List<Member> searchMembersByName(String nameQuery) {
        String q = nameQuery.toLowerCase();
        return members.stream()
                .filter(m -> m.getFirstName().toLowerCase().contains(q) ||
                        m.getLastName().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    public boolean saveData() {
        return DataHandler.saveMembers(members);
    }
}