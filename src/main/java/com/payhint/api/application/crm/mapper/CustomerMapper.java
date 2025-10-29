package com.payhint.api.application.crm.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.payhint.api.application.crm.dto.request.CreateCustomerRequest;
import com.payhint.api.application.crm.dto.response.CustomerResponse;
import com.payhint.api.application.shared.ValueObjectMapper;
import com.payhint.api.domain.crm.model.Customer;
import com.payhint.api.domain.crm.valueobjects.UserId;

@Mapper(componentModel = "spring", uses = { ValueObjectMapper.class })
public interface CustomerMapper {

    @Mapping(source = "contactEmail", target = "contactEmail")
    List<CustomerResponse> toResponseList(List<Customer> customers);

    @Mapping(source = "contactEmail", target = "contactEmail")
    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "id", target = "id")
    CustomerResponse toResponse(Customer customer);

    @Mapping(source = "request.contactEmail", target = "contactEmail")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Customer toDomain(UserId userId, CreateCustomerRequest request);
}
