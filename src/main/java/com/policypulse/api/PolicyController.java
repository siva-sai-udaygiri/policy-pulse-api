package com.policypulse.api;

import com.policypulse.api.policy.dto.CreatePolicyRequest;
import com.policypulse.api.policy.dto.PolicyResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.util.List;
@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    public Page<Policy> getAllPolicies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return policyService.getAllPolicies(page, size);
    }
    @PostMapping
    public PolicyResponse createPolicy(
            @Valid @RequestBody CreatePolicyRequest request
    ) {
        return policyService.createPolicy(request);
    }

    @GetMapping("/{id}")
    public Policy getPolicyById(@PathVariable Long id) {
        return policyService.getPolicyById(id);
    }
    @PutMapping("/{id}")
    public Policy updatePolicy(@PathVariable Long id, @Valid @RequestBody Policy policy) {
        return policyService.updatePolicy(id, policy);
    }
    @DeleteMapping("/{id}")
    public void deletePolicy(@PathVariable Long id) {
        policyService.deletePolicy(id);
    }
    @GetMapping("/search")
    public Page<Policy> getPoliciesByStatus(
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return policyService.getPoliciesByStatus(status, page, size);
    }
    @PostMapping("/{id}/document")
    public Policy uploadPolicyDocument(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        return policyService.uploadPolicyDocument(id, file);
    }
    @GetMapping("/{id}/document")
    public ResponseEntity<byte[]> downloadPolicyDocument(@PathVariable Long id) {
        Policy policy = policyService.getPolicyById(id);
        byte[] fileBytes = policyService.downloadPolicyDocument(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + policy.getDocumentKey() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fileBytes);
    }
}