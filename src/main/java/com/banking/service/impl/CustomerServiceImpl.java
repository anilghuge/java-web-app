package com.banking.service.impl;

import com.banking.dto.CommonDtos.PageResponse;
import com.banking.dto.CustomerDtos;
import com.banking.entity.Account;
import com.banking.entity.Customer;
import com.banking.entity.Role;
import com.banking.entity.User;
import com.banking.enums.AccountStatus;
import com.banking.enums.KycStatus;
import com.banking.enums.NotificationType;
import com.banking.enums.RoleName;
import com.banking.exception.ApiException;
import com.banking.mapper.CustomerMapper;
import com.banking.repository.AccountRepository;
import com.banking.repository.CustomerRepository;
import com.banking.repository.RoleRepository;
import com.banking.repository.UserRepository;
import com.banking.security.SecurityUtils;
import com.banking.service.AuditService;
import com.banking.service.AuthenticatedUserService;
import com.banking.service.CustomerService;
import com.banking.service.NotificationService;
import com.banking.util.PaginationUtil;
import com.banking.util.ReferenceGenerator;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional
    public CustomerDtos.CustomerResponse create(CustomerDtos.CreateCustomerRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("EMAIL_EXISTS", "An account already exists with this email");
        }
        Role role = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> ApiException.badRequest("ROLE_NOT_CONFIGURED", "Customer role is not configured"));

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.addRole(role);
        userRepository.save(user);

        Customer customer = new Customer();
        customer.setCustomerNumber(generateUniqueCustomerNumber());
        customer.setUser(user);
        customer.setDateOfBirth(request.dateOfBirth());
        customer.setGender(request.gender());
        customer.setAddress(request.address());
        customer.setCity(request.city());
        customer.setState(request.state());
        customer.setPostalCode(request.postalCode());
        customer.setNationalId(request.nationalId());
        customer.setOccupation(request.occupation());
        customer.setAnnualIncome(request.annualIncome());
        customer.setKycStatus(KycStatus.PENDING);
        customer.setCountry("India");
        customerRepository.save(customer);

        notificationService.notify(user, "Welcome to Northstar Bank",
                "A customer profile has been created for you. Please complete KYC verification.",
                NotificationType.SYSTEM, "/profile");
        auditService.log("CUSTOMER_CREATED", "Customer", customer.getId(),
                "Customer " + customer.getCustomerNumber() + " created");
        return toResponse(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerDtos.CustomerResponse> list(String query, KycStatus kycStatus, String city,
                                                            int page, int size, String sortBy, String sortDir) {
        Specification<Customer> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));
            if (StringUtils.hasText(query)) {
                Join<Customer, User> user = root.join("user");
                String like = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("customerNumber")), like),
                        cb.like(cb.lower(user.get("firstName")), like),
                        cb.like(cb.lower(user.get("lastName")), like),
                        cb.like(cb.lower(user.get("email")), like),
                        cb.like(cb.lower(user.get("phone")), like)));
            }
            if (kycStatus != null) {
                predicates.add(cb.equal(root.get("kycStatus"), kycStatus));
            }
            if (StringUtils.hasText(city)) {
                predicates.add(cb.equal(cb.lower(root.get("city")), city.trim().toLowerCase()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Customer> customers = customerRepository.findAll(spec, customerPageable(page, size, sortBy, sortDir));
        List<CustomerDtos.CustomerResponse> content = customers.getContent().stream().map(this::toResponse).toList();
        return new PageResponse<>(content, customers.getNumber(), customers.getSize(),
                customers.getTotalElements(), customers.getTotalPages(), customers.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDtos.CustomerResponse get(Long id) {
        Customer customer = findCustomer(id);
        assertCanAccess(customer);
        return toResponse(customer);
    }

    @Override
    @Transactional
    public CustomerDtos.CustomerResponse update(Long id, CustomerDtos.UpdateCustomerRequest request) {
        Customer customer = findCustomer(id);
        assertCanAccess(customer);
        String previousNationalId = customer.getNationalId();
        User user = customer.getUser();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(request.phone());
        userRepository.save(user);

        customer.setDateOfBirth(request.dateOfBirth());
        customer.setGender(request.gender());
        customer.setAddress(request.address());
        customer.setCity(request.city());
        customer.setState(request.state());
        customer.setPostalCode(request.postalCode());
        customer.setNationalId(request.nationalId());
        customer.setOccupation(request.occupation());
        customer.setAnnualIncome(request.annualIncome());
        if (customer.getKycStatus() == KycStatus.VERIFIED && previousNationalId != null
                && !previousNationalId.equals(request.nationalId())) {
            customer.setKycStatus(KycStatus.PENDING);
            customer.setKycVerifiedAt(null);
            customer.setKycRemarks("KYC requires re-verification after national ID change");
        }
        customerRepository.save(customer);
        auditService.log("CUSTOMER_UPDATED", "Customer", customer.getId(), "Customer profile updated");
        return toResponse(customer);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Customer customer = findCustomer(id);
        BigDecimal balance = accountRepository.sumBalanceByCustomer(customer.getId());
        if (balance != null && balance.compareTo(BigDecimal.ZERO) > 0) {
            throw ApiException.conflict("CUSTOMER_HAS_BALANCE",
                    "Customer accounts must have zero balance before deletion");
        }
        List<Account> accounts = accountRepository.findByCustomerIdAndDeletedFalseOrderByOpenedAtDesc(customer.getId());
        accounts.forEach(account -> {
            account.setStatus(AccountStatus.CLOSED);
            account.setDeleted(true);
            accountRepository.save(account);
        });
        customer.setDeleted(true);
        customer.getUser().setEnabled(false);
        userRepository.save(customer.getUser());
        customerRepository.save(customer);
        auditService.log("CUSTOMER_DELETED", "Customer", customer.getId(),
                "Customer soft-deleted and login disabled");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerDtos.CustomerSummary> search(String query, int page, int size) {
        if (!StringUtils.hasText(query)) {
            throw ApiException.badRequest("QUERY_REQUIRED", "Search query is required");
        }
        Pageable pageable = PaginationUtil.pageable(page, size, "createdAt", "desc");
        Page<Customer> customers = customerRepository.search(query.trim(), pageable);
        return new PageResponse<>(
                customers.getContent().stream().map(CustomerMapper::toSummary).toList(),
                customers.getNumber(), customers.getSize(), customers.getTotalElements(),
                customers.getTotalPages(), customers.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDtos.CustomerResponse me() {
        return toResponse(authenticatedUserService.customer());
    }

    @Override
    @Transactional
    public CustomerDtos.CustomerResponse updateMe(CustomerDtos.UpdateCustomerRequest request) {
        return update(authenticatedUserService.customer().getId(), request);
    }

    @Override
    @Transactional
    public CustomerDtos.CustomerResponse submitKyc(CustomerDtos.KycSubmissionRequest request) {
        Customer customer = authenticatedUserService.customer();
        if (customer.getKycStatus() == KycStatus.VERIFIED) {
            throw ApiException.conflict("KYC_ALREADY_VERIFIED", "KYC is already verified");
        }
        customer.setNationalId(request.nationalId());
        customer.setAddress(request.address());
        customer.setCity(request.city());
        customer.setState(request.state());
        customer.setPostalCode(request.postalCode());
        customer.setOccupation(request.occupation());
        customer.setAnnualIncome(request.annualIncome());
        customer.setKycStatus(KycStatus.PENDING);
        customer.setKycRemarks("Submitted on " + Instant.now());
        customerRepository.save(customer);
        notificationService.notify(customer.getUser(), "KYC submitted",
                "Your KYC documents were submitted and are pending verification.",
                NotificationType.KYC, "/profile");
        auditService.log("KYC_SUBMITTED", "Customer", customer.getId(), "KYC documents submitted");
        return toResponse(customer);
    }

    @Override
    @Transactional
    public CustomerDtos.CustomerResponse decideKyc(Long customerId, CustomerDtos.KycDecisionRequest request) {
        if (request.status() == KycStatus.PENDING) {
            throw ApiException.badRequest("INVALID_KYC_DECISION", "Decision must be VERIFIED or REJECTED");
        }
        Customer customer = findCustomer(customerId);
        customer.setKycStatus(request.status());
        customer.setKycRemarks(request.remarks());
        customer.setKycVerifiedAt(request.status() == KycStatus.VERIFIED ? Instant.now() : null);
        customerRepository.save(customer);
        notificationService.notify(customer.getUser(),
                request.status() == KycStatus.VERIFIED ? "KYC verified" : "KYC rejected",
                request.status() == KycStatus.VERIFIED
                        ? "Your identity has been verified. All banking services are now available."
                        : "Your KYC verification was rejected. " + (request.remarks() == null ? "" : request.remarks()),
                NotificationType.KYC, "/profile");
        auditService.log("KYC_" + request.status().name(), "Customer", customer.getId(),
                "KYC decision: " + request.status());
        return toResponse(customer);
    }

    private Customer findCustomer(Long id) {
        return customerRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND", "Customer not found"));
    }

    private void assertCanAccess(Customer customer) {
        if (SecurityUtils.isStaff()) {
            return;
        }
        String email = SecurityUtils.currentEmail();
        if (email == null || !customer.getUser().getEmail().equalsIgnoreCase(email)) {
            throw ApiException.forbidden("CUSTOMER_ACCESS_DENIED", "You can only access your own customer profile");
        }
    }

    private CustomerDtos.CustomerResponse toResponse(Customer customer) {
        long count = accountRepository.findByCustomerIdAndDeletedFalseOrderByOpenedAtDesc(customer.getId()).size();
        BigDecimal balance = accountRepository.sumBalanceByCustomer(customer.getId());
        return CustomerMapper.toResponse(customer, count, balance == null ? BigDecimal.ZERO : balance);
    }

    private Pageable customerPageable(int page, int size, String sortBy, String sortDir) {
        String sortField = switch (sortBy == null ? "" : sortBy) {
            case "firstName" -> "user.firstName";
            case "lastName" -> "user.lastName";
            case "email" -> "user.email";
            case "customerNumber", "city", "kycStatus", "createdAt" -> sortBy;
            default -> "createdAt";
        };
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100),
                Sort.by(direction, sortField));
    }

    private String generateUniqueCustomerNumber() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String number = ReferenceGenerator.customerNumber();
            if (!customerRepository.existsByCustomerNumber(number)) {
                return number;
            }
        }
        throw new IllegalStateException("Unable to generate a unique customer number");
    }
}
