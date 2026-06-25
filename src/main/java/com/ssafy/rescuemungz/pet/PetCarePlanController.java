package com.ssafy.rescuemungz.pet;

import com.ssafy.rescuemungz.auth.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pet-care-plans")
public class PetCarePlanController {
    private final PetCarePlanService service;

    public PetCarePlanController(PetCarePlanService service) {
        this.service = service;
    }

    @GetMapping
    public List<PetCarePlan> findMine() {
        return service.findMine(AuthUtil.requiredUserId());
    }

    @PostMapping
    public ResponseEntity<PetCarePlan> create(@Valid @RequestBody PetCarePlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(AuthUtil.requiredUserId(), request));
    }

    @PutMapping("/{id}")
    public PetCarePlan update(@PathVariable long id, @Valid @RequestBody PetCarePlanRequest request) {
        return service.update(id, AuthUtil.requiredUserId(), request);
    }

    @PatchMapping("/{id}/completed")
    public PetCarePlan updateCompleted(@PathVariable long id, @RequestBody java.util.Map<String, Boolean> body) {
        return service.updateCompleted(id, AuthUtil.requiredUserId(), Boolean.TRUE.equals(body.get("completed")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id, AuthUtil.requiredUserId());
        return ResponseEntity.noContent().build();
    }
}
