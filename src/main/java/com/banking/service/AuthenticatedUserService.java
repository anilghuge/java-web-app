package com.banking.service;

import com.banking.entity.Customer;
import com.banking.entity.Employee;
import com.banking.entity.User;
import com.banking.exception.ApiException;
import com.banking.repository.CustomerRepository;
import com.banking.repository.EmployeeRepository;
import com.banking.repository.UserRepository;
import com.banking.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticatedUserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;

    public User user() {
        String email = SecurityUtils.currentEmail();
        if (email == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Authentication is required");
        }
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.unauthorized("USER_NOT_FOUND", "Authenticated user no longer exists"));
    }

    public Customer customer() {
        User user = user();
        return customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("CUSTOMER_PROFILE_MISSING",
                        "No customer profile is linked to this user"));
    }

    public Employee employee() {
        User user = user();
        return employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("EMPLOYEE_PROFILE_MISSING",
                        "No employee profile is linked to this user"));
    }

    public boolean isStaff() {
        return SecurityUtils.isStaff();
    }
}
