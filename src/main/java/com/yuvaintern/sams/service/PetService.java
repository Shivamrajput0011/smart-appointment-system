package com.yuvaintern.sams.service;

import com.yuvaintern.sams.entity.Pet;
import com.yuvaintern.sams.exception.ResourceNotFoundException;
import com.yuvaintern.sams.repository.PetRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for pet registration. */
@Service
public class PetService {

    private final PetRepository petRepository;

    public PetService(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    @Transactional
    public Pet register(Pet pet) {
        pet.setId(null); // the database generates the Pet ID
        return petRepository.save(pet);
    }

    @Transactional(readOnly = true)
    public List<Pet> getAll() {
        return petRepository.findAll(Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public Pet getById(Long id) {
        return petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet with ID " + id + " was not found."));
    }

    /** Matches the Pet ID exactly (when the text is a number) and/or part of the pet name. */
    @Transactional(readOnly = true)
    public List<Pet> search(String query) {
        if (query == null || query.isBlank()) {
            return getAll();
        }
        String term = query.trim();
        Map<Long, Pet> results = new LinkedHashMap<>();
        if (term.matches("\\d{1,18}")) {
            petRepository.findById(Long.parseLong(term)).ifPresent(p -> results.put(p.getId(), p));
        }
        petRepository.findByNameContainingIgnoreCase(term).forEach(p -> results.putIfAbsent(p.getId(), p));
        return new ArrayList<>(results.values());
    }

    @Transactional
    public Pet update(Long id, Pet updated) {
        Pet existing = getById(id);
        existing.setName(updated.getName());
        existing.setType(updated.getType());
        existing.setBreed(updated.getBreed());
        existing.setAge(updated.getAge());
        existing.setOwnerName(updated.getOwnerName());
        existing.setOwnerPhone(updated.getOwnerPhone());
        existing.setOwnerEmail(updated.getOwnerEmail());
        return petRepository.save(existing);
    }

    /** Deletes the pet and (by cascade) all of its appointments. */
    @Transactional
    public void delete(Long id) {
        petRepository.delete(getById(id));
    }
}
