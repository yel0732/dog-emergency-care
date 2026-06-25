package com.ssafy.rescuemungz.emergencycheck;

import com.ssafy.rescuemungz.auth.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/emergency-checks")
public class EmergencyCheckController {
    private final EmergencyCheckService service;

    public EmergencyCheckController(EmergencyCheckService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<EmergencyCheck> create(@Valid @RequestBody EmergencyCheckRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(AuthUtil.requiredUserId(), request));
    }

    @GetMapping
    public List<EmergencyCheck> findMine() {
        return service.findMine(AuthUtil.requiredUserId());
    }

    @GetMapping("/{id}")
    public EmergencyCheck find(@PathVariable long id) {
        return service.find(id, AuthUtil.requiredUserId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id, AuthUtil.requiredUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/vet-report")
    public VetReport findVetReport(@PathVariable long id) {
        return service.findVetReport(id, AuthUtil.requiredUserId());
    }

    @GetMapping("/{id}/vet-report/pdf")
    public ResponseEntity<byte[]> downloadVetReport(@PathVariable long id) {
        byte[] pdf = service.vetReportPdf(id, AuthUtil.requiredUserId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("구해줘 멍즈 수의사 전달 리포트.pdf", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(pdf);
    }
}
