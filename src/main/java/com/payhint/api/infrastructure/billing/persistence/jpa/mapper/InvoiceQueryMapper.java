package com.payhint.api.infrastructure.billing.persistence.jpa.mapper;

import java.util.List;

import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.payhint.api.application.billing.dto.response.InvoiceSummaryResponse;
import com.payhint.api.infrastructure.billing.persistence.jpa.repository.InvoiceSpringRepository;

@Mapper(componentModel = "spring")
public interface InvoiceQueryMapper {
    @Named("toInvoiceSummaryResponse")
    @Mapping(target = "status", expression = "java(proj.getStatus().name())")
    @Mapping(target = "createdAt", expression = "java(proj.getCreatedAt().toString())")
    @Mapping(target = "updatedAt", expression = "java(proj.getUpdatedAt().toString())")
    InvoiceSummaryResponse toResponse(InvoiceSpringRepository.InvoiceSummaryProjection proj);

    @IterableMapping(qualifiedByName = "toInvoiceSummaryResponse")
    List<InvoiceSummaryResponse> toResponse(List<InvoiceSpringRepository.InvoiceSummaryProjection> projections);
}
