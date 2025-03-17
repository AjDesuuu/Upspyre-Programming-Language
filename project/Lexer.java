package project;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Scanner;

public class Lexer {
    private String input;
    private int index = 0, line = 1, position = 0;
    private SymbolTable symbolTable;
    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
            Map.entry("start", TokenType.START), Map.entry("end", TokenType.END),
            Map.entry("if", TokenType.IF), Map.entry("otherwise", TokenType.OTHERWISE),
            Map.entry("repeat", TokenType.REPEAT), Map.entry("until", TokenType.UNTIL),
            Map.entry("for", TokenType.FOR), Map.entry("stop", TokenType.STOP),
            Map.entry("continue", TokenType.CONTINUE), Map.entry("method", TokenType.METHOD),   
            Map.entry("output", TokenType.OUTPUT), Map.entry("get", TokenType.GET),
            Map.entry("show", TokenType.SHOW), Map.entry("true", TokenType.TRUE),
            Map.entry("false", TokenType.FALSE), Map.entry("none", TokenType.NONE),
            Map.entry("number", TokenType.NUMBER_TYPE), Map.entry("decimal", TokenType.DECIMAL_TYPE),
            Map.entry("text", TokenType.TEXT_TYPE), Map.entry("binary", TokenType.BINARY_TYPE),
            Map.entry("list_of", TokenType.LIST_TYPE), Map.entry("pair_map", TokenType.PAIR_MAP_TYPE),
            Map.entry("choose_what", TokenType.CHOOSE_WHAT), Map.entry("convertTo", TokenType.CONVERT_TO),
            Map.entry("len", TokenType.LEN), Map.entry("sort", TokenType.SORT),
            Map.entry("key", TokenType.KEY), Map.entry("value", TokenType.VALUE),
            Map.entry("toText", TokenType.TO_TEXT), Map.entry("pick", TokenType.PICK),
            Map.entry("AND", TokenType.AND),
            Map.entry("OR", TokenType.OR), Map.entry("NOT", TokenType.NOT)
    );

    private static final Map<String, TokenType> OPERATORS = Map.ofEntries(
            Map.entry("=", TokenType.ASSIGN), Map.entry("+", TokenType.PLUS),
            Map.entry("-", TokenType.MINUS), Map.entry("*", TokenType.MULT),
            Map.entry("/", TokenType.DIV), Map.entry("**", TokenType.EXPONENT),
            Map.entry("%", TokenType.MOD), Map.entry("==", TokenType.EQ),
            Map.entry("!=", TokenType.NEQ), Map.entry("<", TokenType.LT),
            Map.entry(">", TokenType.GT), Map.entry("<=", TokenType.LEQ),
            Map.entry(">=", TokenType.GEQ), Map.entry("AND", TokenType.AND),
            Map.entry("OR", TokenType.OR), Map.entry("NOT", TokenType.NOT),
            Map.entry("&", TokenType.BITWISE_AND), Map.entry("|", TokenType.BITWISE_OR),
            Map.entry("^", TokenType.BITWISE_XOR), Map.entry("~", TokenType.BITWISE_NOT),
            Map.entry("<<", TokenType.LSHIFT), Map.entry(">>", TokenType.RSHIFT), 
            Map.entry("///", TokenType.FLOOR_DIV), Map.entry("+=", TokenType.PLUS_ASSIGN),
            Map.entry("-=", TokenType.MINUS_ASSIGN), Map.entry("*=", TokenType.MULT_ASSIGN),
            Map.entry("!",TokenType.S_NOT), Map.entry("\"",TokenType.QUOTE)
    );

    private static final Map<Character, TokenType> SPECIAL_SYMBOLS = Map.of(
            '(', TokenType.LPAREN, ')', TokenType.RPAREN,
            '{', TokenType.LCURLY, '}', TokenType.RCURLY,
            '[', TokenType.LBRACKET, ']', TokenType.RBRACKET,
            ';', TokenType.SEMI, ',', TokenType.COMMA,
            ':', TokenType.COLON, '.', TokenType.DOT
    );

    public Lexer(String filePath, SymbolTable symbolTable) throws FileNotFoundException {
        this.input = readFileContent(filePath);
        this.symbolTable = symbolTable;
    }

    private String readFileContent(String filePath) throws FileNotFoundException {
        File file = new File(filePath);
        StringBuilder content = new StringBuilder();

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                content.append(scanner.nextLine()).append("\n");
            }
        }
        return content.toString();
    }

    private char peek() {
        return (index < input.length()) ? input.charAt(index) : '\0';
    }

    //n lookahead
    private char peek(int n) {
        return (index + n < input.length()) ? input.charAt(index + n) : '\0';
    }

    private char advance() {
        char c = peek();
        index++;
        position++;
        if (c == '\n') {
            line++;
            position = 0;
        }

        return c;
    }

    private void skipWhitespace() {
        while (Character.isWhitespace(peek())) advance();
    }

    //MADE BY: NGAN
    private Token scanIdentifierOrKeyword() {
        int startPos = position;
        int startIdx = index;
        char firstChar = peek();

        // S1: Check if the first character is not a letter or an underscore
        if (!(Character.isLetter(firstChar) || firstChar == '_')) {
            advance();
            String lexeme = input.substring(startIdx, index);
            return new Token(TokenType.ERROR, "Invalid token: "+lexeme, line, startPos);
        }

        // S2: Continue scanning if the first character is valid
        advance();
        while (Character.isLetterOrDigit(peek()) || peek() == '_') {
            advance();
        }

        String lexeme = input.substring(startIdx, index);

        // S3: Check if the lexeme is a keyword, otherwise classify it as an identifier
        TokenType type = KEYWORDS.getOrDefault(lexeme, TokenType.IDENTIFIER);

        // Add identifier to symbol table if it's not a keyword
        if (type == TokenType.IDENTIFIER && !symbolTable.containsIdentifier(lexeme)) {
            symbolTable.addIdentifier(lexeme, type);
        }

        return new Token(type, lexeme, line, startPos);
    }

    //Assigned to Ansel | EDITED BY: NGAN
    private Token scanNumber() {
        int startPos = position;
        int startIdx = index;
        boolean isDecimal = false;
        boolean hasDigitBeforeDot = false;
        boolean hasDigitAfterDot = false;
    
        while (Character.isDigit(peek()) || peek() == '.') {
            if (peek() == '.') {
                if (isDecimal) {
                    // Second dot encountered, return error
                    while (!Character.isWhitespace(peek())) {
                        advance();
                    }
                    String lexeme = input.substring(startIdx, index);
                    return new Token(TokenType.ERROR, "Invalid decimal: " + lexeme, line, startPos);
                }
                isDecimal = true;
                advance(); // Consume the dot
            } else {
                if (isDecimal) {
                    hasDigitAfterDot = true;
                } else {
                    hasDigitBeforeDot = true;
                }
                advance(); // Consume the digit
            }
        }
    
        // Check for invalid characters after the number
        if (Character.isLetter(peek())) {
            while (!Character.isWhitespace(peek()) && peek() != '\0') {
                advance();
            }
            String lexeme = input.substring(startIdx, index);
            return new Token(TokenType.ERROR, "Invalid token: " + lexeme, line, startPos);
        }
    
        // Handle cases like ".123" and "1."
        if (isDecimal) {
            if (!hasDigitBeforeDot && !hasDigitAfterDot) {
                // Both before and after dot are missing, e.g., "."
                String lexeme = input.substring(startIdx, index);
                return new Token(TokenType.ERROR, "Invalid number: " + lexeme, line, startPos);
            } else if (!hasDigitBeforeDot) {
                // Case like ".123", treat as "0.123"
                String lexeme = "0" + input.substring(startIdx, index);
                return new Token(TokenType.DECIMAL, lexeme, line, startPos);
            } else if (!hasDigitAfterDot) {
                // Case like "1.", return error
                String lexeme = input.substring(startIdx, index);
                return new Token(TokenType.ERROR, "Invalid decimal: " + lexeme, line, startPos);
            }
        }
    
        // Return the appropriate token type
        String lexeme = input.substring(startIdx, index);
        TokenType type = isDecimal ? TokenType.DECIMAL : TokenType.NUMBER;
        return new Token(type, lexeme, line, startPos);
    }
    

    //MADE BY: SIMON
    private Token scanText() {
        int startPos = position;
        String lexeme = "";
        boolean warningIssued = false;

        advance(); // Skip opening quote

        // S1: Process characters until closing quote or end of input
        while (peek() != '"' && peek() != '\0') {
            char current = peek();

            if (current == '\\') {
                // DFA Transition: S1 -> S2 on backslash
                advance(); // Consume '\\'
                char escapeChar = peek();

                // Check for end of input after backslash (possible invalid escape sequence)
                if (escapeChar == '\0') {
                    warningIssued = true;
                    lexeme += '\\'; // Treat lone backslash as literal
                    break;
                }

                // DFA State: S2 - Handle escape sequences
                switch (escapeChar) {
                    case 'n':
                        lexeme += '\n';
                        break;
                    case 't':
                        lexeme += '\t';
                        break;
                    case '"':
                        lexeme += '"';
                        break;
                    case '\\':
                        lexeme += '\\';
                        break;
                    default:
                        warningIssued = true; // Issue warning for invalid escape sequences
                        lexeme += "\\" + escapeChar; // Include unknown escape sequence as-is
                        break;
                }
                advance(); // Consume escape character
            } else {
                // DFA Transition: S1 -> S1 on valid character (except '"' and '\\')
                lexeme += current;
                advance();
            }
        }

        // End condition: Closing quote reached -> S1 -> Text
        if (peek() == '"') {
            advance(); // Consume closing quote
            if (warningIssued) {
                System.out.println("Warning: Possible invalid escape sequence in string literal at line " + line + ".");
            }
            return new Token(TokenType.TEXT, lexeme, line, startPos);
        }

        // Error: Unterminated string literal
        return new Token(TokenType.ERROR, "Unterminated string literal: "+ lexeme, line, startPos);
    }

    //Assign to  Jules
    private Token scanComment() {
        int startPos = position;
        int startLine = line;
    
        // Check if it's a multi-line comment
        if (peek() == '/' && peek(1) == '*') {
            advance(); // Consume '/'
            advance(); // Consume '*'
    
            // Scan until the end of the multi-line comment
            while (true) {
                if (peek() == '\0') {
                    // End of input reached without closing the comment
                    return new Token(TokenType.ERROR, "Unterminated multi-line comment" + startLine, startLine, startPos);
                }
    
                if (peek() == '*' && peek(1) == '/') {
                    advance(); // Consume '*'
                    advance(); // Consume '/'
                    break; // End of multi-line comment
                }
    
                advance(); // Consume the current character
            }
    
            return new Token(TokenType.MCOMMENT, "", line, startPos);
        } else {
            // Single-line comment
            while (peek() != '\n' && peek() != '\0') {
                advance();
            }
            return new Token(TokenType.SCOMMENT, "", line, startPos);
        }
    }
    //Assigned to Mark Jason
    private Token scanOperatorOrSpecialSymbol() {
        char firstChar = peek();
        // Check for multi-character operators first
        switch (firstChar) {
            case '!':
                advance();
                if (peek() == '=') {
                    advance();
                    return new Token(TokenType.NEQ, "!=", line, position - 2);
                } else {
                    return new Token(TokenType.S_NOT, "!", line, position - 1);
                }
            case '+':
                advance();
                if (peek() == '=') {
                    advance();
                    return new Token(TokenType.PLUS_ASSIGN, "+=", line, position - 2);
                } else {
                    return new Token(TokenType.PLUS, "+", line, position - 1);
                }
            case '-':  
                advance();
                if (peek() == '=') {
                    advance();
                    return new Token(TokenType.MINUS_ASSIGN, "-=", line, position - 2);
                } else {
                    return new Token(TokenType.MINUS, "-", line, position - 1);
                }
            case '=':
                advance();
                if (peek() == '=') {
                    advance();
                    return new Token(TokenType.EQ, "==", line, position - 2);
                } else {
                    return new Token(TokenType.ASSIGN, "=", line, position - 1);
                }
            case '<':
                advance();
                if (peek() == '=') {
                    advance();
                    return new Token(TokenType.LEQ, "<=", line, position - 2);
                } else if (peek() == '<') {
                    advance();
                    return new Token(TokenType.LSHIFT, "<<", line, position - 2);
                } else {
                    return new Token(TokenType.LT, "<", line, position - 1);
                }
            case '>':
                advance();
                if (peek() == '=') {
                    advance();
                    return new Token(TokenType.GEQ, ">=", line, position - 2);
                } else if (peek() == '>') {
                    advance();
                    return new Token(TokenType.RSHIFT, ">>", line, position - 2);
                } else {
                    return new Token(TokenType.GT, ">", line, position - 1);
                }
            case '&':
                advance();
                return new Token(TokenType.BITWISE_AND, "&", line, position - 1);
            case '|':
                advance();
                return new Token(TokenType.BITWISE_OR, "|", line, position - 1);
            case '*':
                advance();
                if (peek() == '*') {
                    advance();
                    return new Token(TokenType.EXPONENT, "**", line, position - 2);
                } 
                if(peek() == '=') {
                    advance();
                    return new Token(TokenType.MULT_ASSIGN, "*=", line, position - 2);
                }
                else {
                    return new Token(TokenType.MULT, "*", line, position - 1);
                }
            case '/':
                if (peek() == '/' && peek(2) == '/') {
                    // Check for floor division ("///")
                    advance(); // Consume first '/'
                    advance(); // Consume second '/'
                    advance(); // Consume third '/'
                    return new Token(TokenType.FLOOR_DIV, "///", line, position - 3);
                } else if (peek(1) == '/' || peek(1) == '*') {
                    // Check for comments
                    return scanComment();
                }
                // Handle single '/' as division or other operators if applicable
                advance();
                return new Token(TokenType.DIV, "/", line, position - 1);
            case '%':
                advance();
                return new Token(TokenType.MOD, "%", line, position - 1);
            case '^':
                advance();
                return new Token(TokenType.BITWISE_XOR, "^", line, position - 1);
            case '~':
                advance();
                return new Token(TokenType.BITWISE_NOT, "~", line, position - 1);
            default:
                // Check for special symbols
                if (SPECIAL_SYMBOLS.containsKey(firstChar)) {
                    advance();
                    return new Token(SPECIAL_SYMBOLS.get(firstChar), String.valueOf(firstChar), line, position - 1);
                } else {
                    // If no match, return an error token
                    advance();
                    return new Token(TokenType.ERROR, String.valueOf(firstChar), line, position - 1);
                }
        }
    }

    //Assigned to Aaron
    private Token scanTokenUsingDFA() {
        char currentChar = peek();
    
        // Handle identifiers or keywords
        if (Character.isLetter(currentChar) || currentChar == '_') {
            return scanIdentifierOrKeyword();
        }
    
        // Handle numbers
        if (Character.isDigit(currentChar)) {
            return scanNumber();
        }
    
        // Handle floating-point numbers starting with a dot
        if (currentChar == '.' && Character.isDigit(peek(1))) {
            return scanNumber();
        }
    
        // Handle text literals
        if (currentChar == '"') {
            return scanText();
        }
    
        // Handle operators or special symbols (including floor division)
        if (OPERATORS.containsKey(String.valueOf(currentChar)) || SPECIAL_SYMBOLS.containsKey(currentChar)) {
            return scanOperatorOrSpecialSymbol();
        }
    
        // Handle comments
        if (currentChar == '/') {
            if (peek(1) == '/' || peek(1) == '*') {
                return scanComment();
            }
        }
    
        // Handle unrecognized tokens
        advance();
        return new Token(TokenType.ERROR, "Unrecognized token: " + String.valueOf(currentChar), line, position - 1);
    }

    public Token nextToken() {
        skipWhitespace();
        if (index >= input.length()) return new Token(TokenType.EOF, "EOF", line, position);
        return scanTokenUsingDFA();
    }
}