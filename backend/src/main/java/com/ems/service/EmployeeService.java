package com.ems.service;

import com.ems.dto.EmployeeRequest;
import com.ems.dto.EmployeeResponse;
import com.ems.dto.PageResponse;

import java.util.List;

public interface EmployeeService {

    PageResponse<EmployeeResponse> getEmployees(String keyword, String department,
                                                int page, int size, String sortBy, String direction);

    EmployeeResponse getEmployee(Long id);

    List<String> getDepartments();

    EmployeeResponse createEmployee(EmployeeRequest request);

    EmployeeResponse updateEmployee(Long id, EmployeeRequest request);

    void deleteEmployee(Long id);
}
