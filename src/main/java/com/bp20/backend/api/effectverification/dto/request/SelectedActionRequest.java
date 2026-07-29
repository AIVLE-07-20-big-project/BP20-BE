package com.bp20.backend.api.effectverification.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SelectedActionRequest {

    @NotBlank
    @Size(max = 100)
    @JsonProperty("\uBC29\uC548")
    private String action;

    @Size(max = 100)
    private String axis;
}
