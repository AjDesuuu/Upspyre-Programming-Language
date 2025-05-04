package project.interpreterComponents.utils;

import java.util.List;
import java.util.Map;
import project.TokenType;

public class TypeChecker {
    public void checkType(Object value, Class<?> expected, String errorMessage) {
        if (value == null || !expected.isInstance(value)) {
            throw new InterpreterException(errorMessage + ": Got " + 
                   (value == null ? "null" : value.getClass().getSimpleName()), 0);
        }
    }
    
    public boolean isCompatible(Object value, TokenType type) {
        return switch(type) {
            case NUMBER -> value instanceof Integer;
            case DECIMAL -> value instanceof Double || value instanceof Integer;
            case TEXT -> value instanceof String;
            case BINARY_TYPE -> value instanceof Boolean;
            case LIST_TYPE -> value instanceof List;
            case PAIR_MAP_TYPE -> value instanceof Map;
            default -> false;
        };
    }
    
    public Object convertIfNeeded(Object value, TokenType targetType) {
        if (value == null) return null;
        
        return switch(targetType) {
            case NUMBER -> {
                if (value instanceof Integer) yield value;
                if (value instanceof Double) yield ((Double)value).intValue();
                if (value instanceof String) {
                    try {
                        yield Integer.parseInt((String)value);
                    } catch (NumberFormatException e) {
                        throw new InterpreterException("Cannot convert string to number: " + value, 0);
                    }
                }
                throw new InterpreterException("Cannot convert to number: " + value.getClass(), 0);
            }
            case DECIMAL -> {
                if (value instanceof Double) yield value;
                if (value instanceof Integer) yield ((Integer)value).doubleValue();
                if (value instanceof String) {
                    try {
                        yield Double.parseDouble((String)value);
                    } catch (NumberFormatException e) {
                        throw new InterpreterException("Cannot convert string to decimal: " + value, 0);
                    }
                }
                throw new InterpreterException("Cannot convert to decimal: " + value.getClass(), 0);
            }
            case TEXT -> value.toString();
            default -> {
                if (isCompatible(value, targetType)) {
                    yield value;
                }
                throw new InterpreterException("Cannot convert " + value.getClass() + " to " + targetType, 0);
            }
        };
    }
}