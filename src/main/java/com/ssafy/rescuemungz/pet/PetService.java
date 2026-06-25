package com.ssafy.rescuemungz.pet;

import com.ssafy.rescuemungz.common.ForbiddenException;
import com.ssafy.rescuemungz.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PetService {
    private static final String DEFAULT_SPECIES = "강아지";

    private final PetMapper petMapper;

    public PetService(PetMapper petMapper) {
        this.petMapper = petMapper;
    }

    @Transactional
    public Pet create(long userId, PetRequest request) {
        Pet pet = toPet(request);
        pet.setUserId(userId);
        petMapper.insert(pet);
        return find(pet.getId(), userId);
    }

    public List<Pet> findMine(long userId) {
        return petMapper.findByUserId(userId);
    }

    public Pet find(long id, long userId) {
        Pet pet = petMapper.findByIdAndUserId(id, userId);
        if (pet == null) {
            throw new NotFoundException("반려동물을 찾을 수 없습니다.");
        }
        return pet;
    }

    @Transactional
    public Pet update(long id, long userId, PetRequest request) {
        assertPetOwner(id, userId);
        Pet pet = toPet(request);
        pet.setId(id);
        pet.setUserId(userId);
        if (petMapper.update(pet) == 0) {
            throw new NotFoundException("반려동물을 찾을 수 없습니다.");
        }
        return find(id, userId);
    }

    @Transactional
    public void delete(long id, long userId) {
        assertPetOwner(id, userId);
        if (petMapper.delete(id, userId) == 0) {
            throw new NotFoundException("반려동물을 찾을 수 없습니다.");
        }
    }

    private Pet toPet(PetRequest request) {
        Pet pet = new Pet();
        pet.setName(request.name().trim());
        pet.setSpecies(DEFAULT_SPECIES);
        pet.setBreed(blankToNull(request.breed()));
        pet.setAge(request.age());
        pet.setWeight(request.weight());
        pet.setGender(normalizeGender(request.gender()));
        pet.setNeutered(Boolean.TRUE.equals(request.neutered()));
        pet.setAllergies(blankToNull(request.allergies()));
        pet.setDiseases(blankToNull(request.diseases()));
        return pet;
    }

    private void assertPetOwner(long id, long userId) {
        Pet pet = petMapper.findById(id);
        if (pet == null) {
            throw new NotFoundException("반려동물을 찾을 수 없습니다.");
        }
        if (!pet.getUserId().equals(userId)) {
            throw new ForbiddenException("본인이 등록한 반려동물만 수정하거나 삭제할 수 있습니다.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeGender(String value) {
        if (value == null || value.isBlank()) return "UNKNOWN";
        return switch (value.trim().toUpperCase()) {
            case "MALE", "M" -> "M";
            case "FEMALE", "F" -> "F";
            default -> "UNKNOWN";
        };
    }
}
