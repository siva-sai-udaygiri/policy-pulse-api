package com.policypulse.api;

import com.policypulse.api.policy.domain.PolicyStatus;
import com.policypulse.api.policy.dto.CreatePolicyRequest;
import com.policypulse.api.policy.dto.PolicyResponse;
import com.policypulse.api.policy.dto.UpdatePolicyRequest;
import com.policypulse.api.policy.exception.PolicyNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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
    public PolicyResponse getPolicyById(Long id) {
        Policy policy = getPolicyEntityById(id);

        return toResponse(policy);
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

    public PolicyResponse updatePolicy(
            Long id,
            UpdatePolicyRequest request
    ) {
        Policy existingPolicy = getPolicyEntityById(id);

        existingPolicy.setPolicyNumber(request.policyNumber());
        existingPolicy.setHolderName(request.holderName());
        existingPolicy.setStatus(request.status().name());
        existingPolicy.setPremium(request.premium());

        Policy savedPolicy = policyRepository.save(existingPolicy);

        return toResponse(savedPolicy);
    }

    public void deletePolicy(Long id) {
        Policy existingPolicy = getPolicyEntityById(id);

        policyRepository.delete(existingPolicy);
    }

    public Page<PolicyResponse> getAllPolicies(int page, int size) {
        return policyRepository
                .findAll(PageRequest.of(page, size))
                .map(this::toResponse);
    }

    public Page<PolicyResponse> getPoliciesByStatus(
            String status,
            int page,
            int size
    ) {
        return policyRepository
                .findByStatusIgnoreCase(
                        status,
                        PageRequest.of(page, size)
                )
                .map(this::toResponse);
    }

    public PolicyResponse uploadPolicyDocument(
            Long id,
            MultipartFile file
    ) throws IOException {

        Policy policy = getPolicyEntityById(id);

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

        return toResponse(savedPolicy);
    }

    public byte[] downloadPolicyDocument(Long id) {
        Policy policy = getPolicyEntityById(id);

        if (policy.getDocumentKey() == null
                || policy.getDocumentKey().isBlank()) {

            throw new RuntimeException(
                    "No document found for policy: " + id
            );
        }

        return s3Service.downloadFile(policy.getDocumentKey());
    }
    private Policy getPolicyEntityById(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() ->
                        new PolicyNotFoundException(id)
                );
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