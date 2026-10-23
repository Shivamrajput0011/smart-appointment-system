package com.yuvaintern.sams.controller;

import com.yuvaintern.sams.entity.Pet;
import com.yuvaintern.sams.service.PetService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for pets: /api/pets */
@RestController
@RequestMapping("/api/pets")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Pet register(@Valid @RequestBody Pet pet) {
        return petService.register(pet);
    }

    /** GET /api/pets  or  GET /api/pets?search=bruno  or  GET /api/pets?search=3 */
    @GetMapping
    public List<Pet> list(@RequestParam(required = false) String search) {
        return petService.search(search);
    }

    @GetMapping("/{id}")
    public Pet get(@PathVariable Long id) {
        return petService.getById(id);
    }

    @PutMapping("/{id}")
    public Pet update(@PathVariable Long id, @Valid @RequestBody Pet pet) {
        return petService.update(id, pet);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        petService.delete(id);
    }
}
