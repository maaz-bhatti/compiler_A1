package src;

public enum TokenType {
    KEYWORD,
    IDENTIFIER,
    INTEGER_LITERAL,
    FLOAT_LITERAL,
    STRING_LITERAL,
    CHAR_LITERAL,
    BOOLEAN_LITERAL,
    OPERATOR,
    PUNCTUATOR,
    COMMENT, // For internal tracking, though implementation might skip them in output
    EOF,
    UNKNOWN
}
