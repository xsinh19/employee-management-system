package com.ems.controller;

import com.ems.config.SecurityConfig;
import com.ems.dto.EmployeeResponse;
import com.ems.dto.PageResponse;
import com.ems.exception.ResourceNotFoundException;
import com.ems.security.JsonSecurityErrorHandler;
import com.ems.security.JwtService;
import com.ems.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer slice test: real controllers, validation, exception handler and security rules;
 * the service is mocked. Verifies HTTP status codes and role-based access.
 */
@WebMvcTest(EmployeeController.class)
@Import({SecurityConfig.class, JsonSecurityErrorHandler.class})
class EmployeeControllerTest {

    private static final String VALID_BODY = """
            {"firstName":"Asha","lastName":"Rao","email":"asha@company.com","phone":"9876543210",
             "department":"Engineering","designation":"Engineer","salary":1000000,
             "dateOfJoining":"2024-01-15"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    private static EmployeeResponse sample() {
        return new EmployeeResponse(1L, "Asha", "Rao", "asha@company.com", "9876543210", "Engineering",
                "Engineer", new BigDecimal("1000000"), LocalDate.of(2024, 1, 15), Instant.now(), Instant.now());
    }

    @Test
    void anonymousRequest_isRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(roles = "USER")
    void user_canListEmployees() throws Exception {
        when(employeeService.getEmployees(any(), any(), eq(0), eq(10), eq("id"), eq("asc")))
                .thenReturn(PageResponse.of(List.of(sample()), 0, 10, 1));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("asha@company.com"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void user_cannotCreateEmployee() throws Exception {
        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(employeeService);
    }

    @Test
    @WithMockUser(roles = "USER")
    void user_cannotDeleteEmployee() throws Exception {
        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(employeeService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_createsEmployee_returns201WithLocation() throws Exception {
        when(employeeService.createEmployee(any())).thenReturn(sample());

        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/employees/1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidBody_returns400WithFieldErrors() throws Exception {
        String body = """
                {"firstName":"","lastName":"Rao","email":"not-an-email","department":"Engineering",
                 "designation":"Engineer","salary":-5,"dateOfJoining":"2024-01-15"}
                """;

        mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.firstName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").value("Email must be valid"))
                .andExpect(jsonPath("$.fieldErrors.salary").exists());
        verifyNoInteractions(employeeService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void missingEmployee_returns404() throws Exception {
        when(employeeService.getEmployee(42L)).thenThrow(new ResourceNotFoundException("Employee not found with id 42"));

        mockMvc.perform(get("/api/employees/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found with id 42"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_deletesEmployee_returns204() throws Exception {
        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isNoContent());
        verify(employeeService).deleteEmployee(1L);
    }
}
