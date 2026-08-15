package com.policypulse.api;

import com.policypulse.api.policy.dto.PolicyResponse;
import com.policypulse.api.policy.exception.GlobalExceptionHandler;
import com.policypulse.api.policy.exception.PolicyDocumentNotFoundException;
import com.policypulse.api.policy.exception.PolicyNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;


import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.policypulse.api.policy.dto.CreatePolicyRequest;
import com.policypulse.api.policy.exception.DuplicatePolicyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;


@WebMvcTest(PolicyController.class)
@Import(GlobalExceptionHandler.class)
class PolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PolicyService policyService;

    @Test
    void getPolicyById_whenPolicyDoesNotExist_returns404() throws Exception {

        when(policyService.getPolicyById(999L))
                .thenThrow(new PolicyNotFoundException(999L));

        mockMvc.perform(get("/api/policies/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Policy not found: 999"));
    }
    @Test
    void downloadPolicyDocument_whenDocumentDoesNotExist_returns404() throws Exception {
        Long policyId = 10L;

        when(policyService.downloadPolicyDocument(policyId))
                .thenThrow(new PolicyDocumentNotFoundException(policyId));

        mockMvc.perform(get("/api/policies/10/document"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("No document found for policy: 10"));
    }
    @Test
    void createPolicy_whenRequestIsInvalid_returns400ValidationResponse() throws Exception {

        String requestBody = """
            {
              "policyNumber": "",
              "holderName": "",
              "status": null,
              "premium": null
            }
            """;

        mockMvc.perform(post("/api/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.policyNumber")
                        .value("Policy number is required"))
                .andExpect(jsonPath("$.fieldErrors.holderName")
                        .value("Holder name is required"))
                .andExpect(jsonPath("$.fieldErrors.status")
                        .value("Policy status is required"))
                .andExpect(jsonPath("$.fieldErrors.premium")
                        .value("Premium is required"));
    }
    @Test
    void createPolicy_whenPolicyNumberAlreadyExists_returns409() throws Exception {

        when(policyService.createPolicy(any(CreatePolicyRequest.class)))
                .thenThrow(new DuplicatePolicyException("POL-1001"));

        String requestBody = """
            {
              "policyNumber": "POL-1001",
              "holderName": "John Doe",
              "status": "ACTIVE",
              "premium": 500.00
            }
            """;

        mockMvc.perform(post("/api/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Policy already exists with policy number: POL-1001"));
    }
    @Test
    void getAllPolicies_whenPageIsNegative_returns400() throws Exception {

        mockMvc.perform(get("/api/policies")
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(policyService);
    }
    @Test
    void getAllPolicies_whenSizeIsZero_returns400() throws Exception {
        mockMvc.perform(get("/api/policies")
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(policyService);
    }
    @Test
    void getAllPolicies_whenSizeExceedsMaximum_returns400() throws Exception {
        mockMvc.perform(get("/api/policies")
                        .param("page", "0")
                        .param("size", "101"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(policyService);
    }
    @Test
    void getAllPolicies_whenSizeIsMaximum_returns200() throws Exception {

        when(policyService.getAllPolicies(0, 100))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/policies")
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isOk());

        verify(policyService).getAllPolicies(0, 100);
    }
}