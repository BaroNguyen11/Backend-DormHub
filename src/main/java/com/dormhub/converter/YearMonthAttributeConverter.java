package com.dormhub.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Converter
public class YearMonthAttributeConverter implements AttributeConverter<YearMonth, LocalDateTime> {

    @Override
    public LocalDateTime convertToDatabaseColumn(YearMonth month) {
        return month == null ? null : month.atDay(1).atStartOfDay();
    }

    @Override
    public YearMonth convertToEntityAttribute(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        if (dateTime.getDayOfMonth() != 1 || dateTime.toLocalTime().getNano() != 0
                || dateTime.toLocalTime().toSecondOfDay() != 0) {
            throw new IllegalArgumentException("Billing month must be stored at the start of a month");
        }
        return YearMonth.from(dateTime);
    }
}
