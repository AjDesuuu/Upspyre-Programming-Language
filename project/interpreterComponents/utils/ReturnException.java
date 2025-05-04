package project.interpreterComponents.utils;

public class ReturnException extends RuntimeException {
    public final Object value;

    public ReturnException(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}