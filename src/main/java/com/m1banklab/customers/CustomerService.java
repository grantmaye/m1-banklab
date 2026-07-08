package com.m1banklab.customers;

import com.m1banklab.audit.AuditService;
import com.m1banklab.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final AuditService auditService;

    public CustomerService(CustomerRepository customerRepository, AuditService auditService) {
        this.customerRepository = customerRepository;
        this.auditService = auditService;
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        Customer customer = customerRepository.save(new Customer(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone()
        ));
        auditService.record("CUSTOMER_CREATED", "CUSTOMER", customer.getId(), "api", "Customer profile created.");
        return CustomerResponse.from(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(UUID id) {
        return customerRepository.findById(id)
                .map(CustomerResponse::from)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + id));
    }
}
