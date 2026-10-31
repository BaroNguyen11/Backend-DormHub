package com.dormhub.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;
import java.time.YearMonth;

@Converter
public class YearMonthAttributeConverter implements AttributeConverter<YearMonth, LocalDate> {

    @Override
    public LocalDate convertToDatabaseColumn(YearMonth month) {
        return month == null ? null : month.atDay(1);
    }

    @Override
    public YearMonth convertToEntityAttribute(LocalDate date) {
        if (date == null) {
            return null;
        }
        if (date.getDayOfMonth() != 1) {
            throw new IllegalArgumentException("Billing month must be stored as the first day of a month");
        }
        return YearMonth.from(date);
    }
}
