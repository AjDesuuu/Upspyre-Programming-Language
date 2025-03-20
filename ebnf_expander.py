import re
import sys

class EBNFExpander:
    def __init__(self):
        self.original_grammar = {}
        self.expanded_grammar = {}
        self.new_nonterminals = {}
        self.next_id = 1

    def load_grammar(self, filename):
        """Load the EBNF grammar from a file."""
        with open(filename, 'r') as f:
            content = f.read()
        
        # Split the content into rules
        rule_pattern = re.compile(r'<([^>]+)>\s*::=\s*(.*?)(?=<[^>]+>\s*::=|$)', re.DOTALL)
        matches = rule_pattern.findall(content)
        
        for nonterminal, production in matches:
            production = production.strip()
            self.original_grammar[nonterminal] = production
    
    def expand_grammar(self):
        """Expand the grammar by removing all shorthands."""
        for nonterminal, production in self.original_grammar.items():
            expanded_production = self.expand_production(production)
            self.expanded_grammar[nonterminal] = expanded_production
        
        # Add all the new nonterminals
        self.expanded_grammar.update(self.new_nonterminals)
    
    def expand_production(self, production):
        """Expand a production rule by removing shorthands."""
        # Handle alternatives (|)
        if ' | ' in production:
            alternatives = production.split(' | ')
            expanded_alternatives = []
            for alt in alternatives:
                expanded_alt = self.expand_alternative(alt)
                expanded_alternatives.append(expanded_alt)
            return expanded_alternatives
        else:
            return [self.expand_alternative(production)]
    
    def expand_alternative(self, alt):
        """Expand a single alternative by removing shorthands."""
        # Handle Kleene star (*)
        kleene_pattern = re.compile(r'(\([^)]+\)\*|\S+\*)')
        match = kleene_pattern.search(alt)
        if match:
            before = alt[:match.start()]
            after = alt[match.end():]
            
            kleene_expr = match.group(1)
            if kleene_expr.endswith('*'):
                expr = kleene_expr[:-1]
            
            # Create a new nonterminal for the Kleene star
            new_nonterminal = f"KLEENE_{self.next_id}"
            self.next_id += 1
            
            # Add the new production rules
            if expr.startswith('(') and expr.endswith(')'):
                expr = expr[1:-1]
            
            self.new_nonterminals[new_nonterminal] = [
                f"{expr} {new_nonterminal}",
                "ε"
            ]
            
            # Replace the Kleene star in the original alternative
            alt = f"{before}{new_nonterminal}{after}"
            
            # Recursively expand the rest of the alternative
            return self.expand_alternative(alt)
        
        # Handle grouping (...)
        group_pattern = re.compile(r'\(([^)]+)\)')
        match = group_pattern.search(alt)
        if match:
            before = alt[:match.start()]
            after = alt[match.end():]
            
            group_expr = match.group(1)
            
            # Create a new nonterminal for the group
            new_nonterminal = f"GROUP_{self.next_id}"
            self.next_id += 1
            
            # Add the new production rules
            if ' | ' in group_expr:
                self.new_nonterminals[new_nonterminal] = group_expr.split(' | ')
            else:
                self.new_nonterminals[new_nonterminal] = [group_expr]
            
            # Replace the group in the original alternative
            alt = f"{before}{new_nonterminal}{after}"
            
            # Recursively expand the rest of the alternative
            return self.expand_alternative(alt)
        
        # Handle optional elements (?)
        optional_pattern = re.compile(r'(\([^)]+\)\?|\S+\?)')
        match = optional_pattern.search(alt)
        if match:
            before = alt[:match.start()]
            after = alt[match.end():]
            
            optional_expr = match.group(1)
            if optional_expr.endswith('?'):
                expr = optional_expr[:-1]
            
            # Create a new nonterminal for the optional element
            new_nonterminal = f"OPT_{self.next_id}"
            self.next_id += 1
            
            # Add the new production rules
            if expr.startswith('(') and expr.endswith(')'):
                expr = expr[1:-1]
            
            if ' | ' in expr:
                self.new_nonterminals[new_nonterminal] = expr.split(' | ') + ["ε"]
            else:
                self.new_nonterminals[new_nonterminal] = [expr, "ε"]
            
            # Replace the optional element in the original alternative
            alt = f"{before}{new_nonterminal}{after}"
            
            # Recursively expand the rest of the alternative
            return self.expand_alternative(alt)
        
        # Handle one or more (+)
        plus_pattern = re.compile(r'(\([^)]+\)\+|\S+\+)')
        match = plus_pattern.search(alt)
        if match:
            before = alt[:match.start()]
            after = alt[match.end():]
            
            plus_expr = match.group(1)
            if plus_expr.endswith('+'):
                expr = plus_expr[:-1]
            
            # Create new nonterminals for the one or more construct
            one_nonterminal = f"ONE_{self.next_id}"
            self.next_id += 1
            
            many_nonterminal = f"MANY_{self.next_id}"
            self.next_id += 1
            
            # Add the new production rules
            if expr.startswith('(') and expr.endswith(')'):
                expr = expr[1:-1]
            
            self.new_nonterminals[many_nonterminal] = [
                f"{expr} {many_nonterminal}",
                "ε"
            ]
            
            self.new_nonterminals[one_nonterminal] = [
                f"{expr} {many_nonterminal}"
            ]
            
            # Replace the one or more in the original alternative
            alt = f"{before}{one_nonterminal}{after}"
            
            # Recursively expand the rest of the alternative
            return self.expand_alternative(alt)
        
        # Clean up angle brackets around non-terminals
        cleaned_alt = re.sub(r'<([^>]+)>', r'\1', alt)
        
        return cleaned_alt
    
    def save_grammar(self, filename):
        """Save the expanded grammar to a file."""
        with open(filename, 'w') as f:
            f.write("# Expanded EBNF Grammar\n\n")
            for nonterminal, productions in self.expanded_grammar.items():
                for production in productions:
                    f.write(f"{nonterminal} ::= {production}\n")
                f.write("\n")

def main():
    if len(sys.argv) != 3:
        print("Usage: python ebnf_expander.py input_file output_file")
        sys.exit(1)
    
    input_file = sys.argv[1]
    output_file = sys.argv[2]
    
    expander = EBNFExpander()
    expander.load_grammar(input_file)
    expander.expand_grammar()
    expander.save_grammar(output_file)
    
    print(f"Expanded grammar written to {output_file}")

if __name__ == "__main__":
    main()