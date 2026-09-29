package com.dentist.rendez_vous.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatientRequestDto {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must contain between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9+\\s-]{8,20}$", message = "Invalid phone number format")
    private String phone;
}