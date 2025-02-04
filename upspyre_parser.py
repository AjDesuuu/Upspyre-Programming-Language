import ply.yacc as yacc
from upspyre_lexer import tokens

# Define precedence
precedence = (
    ('left', 'AND', 'OR'),
    ('left', 'EQUAL', 'NEQUAL', 'LT', 'GT', 'LE', 'GE'),
    ('left', 'PLUS', 'MINUS'),
    ('left', 'TIMES', 'DIVIDE', 'MOD'),
    ('right', 'NOT')
)


# Program structure
def p_program(p):
    '''program : START statement_list END'''
    print("Valid program structure!")


def p_statement_list(p):
    '''statement_list : statement
                      | statement statement_list'''


def p_statement(p):
    '''statement : assignment_statement
                 | expression_statement
                 | conditional_statement
                 | loop_statement
                 | function_declaration
                 | function_call
                 | choose_what_statement'''


# Expressions
def p_expression_statement(p):
    '''expression_statement : expression SEMI'''


def p_expression(p):
    '''expression : term
                  | term arithmetic_operator expression'''


def p_term(p):
    '''term : factor
            | factor relational_operator term'''


def p_factor(p):
    '''factor : IDENTIFIER
              | NUMBER
              | DECIMAL
              | BINARY
              | TEXT
              | LPAREN expression RPAREN'''


# Arithmetic and logical operations
def p_arithmetic_operator(p):
    '''arithmetic_operator : PLUS
                           | MINUS
                           | TIMES
                           | DIVIDE
                           | MOD'''


def p_relational_operator(p):
    '''relational_operator : EQUAL
                           | NEQUAL
                           | LT
                           | GT
                           | LE
                           | GE'''


# Assignments
def p_assignment_statement(p):
    '''assignment_statement : IDENTIFIER ASSIGN expression SEMI'''


# Conditionals
def p_conditional_statement(p):
    '''conditional_statement : IF LPAREN expression RPAREN block_statement
                             | IF LPAREN expression RPAREN block_statement OTHERWISE block_statement'''


# Loops
def p_loop_statement(p):
    '''loop_statement : FOR LPAREN assignment_statement expression SEMI expression RPAREN block_statement
                      | REPEAT LPAREN expression RPAREN block_statement
                      | REPEAT block_statement REPEAT_UNTIL LPAREN expression RPAREN'''


# Function Definitions and Calls
def p_function_declaration(p):
    '''function_declaration : METHOD IDENTIFIER LPAREN parameter_list RPAREN block_statement'''


def p_function_call(p):
    '''function_call : IDENTIFIER LPAREN argument_list RPAREN SEMI'''


# Choose-what (Switch)
def p_choose_what_statement(p):
    '''choose_what_statement : CHOOSE_WHAT LBRACE pick_case_list RBRACE'''


def p_pick_case_list(p):
    '''pick_case_list : pick_case
                      | pick_case pick_case_list'''


def p_pick_case(p):
    '''pick_case : PICK expression COLON block_statement'''


# Lists and Pair-maps
def p_list_declaration(p):
    '''list_declaration : LIST_OF IDENTIFIER ASSIGN LBRACE expression_list RBRACE SEMI'''


def p_pair_map_declaration(p):
    '''pair_map_declaration : PAIR_MAP IDENTIFIER ASSIGN LBRACE pair_map_list RBRACE SEMI'''


# Block Statements
def p_block_statement(p):
    '''block_statement : LBRACE statement_list RBRACE'''


# Error handling
def p_error(p):
    if p:
        print(f"Syntax error at '{p.value}'")
    else:
        print("Syntax error at EOF")


# Build the parser
parser = yacc.yacc()

# Test input
test_code = """
start
    x = 5;
    if (x > 3) {
        show "Hello World!";
    }
end
"""

# Run the parser
parser.parse(test_code)
