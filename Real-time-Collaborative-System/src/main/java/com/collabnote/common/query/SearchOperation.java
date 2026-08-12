package com.collabnote.common.query;

public enum SearchOperation {
    EQUALITY, NEGATION, GREATER_THAN, LESS_THAN, LIKE, STARTS_WITH, ENDS_WITH, IN, NOT_IN;

    public static final String[] SIMPLE_OPERATION_SET = {
            ":", "!", ">", "<", "~", "%", "*", "@", "#"
    };

    public static SearchOperation getSimpleOperation(char input) {
        return switch (input) {
            case ':' -> EQUALITY;
            case '!' -> NEGATION;
            case '>' -> GREATER_THAN;
            case '<' -> LESS_THAN;
            case '~' -> LIKE;
            case '%' -> STARTS_WITH;
            case '*' -> ENDS_WITH;
            case '@' -> IN;
            case '#' -> NOT_IN;
            default -> null;
        };
    }
}
