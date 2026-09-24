package com.ems.service;

import com.ems.dao.EmployeeDao;
import com.ems.dto.EmployeeRequest;
import com.ems.dto.EmployeeResponse;
import com.ems.dto.PageResponse;
import com.ems.entity.Employee;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for employees: input normalisation, uniqueness rules, not-found handling
 * and entity/DTO mapping. Owns the transaction boundary for every operation.
 */
@Service
@Transactional(readOnly = true)
public class EmployeeServiceImpl implements EmployeeService {

    static final int MAX_PAGE_SIZE = 100;

    private final EmployeeDao employeeDao;

    public EmployeeServiceImpl(EmployeeDao employeeDao) {
        this.employeeDao = employeeDao;
    }

    @Override
    public PageResponse<EmployeeResponse> getEmployees(String keyword, String department,
                                                       int page, int size, String sortBy, String direction) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (!EmployeeDao.SORTABLE_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Cannot sort by '" + sortBy + "'. Allowed: "
                    + EmployeeDao.SORTABLE_FIELDS.stream().sorted().toList());
        }
        boolean ascending = !"desc".equalsIgnoreCase(direction);

        List<EmployeeResponse> content = employeeDao
                .search(keyword, department, page, size, sortBy, ascending)
                .stream()
                .map(EmployeeResponse::from)
                .toList();
        long total = employeeDao.count(keyword, department);
        return PageResponse.of(content, page, size, total);
    }

    @Override
    public EmployeeResponse getEmployee(Long id) {
        return EmployeeResponse.from(findOrThrow(id));
    }

    @Override
    public List<String> getDepartments() {
        return employeeDao.findDistinctDepartments();
    }

    @Override
    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        String email = normaliseEmail(request.email());
        if (employeeDao.existsByEmail(email)) {
            throw new DuplicateResourceException("An employee with email '" + email + "' already exists");
        }
        Employee employee = new Employee();
        applyRequest(employee, request, email);
        return EmployeeResponse.from(employeeDao.save(employee));
    }

    @Override
    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = findOrThrow(id);
        String email = normaliseEmail(request.email());
        if (employeeDao.existsByEmailAndIdNot(email, id)) {
            throw new DuplicateResourceException("An employee with email '" + email + "' already exists");
        }
        applyRequest(employee, request, email);
        return EmployeeResponse.from(employeeDao.save(employee));
    }

    @Override
    @Transactional
    public void deleteEmployee(Long id) {
        employeeDao.delete(findOrThrow(id));
    }

    private Employee findOrThrow(Long id) {
        return employeeDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + id));
    }

    private static String normaliseEmail(String email) {
        return email.trim().toLowerCase();
    }

    private static void applyRequest(Employee employee, EmployeeRequest request, String email) {
        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        employee.setEmail(email);
        employee.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        employee.setDepartment(request.department().trim());
        employee.setDesignation(request.designation().trim());
        employee.setSalary(request.salary());
        employee.setDateOfJoining(request.dateOfJoining());
    }
}
