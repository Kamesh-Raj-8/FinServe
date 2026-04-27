package com.smelend.smelendbackend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

public class DateWithinWindowValidator implements ConstraintValidator<DateWithinWindow, LocalDate> {

    private int     days;
    private boolean futureOnly;

    @Override
    public void initialize(DateWithinWindow annotation) {
        this.days       = annotation.days();
        this.futureOnly = annotation.futureOnly();
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext ctx) {
        if (value == null) return true; // null handled by @NotNull

        LocalDate today   = LocalDate.now();
        LocalDate latest  = today.plusDays(days);
        // futureOnly=true: must be strictly after today
        // futureOnly=false: ±days window (legacy behaviour for disbursement dates)
        LocalDate earliest = futureOnly ? today.plusDays(1) : today.minusDays(days);

        boolean valid = !value.isBefore(earliest) && !value.isAfter(latest);

        if (!valid) {
            ctx.disableDefaultConstraintViolation();
            String msg = futureOnly
                ? "Date '" + value + "' must be a future date between " + earliest + " and " + latest + "."
                : "Date '" + value + "' is outside ±" + days + " days from today (" + today + "). Allowed: " + earliest + " to " + latest;
            ctx.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
        }
        return valid;
    }
}
