package com.policypulse.api;

import com.policypulse.api.policy.domain.PolicyStatus;
import com.policypulse.api.policy.dto.CreatePolicyRequest;
import com.policypulse.api.policy.dto.PolicyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;


@Service
public class PolicyService {

    private final PolicyRepository policyRepository;

    private final S3Service s3Service;
    private final PolicyKafkaProducer policyKafkaProducer;

    public PolicyService(
            PolicyRepository policyRepository,
            S3Service s3Service,
            PolicyKafkaProducer policyKafkaProducer
    ) {
        this.policyRepository = policyRepository;
        this.s3Service = s3Service;
        this.policyKafkaProducer = policyKafkaProducer;
    }
    public Policy getPolicyById(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy not found: " + id));
    }
    public PolicyResponse createPolicy(CreatePolicyRequest request) {
        Policy policy = new Policy();

        policy.setPolicyNumber(request.policyNumber());
        policy.setHolderName(request.holderName());
        policy.setStatus(request.status().name());
        policy.setPremium(request.premium());

        Policy savedPolicy = policyRepository.save(policy);

        return toResponse(savedPolicy);
    }
    public Policy updatePolicy(Long id, Policy policy) {
        Policy existingPolicy = policyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Policy not found: " + id));

        existingPolicy.setPolicyNumber(policy.getPolicyNumber());
        existingPolicy.setHolderName(policy.getHolderName());
        existingPolicy.setStatus(policy.getStatus());
        existingPolicy.setPremium(policy.getPremium());

        return policyRepository.save(existingPolicy);
    }

    public void deletePolicy(Long id) {
        Policy existingPolicy = policyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Policy not found: " + id));

        policyRepository.delete(existingPolicy);
    }
    public Page<Policy> getAllPolicies(int page, int size) {
        return policyRepository.findAll(PageRequest.of(page, size));
    }
    public Page<Policy> getPoliciesByStatus(String status, int page, int size) {
        return policyRepository.findByStatusIgnoreCase(status, PageRequest.of(page, size));
    }
    public Policy uploadPolicyDocument(Long id, MultipartFile file) throws IOException {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Policy not found: " + id));

        String documentKey = s3Service.uploadFile(file);
        policy.setDocumentKey(documentKey);

        Policy savedPolicy = policyRepository.save(policy);

        policyKafkaProducer.publishDocumentUploaded(
                new PolicyDocumentUploadedEvent(
                        "DOCUMENT_UPLOADED",
                        savedPolicy.getId(),
                        savedPolicy.getPolicyNumber(),
                        savedPolicy.getDocumentKey(),
                        java.time.Instant.now().toString()
                )
        );

        return savedPolicy;
    }
    public byte[] downloadPolicyDocument(Long id) {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Policy not found: " + id));

        if (policy.getDocumentKey() == null || policy.getDocumentKey().isBlank()) {
            throw new RuntimeException("No document found for policy: " + id);
        }

        return s3Service.downloadFile(policy.getDocumentKey());
    }
    private PolicyResponse toResponse(Policy policy) {
        return new PolicyResponse(
                policy.getId(),
                policy.getPolicyNumber(),
                policy.getHolderName(),
                PolicyStatus.valueOf(policy.getStatus()),
                policy.getPremium(),
                policy.getDocumentKey() != null,
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }
}