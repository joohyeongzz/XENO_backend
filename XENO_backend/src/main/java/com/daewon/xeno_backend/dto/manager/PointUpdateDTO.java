package com.daewon.xeno_backend.dto.manager;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PointUpdateDTO {
    @JsonProperty("PointChange")
    private int pointChange;
}
