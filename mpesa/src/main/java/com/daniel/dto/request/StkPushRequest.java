package com.daniel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class StkPushRequest {

    @NotBlank
    @Pattern(regexp = "^254[0-9]{9}$", message = "Phone must be in MSISDN format (254XXXXXXXXX)")
    private String phoneNumber;

    @NotBlank
    @Pattern(regexp = "^[0-9]+(\\.[0-9]{1,2})?$", message = "Amount must be a valid number")
    private String amount;

    @NotBlank
    private String accountReference;

    @NotBlank
    private String description;
}