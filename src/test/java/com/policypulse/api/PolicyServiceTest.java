package com.policypulse.api;

import com.policypulse.api.policy.domain.PolicyStatus;
import com.policypulse.api.policy.dto.CreatePolicyRequest;
import com.policypulse.api.policy.dto.PolicyResponse;
import com.policypulse.api.policy.exception.DuplicatePolicyException;
import com.policypulse.api.policy.exception.PolicyDocumentNotFoundException;
import com.policypulse.api.policy.exception.PolicyNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.policypulse.api.policy.dto.UpdatePolicyRequest;
@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private PolicyKafkaProducer policyKafkaProducer;

    @InjectMocks
    private PolicyService policyService;

    @Test
    void createPolicy_mapsRequest_savesEntity_andReturnsResponse() {
        CreatePolicyRequest request = new CreatePolicyRequest(
                "POL-1001",
                "Test User",
                PolicyStatus.ACTIVE,
                new BigDecimal("1500.00")
        );

        OffsetDateTime timestamp = OffsetDateTime.now();

        when(policyRepository.save(any(Policy.class)))
                .thenAnswer(invocation -> {
                    Policy policy = invocation.getArgument(0);

                    ReflectionTestUtils.setField(policy, "id", 1L);
                    policy.setCreatedAt(timestamp);
                    policy.setUpdatedAt(timestamp);

                    return policy;
                });

        PolicyResponse response = policyService.createPolicy(request);

        ArgumentCaptor<Policy> policyCaptor =
                ArgumentCaptor.forClass(Policy.class);

        verify(policyRepository).save(policyCaptor.capture());

        Policy savedPolicy = policyCaptor.getValue();

        assertEquals("POL-1001", savedPolicy.getPolicyNumber());
        assertEquals("Test User", savedPolicy.getHolderName());
        assertEquals("ACTIVE", savedPolicy.getStatus());
        assertEquals(new BigDecimal("1500.00"), savedPolicy.getPremium());

        assertEquals(1L, response.id());
        assertEquals("POL-1001", response.policyNumber());
        assertEquals(PolicyStatus.ACTIVE, response.status());
        assertFalse(response.hasDocument());

        verifyNoInteractions(s3Service, policyKafkaProducer);
    }
    @Test
    void getPolicyById_returnsPolicyResponse() {
        Long policyId = 1L;

        Policy policy = new Policy();
        ReflectionTestUtils.setField(policy, "id", policyId);

        policy.setPolicyNumber("POL-1001");
        policy.setHolderName("John Doe");
        policy.setStatus("ACTIVE");
        policy.setPremium(new BigDecimal("500.00"));
        policy.setDocumentKey("policies/1/document.pdf");

        when(policyRepository.findById(policyId))
                .thenReturn(Optional.of(policy));

        PolicyResponse response = policyService.getPolicyById(policyId);

        assertEquals(policyId, response.id());
        assertEquals("POL-1001", response.policyNumber());
        assertEquals("John Doe", response.holderName());
        assertEquals(PolicyStatus.ACTIVE, response.status());
        assertEquals(new BigDecimal("500.00"), response.premium());
        assertTrue(response.hasDocument());

        verify(policyRepository).findById(policyId);
        verifyNoInteractions(s3Service, policyKafkaProducer);
    }
    @Test
    void getPolicyById_whenPolicyDoesNotExist_throwsPolicyNotFoundException() {
        Long policyId = 999L;

        when(policyRepository.findById(policyId))
                .thenReturn(Optional.empty());

        PolicyNotFoundException exception = assertThrows(
                PolicyNotFoundException.class,
                () -> policyService.getPolicyById(policyId)
        );

        assertEquals(
                "Policy not found: " + policyId,
                exception.getMessage()
        );

        verify(policyRepository).findById(policyId);
        verifyNoInteractions(s3Service, policyKafkaProducer);
    }
    @Test
    void getAllPolicies_returnsPageOfPolicyResponses() {
        Policy firstPolicy = new Policy();
        ReflectionTestUtils.setField(firstPolicy, "id", 1L);
        firstPolicy.setPolicyNumber("POL-1001");
        firstPolicy.setHolderName("John Doe");
        firstPolicy.setStatus("ACTIVE");
        firstPolicy.setPremium(new BigDecimal("500.00"));
        firstPolicy.setDocumentKey("policies/1/document.pdf");

        Policy secondPolicy = new Policy();
        ReflectionTestUtils.setField(secondPolicy, "id", 2L);
        secondPolicy.setPolicyNumber("POL-1002");
        secondPolicy.setHolderName("Jane Doe");
        secondPolicy.setStatus("PENDING");
        secondPolicy.setPremium(new BigDecimal("700.00"));

        PageRequest pageable = PageRequest.of( 0,
                10,
                Sort.by("createdAt").descending()
        );

        Page<Policy> policyPage = new PageImpl<>(
                List.of(firstPolicy, secondPolicy),
                pageable,
                2
        );

        when(policyRepository.findAll(pageable))
                .thenReturn(policyPage);

        Page<PolicyResponse> responsePage =
                policyService.getAllPolicies(
                        0,
                        10,
                        null,
                        "createdAt",
                        "desc"
                );

        assertEquals(2, responsePage.getContent().size());
        assertEquals(2, responsePage.getTotalElements());
        assertEquals(0, responsePage.getNumber());
        assertEquals(10, responsePage.getSize());

        assertEquals(
                "POL-1001",
                responsePage.getContent().get(0).policyNumber()
        );

        assertTrue(
                responsePage.getContent().get(0).hasDocument()
        );

        assertFalse(
                responsePage.getContent().get(1).hasDocument()
        );

        verify(policyRepository).findAll(pageable);
        verifyNoInteractions(s3Service, policyKafkaProducer);
    }
    @Test
    void getPoliciesByStatus_returnsPageOfPolicyResponses() {
        String status = "ACTIVE";
        PageRequest pageable = PageRequest.of(0, 10);

        Policy policy = new Policy();
        ReflectionTestUtils.setField(policy, "id", 1L);
        policy.setPolicyNumber("POL-1001");
        policy.setHolderName("John Doe");
        policy.setStatus("ACTIVE");
        policy.setPremium(new BigDecimal("500.00"));
        policy.setDocumentKey("policies/1/document.pdf");

        Page<Policy> policyPage = new PageImpl<>(
                List.of(policy),
                pageable,
                1
        );

        when(policyRepository.findByStatusIgnoreCase(status, pageable))
                .thenReturn(policyPage);

        Page<PolicyResponse> responsePage =
                policyService.getPoliciesByStatus(status, 0, 10);

        assertEquals(1, responsePage.getTotalElements());
        assertEquals("POL-1001",
                responsePage.getContent().get(0).policyNumber());
        assertEquals(PolicyStatus.ACTIVE,
                responsePage.getContent().get(0).status());
        assertTrue(responsePage.getContent().get(0).hasDocument());

        verify(policyRepository)
                .findByStatusIgnoreCase(status, pageable);

        verifyNoInteractions(s3Service, policyKafkaProducer);
    }
    @Test
    void updatePolicy_updatesAllowedFieldsAndPreservesDocumentKey() {
        Long policyId = 1L;

        Policy existingPolicy = new Policy();
        ReflectionTestUtils.setField(existingPolicy, "id", policyId);
        existingPolicy.setPolicyNumber("POL-OLD");
        existingPolicy.setHolderName("Old Name");
        existingPolicy.setStatus("PENDING");
        existingPolicy.setPremium(new BigDecimal("400.00"));
        existingPolicy.setDocumentKey("policies/1/document.pdf");

        UpdatePolicyRequest request = new UpdatePolicyRequest(
                "POL-UPDATED",
                "John Doe",
                PolicyStatus.ACTIVE,
                new BigDecimal("800.00")
        );

        when(policyRepository.findById(policyId))
                .thenReturn(Optional.of(existingPolicy));

        when(policyRepository.save(existingPolicy))
                .thenReturn(existingPolicy);

        PolicyResponse response =
                policyService.updatePolicy(policyId, request);

        assertEquals("POL-UPDATED", existingPolicy.getPolicyNumber());
        assertEquals("John Doe", existingPolicy.getHolderName());
        assertEquals("ACTIVE", existingPolicy.getStatus());
        assertEquals(
                new BigDecimal("800.00"),
                existingPolicy.getPremium()
        );

        assertEquals(
                "policies/1/document.pdf",
                existingPolicy.getDocumentKey()
        );

        assertEquals(policyId, response.id());
        assertEquals("POL-UPDATED", response.policyNumber());
        assertEquals(PolicyStatus.ACTIVE, response.status());
        assertTrue(response.hasDocument());

        verify(policyRepository).findById(policyId);
        verify(policyRepository).save(existingPolicy);
        verifyNoInteractions(s3Service, policyKafkaProducer);
    }
    @Test
    void uploadPolicyDocument_uploadsFileAndReturnsPolicyResponse()
            throws IOException {

        Long policyId = 1L;

        Policy policy = new Policy();
        ReflectionTestUtils.setField(policy, "id", policyId);
        policy.setPolicyNumber("POL-1001");
        policy.setHolderName("John Doe");
        policy.setStatus("ACTIVE");
        policy.setPremium(new BigDecimal("500.00"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "policy.pdf",
                "application/pdf",
                "sample document".getBytes()
        );

        String documentKey = "policies/1/policy.pdf";

        when(policyRepository.findById(policyId))
                .thenReturn(Optional.of(policy));

        when(s3Service.uploadFile(file))
                .thenReturn(documentKey);

        when(policyRepository.save(policy))
                .thenReturn(policy);

        PolicyResponse response =
                policyService.uploadPolicyDocument(policyId, file);

        assertEquals(documentKey, policy.getDocumentKey());

        assertEquals(policyId, response.id());
        assertEquals("POL-1001", response.policyNumber());
        assertTrue(response.hasDocument());

        verify(policyRepository).findById(policyId);
        verify(s3Service).uploadFile(file);
        verify(policyRepository).save(policy);

        verify(policyKafkaProducer).publishDocumentUploaded(
                any(PolicyDocumentUploadedEvent.class)
        );

    }
    @Test
    void downloadPolicyDocument_whenDocumentDoesNotExist_throwsPolicyDocumentNotFoundException() {
        Long policyId = 10L;

        Policy policy = new Policy();
        policy.setDocumentKey(null);

        when(policyRepository.findById(policyId))
                .thenReturn(Optional.of(policy));

        PolicyDocumentNotFoundException exception = assertThrows(
                PolicyDocumentNotFoundException.class,
                () -> policyService.downloadPolicyDocument(policyId)
        );

        assertEquals(
                "No document found for policy: " + policyId,
                exception.getMessage()
        );

        verify(policyRepository).findById(policyId);
        verifyNoInteractions(s3Service, policyKafkaProducer);
    }
    @Test
    void createPolicy_whenPolicyNumberAlreadyExists_throwsDuplicatePolicyException() {
        String policyNumber = "POL-1001";

        CreatePolicyRequest request = new CreatePolicyRequest(
                policyNumber,
                "John Doe",
                PolicyStatus.ACTIVE,
                new BigDecimal("500.00")
        );

        when(policyRepository.existsByPolicyNumber(policyNumber))
                .thenReturn(true);

        DuplicatePolicyException exception = assertThrows(
                DuplicatePolicyException.class,
                () -> policyService.createPolicy(request)
        );

        assertEquals(
                "Policy already exists with policy number: " + policyNumber,
                exception.getMessage()
        );

        verify(policyRepository).existsByPolicyNumber(policyNumber);
        verify(policyRepository, never()).save(any());
    }
    @Test
    void getAllPolicies_whenSortAscending_createsCorrectPageable() {

        when(policyRepository.findAll(any(Pageable.class)))
                .thenReturn(Page.empty());

        policyService.getAllPolicies(
                0,
                20,
                null,
                "premium",
                "desc"
        );

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(policyRepository).findAll(pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        assertEquals(
                Sort.Direction.DESC,
                pageable.getSort().getOrderFor("premium").getDirection()
        );
    }
    @Test
    void getAllPolicies_whenPolicyNumberProvided_filtersByPolicyNumber() {

        when(policyRepository.findByPolicyNumber(
                eq("POL-12345"),
                any(Pageable.class)))
                .thenReturn(Page.empty());

        policyService.getAllPolicies(
                0,
                20,
                "POL-12345",
                "createdAt",
                "desc"
        );

        verify(policyRepository).findByPolicyNumber(
                eq("POL-12345"),
                any(Pageable.class)
        );

        verify(policyRepository, never())
                .findAll(any(Pageable.class));
    }
}