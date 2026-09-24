package com.ems.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Payload for creating or updating an employee. The id comes from the URL, never the body. */
public record EmployeeRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must be at most 50 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must be at most 50 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 100, message = "Email must be at most 100 characters")
        String email,

        @Pattern(regexp = "^$|^[0-9+\\- ]{7,15}$", message = "Phone must be 7-15 digits")
        String phone,

        @NotBlank(message = "Department is required")
        @Size(max = 50, message = "Department must be at most 50 characters")
        String department,

        @NotBlank(message = "Designation is required")
        @Size(max = 50, message = "Designation must be at most 50 characters")
        String designation,

        @NotNull(message = "Salary is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Salary must be positive")
        @Digits(integer = 10, fraction = 2, message = "Salary can have at most 2 decimal places")
        BigDecimal salary,

        @NotNull(message = "Date of joining is required")
        @PastOrPresent(message = "Date of joining cannot be in the future")
        LocalDate dateOfJoining
) {
}
