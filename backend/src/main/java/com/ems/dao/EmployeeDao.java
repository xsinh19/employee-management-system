package com.ems.dao;

import com.ems.entity.Employee;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Data-access contract for employees. The service layer depends on this interface only,
 * so the persistence technology can change without touching business logic.
 */
public interface EmployeeDao {

    /** Entity fields the list endpoint may be ordered by. */
    Set<String> SORTABLE_FIELDS = Set.of(
            "id", "firstName", "lastName", "email", "department",
            "designation", "salary", "dateOfJoining");

    Optional<Employee> findById(Long id);

    /**
     * Returns one page of employees matching an optional keyword (name/email/designation)
     * and an optional department, ordered by a whitelisted field.
     */
    List<Employee> search(String keyword, String department,
                          int page, int size, String sortField, boolean ascending);

    long count(String keyword, String department);

    List<String> findDistinctDepartments();

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Employee save(Employee employee);

    void delete(Employee employee);
}
