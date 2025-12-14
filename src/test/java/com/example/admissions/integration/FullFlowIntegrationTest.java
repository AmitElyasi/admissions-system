package com.example.admissions.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: starts the full Spring context and runs a happy-path admissions flow
 * using TestRestTemplate against the running application.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FullFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // helper method at class level
    private HttpEntity<Map<String, Object>> makePut(Map<String, Object> body) {
        return new HttpEntity<>(body, jsonHeaders());
    }

    @Test
    void happyPath_flow_leadsToAccepted() {
        // 1. Create user
        Map<String, String> createBody = Map.of("email", "inttest@example.com");
        ResponseEntity<Map<String, Object>> createResp = restTemplate.exchange(
                "/users",
                HttpMethod.POST,
                new HttpEntity<>(createBody, jsonHeaders()),
                MAP_TYPE
        );
        assertEquals(HttpStatus.OK, createResp.getStatusCode());
        String userId = (String) createResp.getBody().get("id");
        assertNotNull(userId);

        // 2. Personal details
        Map<String, Object> personalPayload = Map.of(
                "user_id", userId,
                "first_name", "Int",
                "last_name", "Tester",
                "email", "inttest@example.com",
                "timestamp", "2025-12-09T12:00:00Z"
        );
        Map<String, Object> personalBody = Map.of(
                "user_id", userId,
                "task_id", "personal_details",
                "task_payload", personalPayload
        );
        ResponseEntity<Map<String, Object>> r1 = restTemplate.exchange("/steps/complete", HttpMethod.PUT, makePut(personalBody), MAP_TYPE);
        assertEquals(HttpStatus.OK, r1.getStatusCode());

        // 3. IQ Test - passing (score > 75)
        Map<String, Object> iqPayload = Map.of("user_id", userId, "test_id", "iq-1", "score", 85, "timestamp", "now");
        Map<String, Object> iqBody = Map.of("user_id", userId, "task_id", "iq_test", "task_payload", iqPayload);
        ResponseEntity<Map<String, Object>> r2 = restTemplate.exchange("/steps/complete", HttpMethod.PUT, makePut(iqBody), MAP_TYPE);
        assertEquals(HttpStatus.OK, r2.getStatusCode());

        // 4. Schedule interview
        Map<String, Object> sched = Map.of("user_id", userId, "interview_date", "2025-12-15T10:00:00Z");
        Map<String, Object> schedBody = Map.of("user_id", userId, "task_id", "schedule_interview", "task_payload", sched);
        ResponseEntity<Map<String, Object>> r3 = restTemplate.exchange("/steps/complete", HttpMethod.PUT, makePut(schedBody), MAP_TYPE);
        assertEquals(HttpStatus.OK, r3.getStatusCode());

        // 5. Perform interview -> passed_interview
        Map<String, Object> perf = Map.of("user_id", userId, "interview_date", "2025-12-15T10:00:00Z", "interviewer_id", "int-1", "decision", "passed_interview");
        Map<String, Object> perfBody = Map.of("user_id", userId, "task_id", "perform_interview", "task_payload", perf);
        ResponseEntity<Map<String, Object>> r4 = restTemplate.exchange("/steps/complete", HttpMethod.PUT, makePut(perfBody), MAP_TYPE);
        assertEquals(HttpStatus.OK, r4.getStatusCode());

        // 6. Upload ID
        Map<String,Object> idPayload = Map.of("user_id", userId, "passport_number", "P0001", "timestamp", "now");
        Map<String,Object> idBody = Map.of("user_id", userId, "task_id", "upload_identification_document", "task_payload", idPayload);
        ResponseEntity<Map<String, Object>> r5 = restTemplate.exchange("/steps/complete", HttpMethod.PUT, makePut(idBody), MAP_TYPE);
        assertEquals(HttpStatus.OK, r5.getStatusCode());

        // 7. Sign contract
        Map<String,Object> signPayload = Map.of("user_id", userId, "timestamp", "now");
        Map<String,Object> signBody = Map.of("user_id", userId, "task_id", "sign_contract", "task_payload", signPayload);
        ResponseEntity<Map<String, Object>> r6 = restTemplate.exchange("/steps/complete", HttpMethod.PUT, makePut(signBody), MAP_TYPE);
        assertEquals(HttpStatus.OK, r6.getStatusCode());

        // 8. Payment
        Map<String,Object> payPayload = Map.of("user_id", userId, "payment_id", "pay-1", "timestamp", "now");
        Map<String,Object> payBody = Map.of("user_id", userId, "task_id", "payment", "task_payload", payPayload);
        ResponseEntity<Map<String, Object>> r7 = restTemplate.exchange("/steps/complete", HttpMethod.PUT, makePut(payBody), MAP_TYPE);
        assertEquals(HttpStatus.OK, r7.getStatusCode());

        // 9. Join Slack
        Map<String,Object> slackPayload = Map.of("user_id", userId, "email", "inttest@example.com", "timestamp", "now");
        Map<String,Object> slackBody = Map.of("user_id", userId, "task_id", "join_slack", "task_payload", slackPayload);
        ResponseEntity<Map<String, Object>> r8 = restTemplate.exchange("/steps/complete", HttpMethod.PUT, makePut(slackBody), MAP_TYPE);
        assertEquals(HttpStatus.OK, r8.getStatusCode());

        // 10. Check final status
        ResponseEntity<Map<String, Object>> statusResp = restTemplate.exchange(
                "/users/" + userId + "/status",
                HttpMethod.GET,
                null,
                MAP_TYPE
        );
        assertEquals(HttpStatus.OK, statusResp.getStatusCode());
        assertEquals("accepted", statusResp.getBody().get("status"));
    }

    @Test
    void createUser_duplicateEmail_returnsConflict() {
        Map<String, String> createBody = Map.of("email", "duplicate@example.com");
        
        // First creation should succeed
        ResponseEntity<Map<String, Object>> createResp1 = restTemplate.exchange(
                "/users",
                HttpMethod.POST,
                new HttpEntity<>(createBody, jsonHeaders()),
                MAP_TYPE
        );
        assertEquals(HttpStatus.OK, createResp1.getStatusCode());
        
        // Second creation with same email should fail
        ResponseEntity<Map<String, Object>> createResp2 = restTemplate.exchange(
                "/users",
                HttpMethod.POST,
                new HttpEntity<>(createBody, jsonHeaders()),
                MAP_TYPE
        );
        assertEquals(HttpStatus.CONFLICT, createResp2.getStatusCode());
        assertTrue(createResp2.getBody().get("error").toString().contains("already exists"));
    }

    @Test
    void createUser_duplicateEmailCaseInsensitive_returnsConflict() {
        Map<String, String> createBody1 = Map.of("email", "case@example.com");
        Map<String, String> createBody2 = Map.of("email", "CASE@example.com");
        
        // First creation
        ResponseEntity<Map<String, Object>> createResp1 = restTemplate.exchange(
                "/users",
                HttpMethod.POST,
                new HttpEntity<>(createBody1, jsonHeaders()),
                MAP_TYPE
        );
        assertEquals(HttpStatus.OK, createResp1.getStatusCode());
        
        // Second creation with different case should fail
        ResponseEntity<Map<String, Object>> createResp2 = restTemplate.exchange(
                "/users",
                HttpMethod.POST,
                new HttpEntity<>(createBody2, jsonHeaders()),
                MAP_TYPE
        );
        assertEquals(HttpStatus.CONFLICT, createResp2.getStatusCode());
    }

    @Test
    void createUser_invalidEmail_returnsBadRequest() {
        Map<String, String> createBody = Map.of("email", "not-an-email");
        
        ResponseEntity<Map<String, Object>> createResp = restTemplate.exchange(
                "/users",
                HttpMethod.POST,
                new HttpEntity<>(createBody, jsonHeaders()),
                MAP_TYPE
        );
        assertEquals(HttpStatus.BAD_REQUEST, createResp.getStatusCode());
    }

    @Test
    void getUserStatus_nonexistentUser_returnsNotFound() {
        ResponseEntity<Map<String, Object>> statusResp = restTemplate.exchange(
                "/users/99999/status",
                HttpMethod.GET,
                null,
                MAP_TYPE
        );
        assertEquals(HttpStatus.NOT_FOUND, statusResp.getStatusCode());
    }

    @Test
    void getCurrentPosition_nonexistentUser_returnsNotFound() {
        // Non-existent users should return 404
        ResponseEntity<Map<String, Object>> positionResp = restTemplate.exchange(
                "/users/99999/current",
                HttpMethod.GET,
                null,
                MAP_TYPE
        );
        assertEquals(HttpStatus.NOT_FOUND, positionResp.getStatusCode());
    }

    @Test
    void completeStep_invalidTaskId_returnsBadRequest() {
        Map<String, String> createBody = Map.of("email", "test@example.com");
        ResponseEntity<Map<String, Object>> createResp = restTemplate.exchange(
                "/users",
                HttpMethod.POST,
                new HttpEntity<>(createBody, jsonHeaders()),
                MAP_TYPE
        );
        String userId = (String) createResp.getBody().get("id");

        Map<String, Object> invalidBody = Map.of(
                "user_id", userId,
                "task_id", "non_existent_task",
                "task_payload", Map.of("user_id", userId)
        );
        
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/steps/complete",
                HttpMethod.PUT,
                makePut(invalidBody),
                MAP_TYPE
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void completeStep_missingRequiredFields_returnsBadRequest() {
        Map<String, String> createBody = Map.of("email", "test2@example.com");
        ResponseEntity<Map<String, Object>> createResp = restTemplate.exchange(
                "/users",
                HttpMethod.POST,
                new HttpEntity<>(createBody, jsonHeaders()),
                MAP_TYPE
        );
        String userId = (String) createResp.getBody().get("id");

        // Missing required fields for IQ Test
        Map<String, Object> invalidPayload = Map.of("user_id", userId, "test_id", "iq-1", "score", 85); // Missing timestamp
        Map<String, Object> invalidBody = Map.of(
                "user_id", userId,
                "task_id", "iq_test",
                "task_payload", invalidPayload
        );
        
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/steps/complete",
                HttpMethod.PUT,
                makePut(invalidBody),
                MAP_TYPE
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void flow_iqTestFailing_leadsToRejected() {
        // Create user
        Map<String, String> createBody = Map.of("email", "rejected@example.com");
        ResponseEntity<Map<String, Object>> createResp = restTemplate.exchange(
                "/users",
                HttpMethod.POST,
                new HttpEntity<>(createBody, jsonHeaders()),
                MAP_TYPE
        );
        String userId = (String) createResp.getBody().get("id");

        // Complete personal details
        Map<String, Object> personalPayload = Map.of(
                "user_id", userId,
                "first_name", "Test",
                "last_name", "User",
                "email", "rejected@example.com",
                "timestamp", "2025-12-09T12:00:00Z"
        );
        Map<String, Object> personalBody = Map.of(
                "user_id", userId,
                "task_id", "personal_details",
                "task_payload", personalPayload
        );
        restTemplate.exchange("/steps/complete", HttpMethod.PUT, makePut(personalBody), MAP_TYPE);

        // IQ Test - failing (score <= 75)
        Map<String, Object> iqPayload = Map.of("user_id", userId, "test_id", "iq-1", "score", 70, "timestamp", "now");
        Map<String, Object> iqBody = Map.of("user_id", userId, "task_id", "iq_test", "task_payload", iqPayload);
        ResponseEntity<Map<String, Object>> iqResp = restTemplate.exchange("/steps/complete", HttpMethod.PUT, makePut(iqBody), MAP_TYPE);
        assertEquals(HttpStatus.OK, iqResp.getStatusCode());
        
        // Check that tasks show as failed
        Object resultsObj = iqResp.getBody().get("results");
        assertNotNull(resultsObj);
        assertTrue(resultsObj instanceof java.util.List);
        
        // Check status is rejected
        ResponseEntity<Map<String, Object>> statusResp = restTemplate.exchange(
                "/users/" + userId + "/status",
                HttpMethod.GET,
                null,
                MAP_TYPE
        );
        assertEquals(HttpStatus.OK, statusResp.getStatusCode());
        assertEquals("rejected", statusResp.getBody().get("status"));
    }

    @Test
    void flow_interviewFailing_leadsToRejected() {
        // Create user
        Map<String, String> createBody = Map.of("email", "interviewfail@example.com");
        ResponseEntity<Map<String, Object>> createResp = restTemplate.exchange(
                "/users",
                HttpMethod.POST,
                new HttpEntity<>(createBody, jsonHeaders()),
                MAP_TYPE
        );
        String userId = (String) createResp.getBody().get("id");

        // Complete personal details
        Map<String, Object> personalPayload = Map.of(
                "user_id", userId,
                "first_name", "Test",
                "last_name", "User",
                "email", "interviewfail@example.com",
                "timestamp", "2025-12-09T12:00:00Z"
        );
        Map<String, Object> personalBody = Map.of(
                "user_id", userId,
                "task_id", "personal_details",
                "task_payload", personalPayload
        );
        restTemplate.exchange("/steps/complete", HttpMethod.PUT, makePut(personalBody), MAP_TYPE);

        // IQ Test - passing
        Map<String, Object> iqPayload = Map.of("user_id", userId, "test_id", "iq-1", "score", 85, "timestamp", "now");
        Map<String, Object> iqBody = Map.of("user_id", userId, "task_id", "iq_test", "task_payload", iqPayload);
        restTemplate.exchange("/steps/complete", HttpMethod.PUT, makePut(iqBody), MAP_TYPE);

        // Schedule interview
        Map<String, Object> sched = Map.of("user_id", userId, "interview_date", "2025-12-15T10:00:00Z");
        Map<String, Object> schedBody = Map.of("user_id", userId, "task_id", "schedule_interview", "task_payload", sched);
        restTemplate.exchange("/steps/complete", HttpMethod.PUT, makePut(schedBody), MAP_TYPE);

        // Perform interview -> failed (not passed_interview)
        Map<String, Object> perf = Map.of("user_id", userId, "interview_date", "2025-12-15T10:00:00Z", "interviewer_id", "int-1", "decision", "failed_interview");
        Map<String, Object> perfBody = Map.of("user_id", userId, "task_id", "perform_interview", "task_payload", perf);
        restTemplate.exchange("/steps/complete", HttpMethod.PUT, makePut(perfBody), MAP_TYPE);

        // Check status is rejected
        ResponseEntity<Map<String, Object>> statusResp = restTemplate.exchange(
                "/users/" + userId + "/status",
                HttpMethod.GET,
                null,
                MAP_TYPE
        );
        assertEquals(HttpStatus.OK, statusResp.getStatusCode());
        assertEquals("rejected", statusResp.getBody().get("status"));
    }

    @Test
    void getFlow_returnsCompleteFlowStructure() {
        ResponseEntity<Map<String, Object>> flowResp = restTemplate.exchange(
                "/flow",
                HttpMethod.GET,
                null,
                MAP_TYPE
        );
        assertEquals(HttpStatus.OK, flowResp.getStatusCode());
        assertNotNull(flowResp.getBody().get("id"));
        assertNotNull(flowResp.getBody().get("name"));
        assertNotNull(flowResp.getBody().get("steps"));
    }

    @Test
    void completeStep_taskOrderViolation_returnsBadRequest() {
        Map<String, String> createBody = Map.of("email", "order@example.com");
        ResponseEntity<Map<String, Object>> createResp = restTemplate.exchange(
                "/users",
                HttpMethod.POST,
                new HttpEntity<>(createBody, jsonHeaders()),
                MAP_TYPE
        );
        String userId = (String) createResp.getBody().get("id");

        // Try to perform interview before scheduling it
        Map<String, Object> performPayload = Map.of(
                "user_id", userId,
                "interview_date", "2025-12-15T10:00:00Z",
                "interviewer_id", "int-1",
                "decision", "passed_interview"
        );
        Map<String, Object> performBody = Map.of(
                "user_id", userId,
                "task_id", "perform_interview",
                "task_payload", performPayload
        );
        
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/steps/complete",
                HttpMethod.PUT,
                makePut(performBody),
                MAP_TYPE
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().get("error").toString().contains("Cannot complete task"));
    }

    @Test
    void completeStep_alreadyCompletedNonRedoableTask_returnsBadRequest() {
        Map<String, String> createBody = Map.of("email", "already@example.com");
        ResponseEntity<Map<String, Object>> createResp = restTemplate.exchange(
                "/users",
                HttpMethod.POST,
                new HttpEntity<>(createBody, jsonHeaders()),
                MAP_TYPE
        );
        String userId = (String) createResp.getBody().get("id");

        // Complete personal details first (required for IQ test)
        Map<String, Object> personalPayload = Map.of(
                "user_id", userId,
                "first_name", "Test",
                "last_name", "User",
                "email", "already@example.com",
                "timestamp", "2025-12-10T12:00:00Z"
        );
        Map<String, Object> personalBody = Map.of(
                "user_id", userId,
                "task_id", "personal_details",
                "task_payload", personalPayload
        );
        restTemplate.exchange("/steps/complete", HttpMethod.PUT, makePut(personalBody), MAP_TYPE);

        // Complete IQ test (non-redoable) first time
        Map<String, Object> iqPayload = Map.of(
                "user_id", userId,
                "test_id", "iq-1",
                "score", 85,
                "timestamp", "2025-12-10T12:00:00Z"
        );
        Map<String, Object> iqBody = Map.of(
                "user_id", userId,
                "task_id", "iq_test",
                "task_payload", iqPayload
        );
        restTemplate.exchange("/steps/complete", HttpMethod.PUT, makePut(iqBody), MAP_TYPE);

        // Try to complete IQ test again - should fail (not redoable)
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/steps/complete",
                HttpMethod.PUT,
                makePut(iqBody),
                MAP_TYPE
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().get("error").toString().contains("already been completed"));
    }

    @Test
    void completeStep_redoableTask_allowsMultipleCompletions() {
        Map<String, String> createBody = Map.of("email", "redoable@example.com");
        ResponseEntity<Map<String, Object>> createResp = restTemplate.exchange(
                "/users",
                HttpMethod.POST,
                new HttpEntity<>(createBody, jsonHeaders()),
                MAP_TYPE
        );
        String userId = (String) createResp.getBody().get("id");

        // Complete personal details (redoable) first time
        Map<String, Object> personalPayload = Map.of(
                "user_id", userId,
                "first_name", "Test",
                "last_name", "User",
                "email", "redoable@example.com",
                "timestamp", "2025-12-10T12:00:00Z"
        );
        Map<String, Object> personalBody = Map.of(
                "user_id", userId,
                "task_id", "personal_details",
                "task_payload", personalPayload
        );
        ResponseEntity<Map<String, Object>> response1 = restTemplate.exchange(
                "/steps/complete",
                HttpMethod.PUT,
                makePut(personalBody),
                MAP_TYPE
        );
        assertEquals(HttpStatus.OK, response1.getStatusCode());

        // Complete again - should work (redoable)
        ResponseEntity<Map<String, Object>> response2 = restTemplate.exchange(
                "/steps/complete",
                HttpMethod.PUT,
                makePut(personalBody),
                MAP_TYPE
        );
        assertEquals(HttpStatus.OK, response2.getStatusCode());
    }
}