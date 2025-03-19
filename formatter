import re

def generate_short_names(non_terminals):
    short_map = {}
    used_names = set()
    
    for nt in sorted(non_terminals, key=len, reverse=True):
        abbr = "".join(word[0] for word in nt.strip('<>').split('_'))
        if abbr in used_names or len(abbr) < 2:
            count = 1
            while f"{abbr}{count}" in used_names:
                count += 1
            abbr = f"{abbr}{count}"
        short_map[nt] = f"<{abbr}>"
        used_names.add(abbr)
    
    return short_map

def shorten_ebnf(grammar: str) -> str:
    non_terminals = set(re.findall(r'<[^>]+>', grammar))
    short_map = generate_short_names(non_terminals)
    
    for long, short in short_map.items():
        grammar = grammar.replace(long, short)
    
    return grammar

def format_ebnf(grammar: str) -> str:
    grammar = shorten_ebnf(grammar)
    lines = grammar.split("\n")
    formatted_lines = []
    
    for line in lines:
        line = line.strip()
        if not line:
            continue
        
        line = re.sub(r'\s*::=\s*', ' ::= ', line)
        line = re.sub(r'\s*\|\s*', ' | ', line)
        
        if "|" in line:
            parts = line.split(" | ")
            formatted_lines.append(parts[0])
            for part in parts[1:]:
                formatted_lines.append(f"    | {part}")
        else:
            formatted_lines.append(line)
    
    return "\n".join(formatted_lines)

def process_ebnf_file(input_path: str, output_path: str):
    with open(input_path, "r", encoding="utf-8") as file:
        ebnf_content = file.read()
    
    formatted_ebnf = format_ebnf(ebnf_content)
    
    with open(output_path, "w", encoding="utf-8") as file:
        file.write(formatted_ebnf)
    
    print(f"Formatted EBNF saved to {output_path}")

# Example usage
input_file = "upspyre.ebnf"
output_file = "format.ebnf"
process_ebnf_file(input_file, output_file)
