package com.ssafy.rescuemungz.hospital;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/hospitals")
public class HospitalController {
    private final HospitalService service;

    public HospitalController(HospitalService service) {
        this.service = service;
    }

    @GetMapping
    public List<Hospital> search(@RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) BigDecimal lat,
                                 @RequestParam(required = false) BigDecimal lng,
                                 @RequestParam(defaultValue = "false") boolean emergencyOnly,
                                 @RequestParam(defaultValue = "false") boolean nightOnly,
                                 @RequestParam(defaultValue = "false") boolean phoneOnly,
                                 @RequestParam(defaultValue = "false") boolean locatedOnly,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(required = false) String sido,
                                 @RequestParam(required = false) String sigungu) {
        return service.search(keyword, lat, lng, emergencyOnly, nightOnly, phoneOnly, locatedOnly, status, sido, sigungu);
    }

    @GetMapping("/regions")
    public HospitalRegionResponse regions() {
        return service.regions();
    }

    @GetMapping("/{id}")
    public Hospital find(@PathVariable long id) {
        return service.find(id);
    }

    @PostMapping("/geocode")
    public HospitalGeocodeResult geocode(@RequestParam(defaultValue = "20") int limit) {
        return service.geocodeMissing(limit);
    }

    @PostMapping("/sync-hours")
    public HospitalHoursResult syncHours(@RequestParam(defaultValue = "20") int limit,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) BigDecimal lat,
                                         @RequestParam(required = false) BigDecimal lng,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String sido,
                                         @RequestParam(required = false) String sigungu) {
        return service.syncOpeningHours(limit, keyword, lat, lng, status, sido, sigungu);
    }
}
