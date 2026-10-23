package com.yuvaintern.sams.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yuvaintern.sams.entity.Pet;
import com.yuvaintern.sams.exception.ResourceNotFoundException;
import com.yuvaintern.sams.repository.PetRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock
    private PetRepository petRepository;

    @InjectMocks
    private PetService petService;

    private Pet pet(Long id, String name) {
        Pet p = new Pet();
        p.setId(id);
        p.setName(name);
        p.setType("Dog");
        p.setBreed("Labrador");
        p.setAge(3);
        p.setOwnerName("Asha Verma");
        p.setOwnerPhone("9876543210");
        p.setOwnerEmail("asha@example.com");
        return p;
    }

    @Test
    @DisplayName("register: ignores any client-sent ID and saves the pet")
    void register_clearsIdAndSaves() {
        Pet input = pet(99L, "Bruno");
        when(petRepository.save(any(Pet.class))).thenAnswer(inv -> {
            Pet saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Pet result = petService.register(input);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Bruno");
    }

    @Test
    @DisplayName("getById: returns the pet when it exists")
    void getById_found() {
        when(petRepository.findById(1L)).thenReturn(Optional.of(pet(1L, "Bruno")));

        assertThat(petService.getById(1L).getName()).isEqualTo("Bruno");
    }

    @Test
    @DisplayName("getById: throws ResourceNotFoundException when missing")
    void getById_missing() {
        when(petRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> petService.getById(5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("5");
    }

    @Test
    @DisplayName("search: numeric text matches by ID and by name, without duplicates")
    void search_numericMatchesIdAndName() {
        Pet byId = pet(3L, "Bruno");
        Pet byName = pet(7L, "Rex 3");
        when(petRepository.findById(3L)).thenReturn(Optional.of(byId));
        when(petRepository.findByNameContainingIgnoreCase("3")).thenReturn(List.of(byId, byName));

        List<Pet> result = petService.search(" 3 ");

        assertThat(result).extracting(Pet::getId).containsExactly(3L, 7L);
    }

    @Test
    @DisplayName("search: plain text searches by name only")
    void search_byNameOnly() {
        when(petRepository.findByNameContainingIgnoreCase("bru")).thenReturn(List.of(pet(1L, "Bruno")));

        assertThat(petService.search("bru")).hasSize(1);
        verify(petRepository, never()).findById(any());
    }

    @Test
    @DisplayName("update: copies new values onto the existing pet")
    void update_changesFields() {
        Pet existing = pet(1L, "Bruno");
        when(petRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(petRepository.save(any(Pet.class))).thenAnswer(inv -> inv.getArgument(0));

        Pet changes = pet(null, "Bruno Jr");
        changes.setAge(4);

        Pet result = petService.update(1L, changes);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Bruno Jr");
        assertThat(result.getAge()).isEqualTo(4);
    }

    @Test
    @DisplayName("update: unknown pet throws ResourceNotFoundException and saves nothing")
    void update_missing() {
        when(petRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> petService.update(9L, pet(null, "X")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(petRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete: removes an existing pet")
    void delete_existing() {
        Pet existing = pet(1L, "Bruno");
        when(petRepository.findById(1L)).thenReturn(Optional.of(existing));

        petService.delete(1L);

        verify(petRepository).delete(existing);
    }

    @Test
    @DisplayName("delete: unknown pet throws ResourceNotFoundException")
    void delete_missing() {
        when(petRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> petService.delete(2L)).isInstanceOf(ResourceNotFoundException.class);
        verify(petRepository, never()).delete(any(Pet.class));
    }
}
