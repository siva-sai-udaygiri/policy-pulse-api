package com.policypulse.api;

import com.policypulse.api.policy.dto.CreatePolicyRequest;
import com.policypulse.api.policy.dto.PolicyResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.policypulse.api.policy.dto.UpdatePolicyRequest;

import java.io.IOException;
@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    public Page<PolicyResponse> getAllPolicies(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String policyNumber,
            @RequestParam(defaultValue = "createdAt") @Pattern(regexp = "createdAt|updatedAt|premium|policyNumber|holderName|status") String sortBy,
            @RequestParam(defaultValue = "desc")
            @Pattern(regexp = "(?i)asc|desc") String sortDir)
    {
        return policyService.getAllPolicies(
                page,
                size,
                policyNumber,
                sortBy,
                sortDir
        );
    }
    @PostMapping
    public PolicyResponse createPolicy(
            @Valid @RequestBody CreatePolicyRequest request
    ) {
        return policyService.createPolicy(request);
    }

    @GetMapping("/{id}")
    public PolicyResponse getPolicyById(@PathVariable Long id) {
        return policyService.getPolicyById(id);
    }

    @PutMapping("/{id}")
    public PolicyResponse updatePolicy(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePolicyRequest request
    ) {
        return policyService.updatePolicy(id, request);
    }
    @DeleteMapping("/{id}")
    public void deletePolicy(@PathVariable Long id) {
        policyService.deletePolicy(id);
    }
    @GetMapping("/search")
    public Page<PolicyResponse> getPoliciesByStatus(
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return policyService.getPoliciesByStatus(status, page, size);
    }
    @PostMapping("/{id}/document")
    public PolicyResponse uploadPolicyDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        return policyService.uploadPolicyDocument(id, file);
    }
    @GetMapping("/{id}/document")
    public ResponseEntity<byte[]> downloadPolicyDocument(
            @PathVariable Long id
    ) {
        byte[] fileBytes = policyService.downloadPolicyDocument(id);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"policy-" + id + "-document\""
                )
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fileBytes);
    }
}