package com.ems.config;

import com.ems.dao.EmployeeDao;
import com.ems.dao.UserDao;
import com.ems.entity.Employee;
import com.ems.entity.Role;
import com.ems.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Creates demo accounts and sample employees on first start. Disable with SEED_ENABLED=false. */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserDao userDao;
    private final EmployeeDao employeeDao;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserDao userDao, EmployeeDao employeeDao, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.employeeDao = employeeDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        createUserIfMissing("admin", "admin123", Role.ADMIN);
        createUserIfMissing("user", "user123", Role.USER);

        if (employeeDao.count(null, null) == 0) {
            seedEmployees();
            log.info("Seeded sample employees");
        }
    }

    private void createUserIfMissing(String username, String rawPassword, Role role) {
        if (!userDao.existsByUsername(username)) {
            userDao.save(new User(username, passwordEncoder.encode(rawPassword), role));
            log.info("Created demo user '{}' with role {}", username, role);
        }
    }

    private void seedEmployees() {
        Object[][] rows = {
                {"Aarav", "Sharma", "Engineering", "Software Engineer", "1200000", "2022-07-11"},
                {"Diya", "Patel", "Engineering", "Senior Software Engineer", "2100000", "2020-01-06"},
                {"Vihaan", "Mehta", "Engineering", "Engineering Manager", "3400000", "2018-03-19"},
                {"Ananya", "Iyer", "Engineering", "QA Engineer", "950000", "2023-02-13"},
                {"Arjun", "Reddy", "Engineering", "DevOps Engineer", "1500000", "2021-09-01"},
                {"Ishaan", "Gupta", "Product", "Product Manager", "2600000", "2019-11-25"},
                {"Saanvi", "Nair", "Product", "Associate Product Manager", "1400000", "2023-06-05"},
                {"Kabir", "Singh", "Design", "UI/UX Designer", "1100000", "2022-04-18"},
                {"Myra", "Joshi", "Design", "Design Lead", "2200000", "2019-08-12"},
                {"Reyansh", "Kulkarni", "Sales", "Account Executive", "900000", "2023-01-09"},
                {"Aadhya", "Desai", "Sales", "Sales Manager", "1800000", "2020-10-05"},
                {"Aryan", "Chopra", "Marketing", "Marketing Associate", "800000", "2024-03-04"},
                {"Kiara", "Bose", "Marketing", "Content Strategist", "950000", "2022-12-01"},
                {"Vivaan", "Rao", "Finance", "Financial Analyst", "1300000", "2021-05-17"},
                {"Navya", "Menon", "Finance", "Finance Manager", "2400000", "2018-07-23"},
                {"Atharv", "Pillai", "HR", "HR Executive", "750000", "2023-08-21"},
                {"Pari", "Verma", "HR", "HR Business Partner", "1600000", "2020-06-15"},
                {"Shaurya", "Malhotra", "Operations", "Operations Analyst", "850000", "2022-09-12"},
                {"Anika", "Saxena", "Operations", "Operations Manager", "1900000", "2019-04-08"},
                {"Dhruv", "Bhatt", "Engineering", "Data Engineer", "1650000", "2021-12-06"},
                {"Meera", "Kapoor", "Engineering", "Frontend Engineer", "1250000", "2022-10-03"},
                {"Rohan", "Agarwal", "Engineering", "Backend Engineer", "1350000", "2021-07-19"},
        };
        for (Object[] r : rows) {
            Employee e = new Employee();
            e.setFirstName((String) r[0]);
            e.setLastName((String) r[1]);
            e.setEmail((r[0] + "." + r[1] + "@company.com").toLowerCase());
            e.setPhone("98" + String.format("%08d", Math.floorMod((r[0] + (String) r[1]).hashCode(), 100_000_000)));
            e.setDepartment((String) r[2]);
            e.setDesignation((String) r[3]);
            e.setSalary(new BigDecimal((String) r[4]));
            e.setDateOfJoining(LocalDate.parse((String) r[5]));
            employeeDao.save(e);
        }
    }
}
