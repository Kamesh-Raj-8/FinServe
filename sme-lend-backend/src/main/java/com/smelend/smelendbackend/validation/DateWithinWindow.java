package com.smelend.smelendbackend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validates that a LocalDate is within an allowed window.
 *
 * When futureOnly=true:  date must be today+1 … today+days (for offers, PTPs).
 * When futureOnly=false: date can be today-days … today+days (for disbursement dates).
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateWithinWindowValidator.class)
@Documented
public @interface DateWithinWindow {
    String message() default "Date must be within the allowed window";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    /** Max days ahead (and behind when futureOnly=false). */
    int days() default 30;
    /** When true, date must be strictly in the future (no past dates). */
    boolean futureOnly() default false;
}
