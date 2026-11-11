package com.banking.entity;

import com.banking.enums.Gender;
import com.banking.enums.KycStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_customers_number", columnList = "customer_number", unique = true),
        @Index(name = "idx_customers_user", columnList = "user_id", unique = true)
})
public class Customer extends BaseEntity {

    @Column(name = "customer_number", nullable = false, unique = true, length = 20)
    private String customerNumber;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Column(length = 255)
    private String address;

    @Column(length = 80)
    private String city;

    @Column(length = 80)
    private String state;

    @Column(length = 12)
    private String postalCode;

    @Column(length = 80)
    private String country = "India";

    @Column(name = "national_id", length = 30)
    private String nationalId;

    @Column(length = 100)
    private String occupation;

    @Column(precision = 19, scale = 2)
    private BigDecimal annualIncome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KycStatus kycStatus = KycStatus.PENDING;

    private Instant kycVerifiedAt;

    @Column(length = 255)
    private String kycRemarks;

    @Column(nullable = false)
    private boolean deleted = false;
}
