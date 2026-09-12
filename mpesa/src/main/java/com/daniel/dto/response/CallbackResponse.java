package com.daniel.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class CallbackResponse {
    private Body body;

    @Data
    public static class Body {
        private JsonNode stkCallback;
    }
}