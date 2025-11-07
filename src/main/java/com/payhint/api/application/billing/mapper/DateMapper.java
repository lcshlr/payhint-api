package com.payhint.api.application.billing.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DateMapper {

    default String mapLocalDateToString(LocalDate date) {
        return date == null ? null : date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    default String mapLocalDateTimeToString(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    default LocalDate mapStringToLocalDate(String date) {
        return date == null ? null : LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    default LocalDateTime mapStringToLocalDateTime(String dateTime) {
        return dateTime == null ? null : LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
