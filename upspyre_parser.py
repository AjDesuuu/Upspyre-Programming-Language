import ply.yacc as yacc
from upspyre_lexer import tokens 

# Program structure
def p_program(p):
    'program : START statement END'
    p[0] = p[2]

def p_statement(p):
    '''statement : implicit_assign_statement 
                 | explicit_assign_statement'''
    p[0] = p[1]

def p_implicit_assign_statement(p):
    'implicit_assign_statement : IDENTIFIER ASSIGN expression SEMI'
    p[0] = ('assign', p[1], p[3])

def p_explicit_assign_statement(p):
    'explicit_assign_statement : type IDENTIFIER ASSIGN expression SEMI'
    p[0] = ('assign', p[1], p[2],p[4])

def p_data_type(p): 
    '''type : NUMBER_TYPE
                 | TEXT_TYPE
                 | BINARY_TYPE
                 | DECIMAL_TYPE'''
    p[0] = p[1]  # Assign the data type

def p_expression(p):
    'expression : term'
    p[0] = p[1]

def p_term(p):
    '''term : NUMBER
            | DECIMAL
            | TEXT
            | BINARY
            | IDENTIFIER'''
    p[0] = p[1]

def p_error(p):
    print("Syntax error in input!"+p.value)

# Build the parser
parser = yacc.yacc()

test_code = "start text st = \"hello\"; end"
result = parser.parse(test_code)
print(result)
