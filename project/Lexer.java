package project;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Scanner;

public class Lexer {
    private String input;
    private int index = 0, line = 1, position = 0;
    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
            Map.entry("start", TokenType.START), Map.entry("end", TokenType.END),
            Map.entry("if", TokenType.IF), Map.entry("otherwise", TokenType.OTHERWISE),
            Map.entry("repeat", TokenType.REPEAT), Map.entry("repeat-until", TokenType.REPEAT_UNTIL),
            Map.entry("for", TokenType.FOR), Map.entry("stop", TokenType.STOP),
            Map.entry("continue", TokenType.CONTINUE), Map.entry("method", TokenType.METHOD),   
            Map.entry("output", TokenType.OUTPUT), Map.entry("get", TokenType.GET),
            Map.entry("show", TokenType.SHOW), Map.entry("true", TokenType.TRUE),
            Map.entry("false", TokenType.FALSE), Map.entry("none", TokenType.NONE),
            Map.entry("number", TokenType.NUMBER_TYPE), Map.entry("decimal", TokenType.DECIMAL_TYPE),
            Map.entry("text", TokenType.TEXT_TYPE), Map.entry("binary", TokenType.BINARY_TYPE),
            Map.entry("list-of", TokenType.LIST_TYPE), Map.entry("pair-map", TokenType.PAIR_MAP_TYPE)
    );

    private static final Map<String, TokenType> OPERATORS = Map.ofEntries(
            Map.entry("=", TokenType.ASSIGN), Map.entry("+", TokenType.PLUS),
            Map.entry("-", TokenType.MINUS), Map.entry("*", TokenType.MULT),
            Map.entry("/", TokenType.DIV), Map.entry("**", TokenType.EXPONENT),
            Map.entry("%", TokenType.MOD), Map.entry("==", TokenType.EQ),
            Map.entry("!=", TokenType.NEQ), Map.entry("<", TokenType.LT),
            Map.entry(">", TokenType.GT), Map.entry("<=", TokenType.LEQ),
            Map.entry(">=", TokenType.GEQ), Map.entry("and", TokenType.AND),
            Map.entry("or", TokenType.OR), Map.entry("not", TokenType.LOG_NOT),
            Map.entry("&", TokenType.BITWISE_AND), Map.entry("|", TokenType.BITWISE_OR),
            Map.entry("^", TokenType.BITWISE_XOR), Map.entry("~", TokenType.BITWISE_NOT),
            Map.entry("<<", TokenType.LSHIFT), Map.entry(">>", TokenType.RSHIFT), 
            Map.entry("//", TokenType.FLOOR_DIV), Map.entry("+=", TokenType.PLUS_ASSIGN),
            Map.entry("-=", TokenType.MINUS_ASSIGN), Map.entry("*=", TokenType.MULT_ASSIGN),
            Map.entry("!",TokenType.NOT)
    );

    private static final Map<Character, TokenType> SPECIAL_SYMBOLS = Map.of(
            '(', TokenType.LPAREN, ')', TokenType.RPAREN,
            '{', TokenType.LBRACE, '}', TokenType.RBRACE,
            '[', TokenType.LBRACKET, ']', TokenType.RBRACKET,
            ';', TokenType.SEMI, ',', TokenType.COMMA,
            ':', TokenType.COLON, '.', TokenType.DOT
    );

    public Lexer(String filePath) throws FileNotFoundException {
        this.input = readFileContent(filePath);
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
            while (!Character.isWhitespace(peek()) && peek() != '\0') {
                advance();
            }
            String lexeme = input.substring(startIdx, index);
            return new Token(TokenType.ERROR, lexeme, line, startPos);
        }

        // S2: Continue scanning if the first character is valid
        advance();
        while (Character.isLetterOrDigit(peek()) || peek() == '_') {
            advance();
        }

        String lexeme = input.substring(startIdx, index);

        // S3: Check if the lexeme is a keyword, otherwise classify it as an identifier
        TokenType type = KEYWORDS.getOrDefault(lexeme, TokenType.IDENTIFIER);

        return new Token(type, lexeme, line, startPos);
    }

    //Assigned to Ansel | EDITED BY: NGAN
    private Token scanNumber() {
        int startPos = position;
        int startIdx = index;
    
        while (Character.isDigit(peek())) {
            advance();
        }
    
        // Added by: Ngan (Error state)
        if (Character.isLetter(peek())) {
            while (!Character.isWhitespace(peek()) && peek() != '\0') {
                advance();
            }
            String lexeme = input.substring(startIdx, index);
            return new Token(TokenType.ERROR, lexeme, line, startPos);
        }
    
        // Otherwise, return a valid number token
        String lexeme = input.substring(startIdx, index);
        return new Token(TokenType.NUMBER, lexeme, line, startPos);
    }
    

    //MADE BY: SIMON
    private Token scanString() {
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
            return new Token(TokenType.STRING, lexeme, line, startPos);
        }

        // Error: Unterminated string literal
        System.out.println("Error: Unterminated string literal at line " + line + ".");
        return new Token(TokenType.ERROR, "Unterminated string literal: " + lexeme, line, startPos);
    }

    //Assign to  Jules
    private Token scanComment() {
        int startPos = position;
        while (peek() != '\n' && peek() != '\0') advance();
        return new Token(TokenType.COMMENT, "", line, startPos);
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
                    return new Token(TokenType.NOT, "!", line, position - 1);
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
                if (peek() == '&') {
                    advance();
                    return new Token(TokenType.AND, "&&", line, position - 2);
                } else {
                    return new Token(TokenType.BITWISE_AND, "&", line, position - 1);
                }
            case '|':
                advance();
                if (peek() == '|') {
                    advance();
                    return new Token(TokenType.OR, "||", line, position - 2);
                } else {
                    return new Token(TokenType.BITWISE_OR, "|", line, position - 1);
                }
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
                advance();
                if (peek() == '/') {
                    advance();
                    return new Token(TokenType.FLOOR_DIV, "//", line, position - 2);
                } else {
                    return new Token(TokenType.DIV, "/", line, position - 1);
                }
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
        if (Character.isLetter(currentChar) || currentChar == '_') {
            return scanIdentifierOrKeyword();
        } else if (Character.isDigit(currentChar)) {
            return scanNumber();
        } else if (currentChar == '"') {
            return scanString();
        } else if (currentChar == '/') {
            advance();
            if (peek() == '/') {
                return scanComment();
            } else {
                return new Token(TokenType.DIV, "/", line, position - 1);
            }
        } else if (OPERATORS.containsKey(String.valueOf(currentChar))) {
            return scanOperatorOrSpecialSymbol();
        } else if (SPECIAL_SYMBOLS.containsKey(currentChar)) {
            return scanOperatorOrSpecialSymbol();
        } else {
            advance();
            return new Token(TokenType.ERROR, String.valueOf(currentChar), line, position - 1);
        }
    }

    public Token nextToken() {
        skipWhitespace();
        if (index >= input.length()) return new Token(TokenType.EOF, "EOF", line, position);
        return scanTokenUsingDFA();
    }
}