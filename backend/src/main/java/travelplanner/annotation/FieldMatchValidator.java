package travelplanner.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;
import org.springframework.beans.BeanWrapperImpl;

public class FieldMatchValidator implements ConstraintValidator<FieldMatch, Object> {
    private String field;
    private String fieldMatch;
    private String message;

    @Override
    public void initialize(FieldMatch constraintAnnotation) {
        this.field = constraintAnnotation.field();
        this.fieldMatch = constraintAnnotation.fieldMatch();
        this.message = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        Object field = new BeanWrapperImpl(value).getPropertyValue(this.field);
        Object fieldMatch = new BeanWrapperImpl(value).getPropertyValue(this.fieldMatch);
        boolean valid = Objects.equals(field, fieldMatch);
        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode(this.fieldMatch)
                    .addConstraintViolation();
        }
        return valid;
    }
}
