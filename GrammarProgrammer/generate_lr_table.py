import subprocess
import sys
import os

def main():
    if len(sys.argv) != 3:
        print("Usage: python generate_lr_table.py input_file output_file")
        sys.exit(1)

    input_file = sys.argv[1]
    output_file = sys.argv[2]

    # Step 1: Expand the EBNF grammar using ebnf_expander.py
    expanded_grammar_file = "expanded_grammar.txt"
    expander_command = f"python ebnf_expander.py {input_file} {expanded_grammar_file}"
    subprocess.run(expander_command, shell=True, check=True)

    # Step 2: Generate the LR table using lr_generator.py
    lr_generator_command = f"python lr_generator.py {expanded_grammar_file} -o {output_file}"
    subprocess.run(lr_generator_command, shell=True, check=True)

    # Optionally, clean up the intermediate expanded grammar file
    os.remove(expanded_grammar_file)

    print(f"LR table generated and saved to {output_file}")

if __name__ == "__main__":
    main()