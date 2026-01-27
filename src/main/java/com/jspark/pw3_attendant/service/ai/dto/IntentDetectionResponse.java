package com.jspark.pw3_attendant.service.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IntentDetectionResponse {

    @JsonProperty("intent")
    private String intent;

    @JsonProperty("params")
    private Map<String, Object> params;
}
