package com.ems.dto;

import com.ems.entity.Employee;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** What the API returns for an employee, decoupled from the JPA entity. */
public record EmployeeResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String department,
        String designation,
        BigDecimal salary,
        LocalDate dateOfJoining,
        Instant createdAt,
        Instant updatedAt
) {
    public static EmployeeResponse from(Employee e) {
        return new EmployeeResponse(
                e.getId(), e.getFirstName(), e.getLastName(), e.getEmail(), e.getPhone(),
                e.getDepartment(), e.getDesignation(), e.getSalary(), e.getDateOfJoining(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
