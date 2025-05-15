package com.featureflagx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.featureflagx.dto.FlagRequest;
import com.featureflagx.dto.FlagResponse;
import com.featureflagx.model.Flag;
import com.featureflagx.service.FlagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is; // Corrected line
import static org.hamcrest.Matchers.hasSize;

@WebMvcTest(com.featureflagx.controller.FlagController.class) // Specify the controller to test
public class FlagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FlagService flagService;

    @Autowired
    private ObjectMapper objectMapper; // For converting objects to JSON strings

    private Flag flag1;
    private FlagRequest flagRequest1;
    private FlagResponse flagResponse1;
    private final String FLAG_KEY_1 = "test-flag-1";

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        flag1 = new Flag();
        flag1.setKey(FLAG_KEY_1);
        flag1.setEnabled(true);
        flag1.setConfig("{ \"variant\": \"A\" }");
        flag1.setUpdatedAt(now);

        flagRequest1 = new FlagRequest();
        flagRequest1.setKey(FLAG_KEY_1);
        flagRequest1.setEnabled(true);
        flagRequest1.setConfig("{ \"variant\": \"A\" }");

        flagResponse1 = new FlagResponse();
        flagResponse1.setKey(FLAG_KEY_1);
        flagResponse1.setEnabled(true);
        flagResponse1.setConfig("{ \"variant\": \"A\" }");
        flagResponse1.setUpdatedAt(now);
    }

    @Test
    void createFlag_shouldReturnCreatedFlag() throws Exception {
        given(flagService.createFlag(any(FlagRequest.class))).willReturn(flag1);

        ResultActions response = mockMvc.perform(post("/flags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(flagRequest1)));

        response.andExpect(status().isCreated())
                .andExpect(jsonPath("$.key", is(flag1.getKey())))
                .andExpect(jsonPath("$.enabled", is(flag1.isEnabled())));
    }

    @Test
    void createFlag_whenKeyIsNull_shouldReturnBadRequest() throws Exception {
        FlagRequest badRequest = new FlagRequest(); // Key is null
        badRequest.setEnabled(true);

        ResultActions response = mockMvc.perform(post("/flags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badRequest)));

        response.andExpect(status().isBadRequest());
    }


    @Test
    void updateFlag_whenFlagExists_shouldReturnUpdatedFlag() throws Exception {
        given(flagService.updateFlag(anyString(), any(FlagRequest.class))).willReturn(Optional.of(flag1));

        ResultActions response = mockMvc.perform(put("/flags/{key}", FLAG_KEY_1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(flagRequest1)));

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.key", is(flag1.getKey())))
                .andExpect(jsonPath("$.enabled", is(flag1.isEnabled())));
    }

    @Test
    void updateFlag_whenFlagNotExists_shouldReturnNotFound() throws Exception {
        given(flagService.updateFlag(anyString(), any(FlagRequest.class))).willReturn(Optional.empty());

        ResultActions response = mockMvc.perform(put("/flags/{key}", "non-existent-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(flagRequest1)));

        response.andExpect(status().isNotFound());
    }

    @Test
    void deleteFlag_whenFlagExists_shouldReturnNoContent() throws Exception {
        given(flagService.deleteFlag(FLAG_KEY_1)).willReturn(true);

        ResultActions response = mockMvc.perform(delete("/flags/{key}", FLAG_KEY_1));

        response.andExpect(status().isNoContent());
    }

    @Test
    void deleteFlag_whenFlagNotExists_shouldReturnNotFound() throws Exception {
        given(flagService.deleteFlag("non-existent-key")).willReturn(false);

        ResultActions response = mockMvc.perform(delete("/flags/{key}", "non-existent-key"));

        response.andExpect(status().isNotFound());
    }

    @Test
    void getFlag_whenFlagExists_shouldReturnFlag() throws Exception {
        given(flagService.getFlag(FLAG_KEY_1)).willReturn(Optional.of(flag1));

        ResultActions response = mockMvc.perform(get("/flags/{key}", FLAG_KEY_1));

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.key", is(flag1.getKey())));
    }

    @Test
    void getFlag_whenFlagNotExists_shouldReturnNotFound() throws Exception {
        given(flagService.getFlag("non-existent-key")).willReturn(Optional.empty());

        ResultActions response = mockMvc.perform(get("/flags/{key}", "non-existent-key"));

        response.andExpect(status().isNotFound());
    }

    @Test
    void getAllFlags_shouldReturnListOfFlags() throws Exception {
        List<Flag> flags = Arrays.asList(flag1);
        given(flagService.getAllFlags()).willReturn(flags);

        ResultActions response = mockMvc.perform(get("/flags"));

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].key", is(flag1.getKey())));
    }

    @Test
    void evaluateFlag_shouldReturnEvaluationResult() throws Exception {
        given(flagService.isEnabled(FLAG_KEY_1, "user123")).willReturn(true);

        ResultActions response = mockMvc.perform(get("/flags/evaluate/{key}", FLAG_KEY_1)
                .param("targetId", "user123"));

        response.andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
     @Test
    void evaluateFlag_whenTargetIdNotProvided_shouldReturnEvaluationResult() throws Exception {
        given(flagService.isEnabled(FLAG_KEY_1, null)).willReturn(true);

        ResultActions response = mockMvc.perform(get("/flags/evaluate/{key}", FLAG_KEY_1));

        response.andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}

