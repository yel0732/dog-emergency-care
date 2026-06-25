package com.ssafy.rescuemungz.pet;

import com.ssafy.rescuemungz.common.ForbiddenException;
import com.ssafy.rescuemungz.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PetCarePlanService {
    private final PetCarePlanMapper planMapper;
    private final PetService petService;

    public PetCarePlanService(PetCarePlanMapper planMapper, PetService petService) {
        this.planMapper = planMapper;
        this.petService = petService;
    }

    public List<PetCarePlan> findMine(long userId) {
        return planMapper.findByUserId(userId);
    }

    @Transactional
    public PetCarePlan create(long userId, PetCarePlanRequest request) {
        petService.find(request.petId(), userId);
        PetCarePlan plan = toPlan(request);
        plan.setUserId(userId);
        planMapper.insert(plan);
        return find(plan.getId(), userId);
    }

    public PetCarePlan find(long id, long userId) {
        PetCarePlan plan = planMapper.findById(id);
        if (plan == null) {
            throw new NotFoundException("관리 계획을 찾을 수 없습니다.");
        }
        if (!plan.getUserId().equals(userId)) {
            throw new ForbiddenException("본인의 관리 계획만 조회할 수 있습니다.");
        }
        return plan;
    }

    @Transactional
    public PetCarePlan update(long id, long userId, PetCarePlanRequest request) {
        assertPlanOwner(id, userId, "수정");
        petService.find(request.petId(), userId);
        PetCarePlan plan = toPlan(request);
        plan.setId(id);
        plan.setUserId(userId);
        if (planMapper.update(plan) == 0) {
            throw new NotFoundException("관리 계획을 찾을 수 없습니다.");
        }
        return find(id, userId);
    }

    @Transactional
    public PetCarePlan updateCompleted(long id, long userId, boolean completed) {
        assertPlanOwner(id, userId, "완료 처리");
        if (planMapper.updateCompleted(id, userId, completed) == 0) {
            throw new NotFoundException("관리 계획을 찾을 수 없습니다.");
        }
        return find(id, userId);
    }

    @Transactional
    public void delete(long id, long userId) {
        assertPlanOwner(id, userId, "삭제");
        if (planMapper.delete(id, userId) == 0) {
            throw new NotFoundException("관리 계획을 찾을 수 없습니다.");
        }
    }

    private PetCarePlan toPlan(PetCarePlanRequest request) {
        PetCarePlan plan = new PetCarePlan();
        plan.setPetId(request.petId());
        plan.setEmergencyCheckId(request.emergencyCheckId());
        plan.setTitle(request.title().trim());
        plan.setCategory(normalizeCareCategory(request.category()));
        plan.setPlanDate(request.planDate());
        plan.setMemo(blankToNull(request.memo()));
        plan.setCompleted(Boolean.TRUE.equals(request.completed()));
        return plan;
    }

    private void assertPlanOwner(long id, long userId, String action) {
        PetCarePlan plan = planMapper.findById(id);
        if (plan == null) {
            throw new NotFoundException("관리 계획을 찾을 수 없습니다.");
        }
        if (!plan.getUserId().equals(userId)) {
            throw new ForbiddenException("본인의 관리 계획만 %s할 수 있습니다.".formatted(action));
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeCareCategory(String value) {
        String category = value == null ? "" : value.trim();
        return switch (category) {
            case "병원 진료", "병원 방문", "병원 예약", "진료" -> "진료";
            case "예방접종", "접종" -> "접종";
            case "약 복용", "복약" -> "약 복용";
            case "응급 체크", "관찰 기록" -> "응급 체크";
            case "검진" -> "검진";
            case "케어 루틴" -> "케어 루틴";
            default -> "케어 루틴";
        };
    }

    private String normalizeCategory(String value) {
        return switch (value == null ? "" : value.trim()) {
            case "병원 방문", "병원 예약" -> "진료";
            case "예방접종" -> "접종";
            case "관찰 기록" -> "응급 체크";
            case "응급 체크", "진료", "접종", "약 복용", "검진", "케어 루틴" -> value.trim();
            case "약복용" -> "약 복용";
            default -> "케어 루틴";
        };
    }
}
