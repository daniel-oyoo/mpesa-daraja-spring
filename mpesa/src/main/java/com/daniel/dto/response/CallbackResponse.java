package com.daniel.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CallbackResponse {
    @JsonProperty("Body")
    private Body body;

    @Data
    public static class Body {
        private JsonNode stkCallback;
    }
}