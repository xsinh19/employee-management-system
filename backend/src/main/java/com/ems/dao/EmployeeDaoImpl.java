package com.ems.dao;

import com.ems.entity.Employee;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JPA implementation of {@link EmployeeDao} built on the {@link EntityManager}.
 * All values go through bound parameters; the ORDER BY column is checked against a
 * whitelist because JPQL cannot bind identifiers as parameters.
 */
@Repository
public class EmployeeDaoImpl implements EmployeeDao {

    private final EntityManager entityManager;

    public EmployeeDaoImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Employee> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Employee.class, id));
    }

    @Override
    public List<Employee> search(String keyword, String department,
                                 int page, int size, String sortField, boolean ascending) {
        // Defence in depth: the service validates too, but this method builds JPQL text
        if (!SORTABLE_FIELDS.contains(sortField)) {
            throw new IllegalArgumentException("Cannot sort by '" + sortField + "'");
        }
        Map<String, Object> params = new HashMap<>();
        String jpql = "SELECT e FROM Employee e" + buildWhere(keyword, department, params)
                + " ORDER BY e." + sortField + (ascending ? " ASC" : " DESC") + ", e.id ASC";

        TypedQuery<Employee> query = entityManager.createQuery(jpql, Employee.class);
        params.forEach(query::setParameter);
        return query.setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    @Override
    public long count(String keyword, String department) {
        Map<String, Object> params = new HashMap<>();
        String jpql = "SELECT COUNT(e) FROM Employee e" + buildWhere(keyword, department, params);

        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        params.forEach(query::setParameter);
        return query.getSingleResult();
    }

    @Override
    public List<String> findDistinctDepartments() {
        return entityManager.createQuery(
                        "SELECT DISTINCT e.department FROM Employee e ORDER BY e.department", String.class)
                .getResultList();
    }

    @Override
    public boolean existsByEmail(String email) {
        return entityManager.createQuery(
                        "SELECT COUNT(e) FROM Employee e WHERE LOWER(e.email) = LOWER(:email)", Long.class)
                .setParameter("email", email)
                .getSingleResult() > 0;
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, Long id) {
        return entityManager.createQuery(
                        "SELECT COUNT(e) FROM Employee e WHERE LOWER(e.email) = LOWER(:email) AND e.id <> :id",
                        Long.class)
                .setParameter("email", email)
                .setParameter("id", id)
                .getSingleResult() > 0;
    }

    @Override
    public Employee save(Employee employee) {
        if (employee.getId() == null) {
            entityManager.persist(employee);
            return employee;
        }
        Employee merged = entityManager.merge(employee);
        // Flush now so @PreUpdate runs (fresh updatedAt in the response) and any
        // constraint violation surfaces here rather than at commit time.
        entityManager.flush();
        return merged;
    }

    @Override
    public void delete(Employee employee) {
        entityManager.remove(entityManager.contains(employee) ? employee : entityManager.merge(employee));
    }

    /** Builds the shared WHERE clause for search and count, collecting bound parameters. */
    private String buildWhere(String keyword, String department, Map<String, Object> params) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (LOWER(e.firstName) LIKE :kw OR LOWER(e.lastName) LIKE :kw")
                    .append(" OR LOWER(e.email) LIKE :kw OR LOWER(e.designation) LIKE :kw)");
            params.put("kw", "%" + keyword.trim().toLowerCase() + "%");
        }
        if (department != null && !department.isBlank()) {
            where.append(" AND e.department = :dept");
            params.put("dept", department.trim());
        }
        return where.toString();
    }
}
