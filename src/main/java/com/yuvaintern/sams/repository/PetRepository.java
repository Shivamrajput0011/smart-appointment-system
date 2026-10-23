package com.yuvaintern.sams.repository;

import com.yuvaintern.sams.entity.Pet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetRepository extends JpaRepository<Pet, Long> {

    List<Pet> findByNameContainingIgnoreCase(String name);
}
