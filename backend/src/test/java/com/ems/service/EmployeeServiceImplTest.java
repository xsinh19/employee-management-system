package com.ems.service;

import com.ems.dao.EmployeeDao;
import com.ems.dto.EmployeeRequest;
import com.ems.dto.EmployeeResponse;
import com.ems.dto.PageResponse;
import com.ems.entity.Employee;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Pure unit tests: the DAO is mocked, so only business rules are exercised. */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeDao employeeDao;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private static EmployeeRequest request(String email) {
        return new EmployeeRequest("  Asha ", "Rao", email, "", "Engineering", "Engineer",
                new BigDecimal("1000000"), LocalDate.of(2024, 1, 15));
    }

    private static Employee employee(Long id, String email) {
        Employee e = new Employee();
        e.setId(id);
        e.setFirstName("Asha");
        e.setLastName("Rao");
        e.setEmail(email);
        e.setDepartment("Engineering");
        e.setDesignation("Engineer");
        e.setSalary(new BigDecimal("1000000"));
        e.setDateOfJoining(LocalDate.of(2024, 1, 15));
        return e;
    }

    @Test
    void createEmployee_normalisesInputAndSaves() {
        when(employeeDao.existsByEmail("asha.rao@company.com")).thenReturn(false);
        when(employeeDao.save(any(Employee.class))).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        EmployeeResponse response = employeeService.createEmployee(request("  Asha.Rao@Company.com "));

        ArgumentCaptor<Employee> saved = ArgumentCaptor.forClass(Employee.class);
        verify(employeeDao).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("asha.rao@company.com");
        assertThat(saved.getValue().getFirstName()).isEqualTo("Asha");
        assertThat(saved.getValue().getPhone()).isNull();
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void createEmployee_rejectsDuplicateEmail() {
        when(employeeDao.existsByEmail("asha.rao@company.com")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.createEmployee(request("asha.rao@company.com")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(employeeDao, never()).save(any());
    }

    @Test
    void updateEmployee_throwsWhenMissing() {
        when(employeeDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.updateEmployee(99L, request("x@company.com")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateEmployee_rejectsEmailTakenByAnotherEmployee() {
        when(employeeDao.findById(1L)).thenReturn(Optional.of(employee(1L, "asha.rao@company.com")));
        when(employeeDao.existsByEmailAndIdNot("taken@company.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> employeeService.updateEmployee(1L, request("taken@company.com")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void deleteEmployee_deletesExisting() {
        Employee existing = employee(5L, "a@company.com");
        when(employeeDao.findById(5L)).thenReturn(Optional.of(existing));

        employeeService.deleteEmployee(5L);

        verify(employeeDao).delete(existing);
    }

    @Test
    void deleteEmployee_throwsWhenMissing() {
        when(employeeDao.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.deleteEmployee(5L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(employeeDao, never()).delete(any());
    }

    @Test
    void getEmployees_buildsPageMetadata() {
        when(employeeDao.search("asha", null, 1, 2, "firstName", false))
                .thenReturn(List.of(employee(3L, "c@company.com")));
        when(employeeDao.count("asha", null)).thenReturn(5L);

        PageResponse<EmployeeResponse> page =
                employeeService.getEmployees("asha", null, 1, 2, "firstName", "DESC");

        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(5);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    @Test
    void getEmployees_rejectsInvalidPageSize() {
        assertThatThrownBy(() -> employeeService.getEmployees(null, null, 0, 500, "id", "asc"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(employeeDao);
    }
}
