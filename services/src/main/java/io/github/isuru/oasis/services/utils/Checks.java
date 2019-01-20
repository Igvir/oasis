package io.github.isuru.oasis.services.utils;

import io.github.isuru.oasis.services.exception.InputValidationException;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author iweerarathna
 */
public final class Checks {

    public static void onlyOneOf(boolean condition1, boolean condition2, String param1, String param2) throws InputValidationException {
        if (condition1 == condition2) {
            throw new InputValidationException(String.format("There can be either '%s' or '%s', not both!",
                    param1, param2));
        }
    }
    public static void havingBoth(boolean condition1, boolean condition2, String param1, String param2) throws InputValidationException {
        if (condition1 && condition2) {
            throw new InputValidationException(String.format("Parameter '%s' and '%s' cannot be defined at the same time!",
                    param1, param2));
        }
    }


    public static <T> void isOneOf(T value, Set<T> allowedValues, String param) throws InputValidationException {
        if (!allowedValues.contains(value)) {
            throw new InputValidationException(String.format("Parameter '%s' must be one of "
                    + allowedValues.stream().map(Object::toString)
                    .collect(Collectors.joining(", ", "[", "]")), param));
        }
    }

    public static void validate(boolean condition, String message) throws InputValidationException {
        if (!condition) {
            throw new InputValidationException(message);
        }
    }

    public static void nonNull(Object value, String paramName) throws InputValidationException {
        if (value == null) {
            throw new InputValidationException(String.format("Parameter '%s' must be non null!", paramName));
        }
    }

    public static void nonNullOrEmpty(String value, String paramName) throws InputValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new InputValidationException(String.format("Parameter '%s' must be non empty!", paramName));
        }
    }

    public static void nonNullOrEmpty(Collection<?> list, String paramName) throws InputValidationException {
        if (list == null || list.isEmpty()) {
            throw new InputValidationException(String.format("Parameter list type '%s' must be non empty!", paramName));
        }
    }

    public static void nonNullOrEmpty(Map<?, ?> map, String paramName) throws InputValidationException {
        if (map == null || map.isEmpty()) {
            throw new InputValidationException(String.format("Parameter map type '%s' must be non empty!", paramName));
        }
    }

    public static void greaterThanZero(long value, String paramName) throws InputValidationException {
        if (value <= 0) {
            throw new InputValidationException(String.format("Parameter '%s' must be greater than zero!", paramName));
        }
    }

    public static void nonNegative(long value, String paramName) throws InputValidationException {
        if (value < 0) {
            throw new InputValidationException(String.format("Parameter '%s' must be non negative!", paramName));
        }
    }

}
