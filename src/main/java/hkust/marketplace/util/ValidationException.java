package hkust.marketplace.util;

import java.util.List;

/** Thrown when user input breaks one or more validation rules. Carries every rule that failed. */
public class ValidationException extends MarketplaceException {
    private static final long serialVersionUID = 1L;

    private final List<String> errors;

    /** @param errors every rule that failed, in display order (at least one) */
    public ValidationException(List<String> errors) {
        super(String.join("\n", errors));
        this.errors = List.copyOf(errors);
    }

    /** @param error the single rule that failed */
    public ValidationException(String error) {
        this(List.of(error));
    }

    /** @return every rule that failed */
    public List<String> getErrors() { return errors; }
}
