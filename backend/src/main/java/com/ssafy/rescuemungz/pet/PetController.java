package com.ssafy.rescuemungz.pet;

import com.ssafy.rescuemungz.auth.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
public class PetController {
    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @PostMapping
    public ResponseEntity<Pet> create(@Valid @RequestBody PetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petService.create(AuthUtil.requiredUserId(), request));
    }

    @GetMapping
    public List<Pet> findMine() {
        return petService.findMine(AuthUtil.requiredUserId());
    }

    @GetMapping("/{id}")
    public Pet find(@PathVariable long id) {
        return petService.find(id, AuthUtil.requiredUserId());
    }

    @PutMapping("/{id}")
    public Pet update(@PathVariable long id, @Valid @RequestBody PetRequest request) {
        return petService.update(id, AuthUtil.requiredUserId(), request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        petService.delete(id, AuthUtil.requiredUserId());
        return ResponseEntity.noContent().build();
    }
}
