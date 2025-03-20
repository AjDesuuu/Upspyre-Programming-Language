import re
import sys

class EBNFExpander:
    def __init__(self):
        self.original_grammar = {}
        self.expanded_grammar = {}
        self.new_nonterminals = {}

    def load_grammar(self, filename):
        """Load the EBNF grammar from a file."""
        with open(filename, 'r') as f:
            content = f.read()
        
        # Split the content into rules
        rule_pattern = re.compile(r'<([^>]+)>\s*::=\s*(.*?)(?=<[^>]+>\s*::=|$)', re.DOTALL)
        matches = rule_pattern.findall(content)
        
        for nonterminal, production in matches:
            nonterminal = nonterminal.strip()
            production = production.strip()
            self.original_grammar[nonterminal] = production
    
    def expand_grammar(self):
        """Expand the grammar by removing all shorthands."""
        for nonterminal, production in self.original_grammar.items():
            expanded_productions = self.expand_rule(nonterminal, production)
            if nonterminal not in self.expanded_grammar:
                self.expanded_grammar[nonterminal] = expanded_productions
            else:
                self.expanded_grammar[nonterminal].extend(expanded_productions)
    
    def expand_rule(self, nonterminal, production):
        """Expand a single rule, handling all EBNF features."""
        results = []
        
        # First, handle parentheses with internal alternatives
        production = self.preprocess_parentheses(production, nonterminal)
        
        # Then handle top-level alternatives
        if ' | ' in production:
            alternatives = production.split(' | ')
            for alt in alternatives:
                expanded_alt = self.expand_alternative(alt, nonterminal)
                results.extend(expanded_alt)
        else:
            expanded = self.expand_alternative(production, nonterminal)
            results.extend(expanded)
            
        return results
    
    def preprocess_parentheses(self, production, current_nonterminal):
        """Handle parentheses with alternatives inside by directly expanding them."""
        # Find all parenthesized groups with modifiers (*, +, ?)
        group_pattern = re.compile(r'\(([^()]+)\)([*+?]?)')
        
        # Keep replacing until no more parentheses with modifiers are found
        while True:
            match = group_pattern.search(production)
            if not match:
                break
                
            group_content = match.group(1)
            modifier = match.group(2)
            before = production[:match.start()]
            after = production[match.end():]
            
            # If there's a modifier, handle it
            if modifier:
                # Create a new nonterminal for the grouped content
                new_nt = f"{current_nonterminal}_GROUP"
                
                # Make sure the name is unique
                counter = 1
                original_new_nt = new_nt
                while new_nt in self.new_nonterminals:
                    new_nt = f"{original_new_nt}_{counter}"
                    counter += 1
                
                # Add the production rules for the grouped content based on the modifier
                if modifier == '*':
                    self.new_nonterminals[new_nt] = [
                        f"{group_content} <{new_nt}>",
                        "ε"
                    ]
                elif modifier == '+':
                    one_nt = f"{new_nt}_ONE"
                    many_nt = f"{new_nt}_MANY"
                    self.new_nonterminals[one_nt] = [
                        f"{group_content} <{many_nt}>"
                    ]
                    self.new_nonterminals[many_nt] = [
                        f"{group_content} <{many_nt}>",
                        "ε"
                    ]
                    new_nt = one_nt
                elif modifier == '?':
                    self.new_nonterminals[new_nt] = [
                        group_content,
                        "ε"
                    ]
                
                # Replace the group with the new nonterminal
                production = f"{before}<{new_nt}>{after}"
            else:
                # If no modifier, just remove the parentheses
                production = f"{before}{group_content}{after}"
        
        return production
    
    def expand_alternative(self, alt, current_nonterminal):
        """Expand a single alternative by removing shorthands."""
        results = [alt]
        new_results = []
        
        # Keep expanding until no more changes
        while True:
            changed = False
            new_results = []
            
            for current in results:
                # Handle Kleene star (*)
                kleene_pattern = re.compile(r'(\S+)\*')
                match = kleene_pattern.search(current)
                if match:
                    changed = True
                    before = current[:match.start()]
                    after = current[match.end():]
                    expr = match.group(1)
                    
                    # Create a new nonterminal for the Kleene star
                    new_nt = f"{current_nonterminal}_KLEENE"
                    
                    # Make sure the name is unique
                    counter = 1
                    original_new_nt = new_nt
                    while new_nt in self.new_nonterminals:
                        new_nt = f"{original_new_nt}_{counter}"
                        counter += 1
                    
                    # Add the production rules for the Kleene star
                    self.new_nonterminals[new_nt] = [
                        f"{expr} <{new_nt}>",
                        "ε"
                    ]
                    
                    # Replace the Kleene star with the new nonterminal
                    new_results.append(f"{before}<{new_nt}>{after}")
                    continue
                
                # Handle optional elements (?)
                optional_pattern = re.compile(r'(\S+)\?')
                match = optional_pattern.search(current)
                if match:
                    changed = True
                    before = current[:match.start()]
                    after = current[match.end():]
                    expr = match.group(1)
                    
                    # Create a new nonterminal for the optional element
                    new_nt = f"{current_nonterminal}_OPT"
                    
                    # Make sure the name is unique
                    counter = 1
                    original_new_nt = new_nt
                    while new_nt in self.new_nonterminals:
                        new_nt = f"{original_new_nt}_{counter}"
                        counter += 1
                    
                    # Add the production rules for the optional element
                    self.new_nonterminals[new_nt] = [
                        expr,
                        "ε"
                    ]
                    
                    # Replace the optional element with the new nonterminal
                    new_results.append(f"{before}<{new_nt}>{after}")
                    continue
                
                # Handle one or more (+)
                plus_pattern = re.compile(r'(\S+)\+')
                match = plus_pattern.search(current)
                if match:
                    changed = True
                    before = current[:match.start()]
                    after = current[match.end():]
                    expr = match.group(1)
                    
                    # Create new nonterminals for the one or more construct
                    one_nt = f"{current_nonterminal}_ONE"
                    many_nt = f"{current_nonterminal}_MANY"
                    
                    # Make sure the names are unique
                    counter = 1
                    original_one_nt = one_nt
                    while one_nt in self.new_nonterminals:
                        one_nt = f"{original_one_nt}_{counter}"
                        counter += 1
                    
                    counter = 1
                    original_many_nt = many_nt
                    while many_nt in self.new_nonterminals:
                        many_nt = f"{original_many_nt}_{counter}"
                        counter += 1
                    
                    # Add the production rules for the one or more construct
                    self.new_nonterminals[many_nt] = [
                        f"{expr} <{many_nt}>",
                        "ε"
                    ]
                    
                    self.new_nonterminals[one_nt] = [
                        f"{expr} <{many_nt}>"
                    ]
                    
                    # Replace the one or more with the new nonterminal
                    new_results.append(f"{before}<{one_nt}>{after}")
                    continue
                
                # If no changes, keep the current alternative
                new_results.append(current)
            
            # If no changes were made, we're done
            if not changed:
                break
                
            results = new_results
        
        return results
    
    def save_grammar(self, filename):
        """Save the expanded grammar to a file, grouping related rules together."""
        with open(filename, 'w', encoding='utf-8') as f:
            f.write("# Expanded EBNF Grammar\n\n")
            
            # First, write the expanded grammar rules
            for nonterminal, productions in self.expanded_grammar.items():
                for production in productions:
                    f.write(f"<{nonterminal}> ::= {production}\n")
                    
                    # Check if this production references any auxiliary nonterminals
                    referenced_nonterminals = re.findall(r'<([^>]+)>', production)
                    for referenced_nt in referenced_nonterminals:
                        if referenced_nt in self.new_nonterminals:
                            # Write the auxiliary rules immediately after the referencing rule
                            for aux_production in self.new_nonterminals[referenced_nt]:
                                f.write(f"<{referenced_nt}> ::= {aux_production}\n")
                            # Remove the auxiliary nonterminal from the dictionary to avoid duplicate writes
                            del self.new_nonterminals[referenced_nt]
            
            # Write any remaining auxiliary nonterminals that were not referenced in the expanded grammar
            for nonterminal, productions in self.new_nonterminals.items():
                for production in productions:
                    f.write(f"<{nonterminal}> ::= {production}\n")

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