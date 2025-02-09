import upspyre_lexer


def test_lexer_from_file(filename):
    with open(filename, "r") as f:
        code = f.read()

    print("Lexing the Upspyre program...\n")
    upspyre_lexer.lexer.input(code)

    for token in upspyre_lexer.lexer:
        print(token)


if __name__ == "__main__":
    test_lexer_from_file("test_code.up")

