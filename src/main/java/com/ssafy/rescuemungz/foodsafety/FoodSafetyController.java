package com.ssafy.rescuemungz.foodsafety;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/food-safety")
public class FoodSafetyController {
    private final FoodSafetyService service;

    public FoodSafetyController(FoodSafetyService service) {
        this.service = service;
    }

    @GetMapping
    public List<FoodSafety> search(@RequestParam(required = false) String keyword) {
        return service.search(keyword);
    }

    @GetMapping("/{id}")
    public FoodSafety find(@PathVariable long id) {
        return service.find(id);
    }
}
