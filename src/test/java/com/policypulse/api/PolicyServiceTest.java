package com.policypulse.api;

import com.policypulse.api.policy.domain.PolicyStatus;
import com.policypulse.api.policy.dto.CreatePolicyRequest;
import com.policypulse.api.policy.dto.PolicyResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
}