import ply.lex as lex

# List of token names
tokens = (
    'NUMBER', 'DECIMAL', 'TEXT', 'BINARY',
    'IDENTIFIER', 'ASSIGN', 'SEMI', 'LPAREN', 'RPAREN',
    'LBRACE', 'RBRACE', 'COMMA', 'COLON', 'RBRACKET', 'LBRACKET',
    'PLUS', 'MINUS', 'TIMES', 'DIVIDE', 'MOD',
    'EQUAL', 'NEQUAL', 'LT', 'GT', 'LE', 'GE',
    'AND', 'OR', 'NOT', 'CONVERT','DOT','COMMENT'
)

# Reserved keywords
reserved = {
    'start': 'START',
    'end': 'END',
    'if': 'IF',
    'otherwise': 'OTHERWISE',
    'repeat': 'REPEAT',
    'repeat-until': 'REPEAT_UNTIL',
    'for': 'FOR',
    'stop': 'STOP',
    'continue': 'CONTINUE',
    'method': 'METHOD',
    'output': 'OUTPUT',
    'choose-what': 'CHOOSE_WHAT',
    'pick': 'PICK',
    'list-of': 'LIST_OF',
    'pair-map': 'PAIR_MAP',
    'get': 'GET',
    'show': 'SHOW',
    'convertTo': 'CONVERT_TO',
    'number': 'NUMBER_TYPE',
    'decimal': 'DECIMAL_TYPE', 
    'text': 'TEXT_TYPE',
    'binary': 'BINARY_TYPE'
}

tokens += tuple(reserved.values())

# Token regex patterns
t_ASSIGN = r'='
t_SEMI = r';'
t_LPAREN = r'\('
t_RPAREN = r'\)'
t_LBRACE = r'\{'
t_RBRACE = r'\}'
t_COMMA = r','
t_COLON = r':'
t_PLUS = r'\+'
t_MINUS = r'-'
t_TIMES = r'\*'
t_DIVIDE = r'/'
t_MOD = r'%'
t_EQUAL = r'=='
t_NEQUAL = r'!='
t_LT = r'<'
t_GT = r'>'
t_LE = r'<='
t_GE = r'>='
t_AND = r'and'
t_OR = r'or'
t_NOT = r'not'
t_RBRACKET = r'\]'
t_LBRACKET = r'\['
t_DOT = r'\.'

def t_SINGLE_LINE(t):
    r'//[^\n]*'
    t.type = 'COMMENT'
    return t

def t_MULTI_LINE(t):
    r'/\*[\s\S]*?\*/'
    t.type = 'COMMENT'
    return t

# Identifiers and literals
def t_IDENTIFIER(t):
    r'[a-zA-Z_][a-zA-Z0-9_]*'
    t.type = reserved.get(t.value, 'IDENTIFIER')  # Check for reserved words
    return t


def t_DECIMAL(t):
    r'\d+\.\d+'
    t.value = float(t.value)
    return t

def t_NUMBER(t):
    r'\d+'
    t.value = int(t.value)
    return t


#Shubashuba
def t_TEXT(t):
    r'\".*?\"'
    t.value = t.value[1:-1]  # Remove quotes
    return t

def t_BINARY(t):
    r'true|false'
    t.value = (t.value == 'true')
    return t

# Ignore whitespace
t_ignore = ' \t\n'
def t_MALFORMED_DECIMAL(t):
    r'\d+\.\d+\.\d+'
    print(f"Lexer Error: Malformed decimal '{t.value}' at line {t.lineno}")
    t.lexer.skip(len(t.value))
def t_UNTERMINATED_STRING(t):
    r'\"([^"\n]*)$'
    print(f"Lexer Error: Unterminated string starting at line {t.lineno}")
    t.lexer.skip(len(t.value))
def t_INVALID_IDENTIFIER(t):
    r'\d+[a-zA-Z_][a-zA-Z0-9_]*'
    print(f"Lexer Error: Invalid identifier '{t.value}' at line {t.lineno}")
    t.lexer.skip(len(t.value))
def t_UNEXPECTED_CHAR(t):
    r'[@#$%^&|~`]'
    print(f"Lexer Error: Unexpected character '{t.value}' at line {t.lineno}")
    t.lexer.skip(1)
def t_UNTERMINATED_COMMENT(t):
    r'/\*[\s\S]*?(?!\*/)'
    print(f"Lexer Error: Unterminated multi-line comment at line {t.lineno}")
    t.lexer.skip(len(t.value))
# Error handling
def t_error(t):
    print(f"Illegal character '{t.value[0]}'")
    t.lexer.skip(1)

# Build the lexer
lexer = lex.lex()

#banana

# Test function
def test_lexer(data):
    lexer.input(data)
    for token in lexer:
        print(token)

# Run lexer on a test string
if __name__ == "__main__":
    test_code = """
    
    """
    test_lexer(test_code)


