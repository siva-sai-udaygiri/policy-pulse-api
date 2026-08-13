package com.policypulse.api;

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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


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
}