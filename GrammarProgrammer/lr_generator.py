import openpyxl
import json
import csv

class Symbol:
    def __init__(self, name, is_terminal=False):
        self.name = name
        self.is_terminal = is_terminal
    
    def __repr__(self):
        return self.name
    
    def __eq__(self, other):
        if isinstance(other, Symbol):
            return self.name == other.name and self.is_terminal == other.is_terminal
        return False
    
    def __hash__(self):
        return hash((self.name, self.is_terminal))


class Production:
    def __init__(self, lhs, rhs):
        self.lhs = lhs  # Left-hand side (non-terminal symbol)
        self.rhs = rhs  # Right-hand side (list of symbols)
    
    def __repr__(self):
        return f"{self.lhs} ::= {' '.join(str(s) for s in self.rhs)}"
    
    def __eq__(self, other):
        if isinstance(other, Production):
            return self.lhs == other.lhs and self.rhs == other.rhs
        return False
    
    def __hash__(self):
        return hash((self.lhs, tuple(self.rhs)))


class Grammar:
    def __init__(self):
        self.productions = []
        self.non_terminals = set()
        self.terminals = set()
        self.start_symbol = None
    
    def add_production(self, lhs, rhs):
        """Add a production to the grammar."""
        self.non_terminals.add(lhs)
        self.productions.append(Production(lhs, rhs))
        
        # Update terminals and non-terminals sets
        for symbol in rhs:
            if symbol.is_terminal:
                self.terminals.add(symbol)
            else:
                self.non_terminals.add(symbol)
    
    def set_start_symbol(self, symbol):
        """Set the start symbol of the grammar."""
        self.start_symbol = symbol
        self.non_terminals.add(symbol)
    
    def __repr__(self):
        result = []
        for production in self.productions:
            result.append(str(production))
        return '\n'.join(result)


class Item:
    def __init__(self, production, dot_position, lookahead=None):
        self.production = production
        self.dot_position = dot_position
        self.lookahead = lookahead if lookahead is not None else set()
    
    def __repr__(self):
        rhs_with_dot = list(self.production.rhs)
        rhs_with_dot.insert(self.dot_position, "•")
        lookahead_str = ""
        if self.lookahead:
            lookahead_str = f", {'/'.join(str(l) for l in self.lookahead)}"
        return f"[{self.production.lhs} ::= {' '.join(str(s) for s in rhs_with_dot)}{lookahead_str}]"
    
    def __eq__(self, other):
        if isinstance(other, Item):
            return (self.production == other.production and 
                    self.dot_position == other.dot_position and 
                    self.lookahead == other.lookahead)
        return False
    
    def __hash__(self):
        return hash((self.production, self.dot_position, frozenset(self.lookahead) if self.lookahead else None))
    
    def get_next_symbol(self):
        """Get the symbol after the dot or None if dot is at the end."""
        if self.dot_position < len(self.production.rhs):
            return self.production.rhs[self.dot_position]
        return None
    
    def advance_dot(self):
        """Create a new item with the dot advanced one position."""
        return Item(self.production, self.dot_position + 1, self.lookahead)


class EBNFParser:
    def __init__(self, grammar_text):
        self.grammar_text = grammar_text
        self.grammar = Grammar()
        self.symbol_table = {}  # Map symbol names to Symbol objects
    
    def parse(self):
        """Parse the EBNF grammar text and build the Grammar object."""
        # Split the grammar text into lines and process each line
        lines = self.grammar_text.strip().split('\n')
        for line in lines:
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            
            # Parse a production rule
            parts = line.split('::=')
            if len(parts) == 2:
                lhs_str = parts[0].strip()
                rhs_str = parts[1].strip()
                
                # Extract the non-terminal name
                lhs_name = lhs_str.strip('<>').strip()
                lhs = self.get_or_create_symbol(lhs_name, False)
                
                # Set the start symbol if not already set
                if not self.grammar.start_symbol:
                    self.grammar.set_start_symbol(lhs)
                
                # Process the right-hand side
                self.process_rhs(lhs, rhs_str)
            
        return self.grammar
    
    def get_or_create_symbol(self, name, is_terminal):
        """Get a symbol from the symbol table or create a new one."""
        if name not in self.symbol_table:
            self.symbol_table[name] = Symbol(name, is_terminal)
        return self.symbol_table[name]
    
    def process_rhs(self, lhs, rhs_str):
        """Process the right-hand side of a production rule."""
        # Split by '|' to handle alternatives
        alternatives = rhs_str.split('|')
        for alt in alternatives:
            alt = alt.strip()
            if not alt:
                continue
            
            rhs = []
            # Process the symbols in the alternative
            i = 0
            while i < len(alt):
                if alt[i] == '<':
                    # Non-terminal symbol
                    end = alt.find('>', i)
                    if end != -1:
                        name = alt[i+1:end]
                        rhs.append(self.get_or_create_symbol(name, False))
                        i = end + 1
                    else:
                        i += 1
                elif alt[i].isalpha() or alt[i] == '_':
                    # Terminal symbol (identifier)
                    start = i
                    while i < len(alt) and (alt[i].isalnum() or alt[i] == '_'):
                        i += 1
                    name = alt[start:i]
                    rhs.append(self.get_or_create_symbol(name, True))
                else:
                    # Skip whitespace and other characters
                    i += 1
            
            if rhs:
                self.grammar.add_production(lhs, rhs)


class LRTableGenerator:
    def __init__(self, grammar):
        self.grammar = grammar
        self.augmented_grammar = self.augment_grammar()
        self.first_sets = {}
        self.follow_sets = {}
        self.canonical_collection = []
        self.goto_table = {}
        self.action_table = {}
        self.EOF = Symbol('$', True)
    
    def augment_grammar(self):
        """Augment the grammar with a new start production S' -> S."""
        augmented = Grammar()
        new_start = Symbol("S'", False)
        augmented.set_start_symbol(new_start)
        
        # S' -> S
        augmented.add_production(new_start, [self.grammar.start_symbol])
        
        # Add all original productions
        for production in self.grammar.productions:
            augmented.add_production(production.lhs, production.rhs)
        
        return augmented
    
    def compute_first_sets(self):
        """Compute FIRST sets for all symbols in the grammar."""
        # Initialize FIRST sets
        for terminal in self.augmented_grammar.terminals:
            self.first_sets[terminal] = {terminal}
        
        # Add the EOF symbol to first_sets
        self.first_sets[self.EOF] = {self.EOF}  # Add this line
        
        for non_terminal in self.augmented_grammar.non_terminals:
            self.first_sets[non_terminal] = set()
        
        # Iteratively compute FIRST sets until no changes
        changed = True
        while changed:
            changed = False
            for production in self.augmented_grammar.productions:
                lhs = production.lhs
                rhs = production.rhs
                
                # Empty production (epsilon)
                if not rhs:
                    if None not in self.first_sets[lhs]:
                        self.first_sets[lhs].add(None)
                        changed = True
                    continue
                
                # Compute FIRST for the RHS
                k = 0
                while k < len(rhs):
                    symbol = rhs[k]
                    # Add all symbols from FIRST(symbol) except epsilon to FIRST(lhs)
                    for s in self.first_sets[symbol]:
                        if s is not None and s not in self.first_sets[lhs]:
                            self.first_sets[lhs].add(s)
                            changed = True
                    
                    # If epsilon is not in FIRST(symbol), stop here
                    if None not in self.first_sets[symbol]:
                        break
                    
                    k += 1
                
                # If all symbols in RHS can derive epsilon, add epsilon to FIRST(lhs)
                if k == len(rhs) and None not in self.first_sets[lhs]:
                    self.first_sets[lhs].add(None)
                    changed = True
        
        return self.first_sets
    
    def compute_follow_sets(self):
        """Compute FOLLOW sets for all non-terminals in the grammar."""
        # Initialize FOLLOW sets
        for non_terminal in self.augmented_grammar.non_terminals:
            self.follow_sets[non_terminal] = set()
        
        # Add EOF to FOLLOW(S') where S' is the start symbol
        self.follow_sets[self.augmented_grammar.start_symbol].add(self.EOF)
        
        # Iteratively compute FOLLOW sets until no changes
        changed = True
        while changed:
            changed = False
            for production in self.augmented_grammar.productions:
                lhs = production.lhs
                rhs = production.rhs
                
                for i, symbol in enumerate(rhs):
                    if not symbol.is_terminal:  # Only non-terminals have FOLLOW sets
                        # Case 1: A -> αBβ, add FIRST(β) - {ε} to FOLLOW(B)
                        first_of_beta = self.compute_first_of_sequence(rhs[i+1:])
                        for s in first_of_beta:
                            if s is not None and s not in self.follow_sets[symbol]:
                                self.follow_sets[symbol].add(s)
                                changed = True
                        
                        # Case 2: A -> αB or A -> αBβ where FIRST(β) contains ε
                        # Add FOLLOW(A) to FOLLOW(B)
                        if i == len(rhs) - 1 or None in first_of_beta:
                            for s in self.follow_sets[lhs]:
                                if s not in self.follow_sets[symbol]:
                                    self.follow_sets[symbol].add(s)
                                    changed = True
        
        return self.follow_sets
    
    def compute_first_of_sequence(self, symbols):
        """Compute FIRST set for a sequence of symbols."""
        result = set()
        
        for symbol in symbols:
            # Add all symbols from FIRST(symbol) except epsilon to result
            for s in self.first_sets[symbol]:
                if s is not None:
                    result.add(s)
            
            # If epsilon is not in FIRST(symbol), stop here
            if None not in self.first_sets[symbol]:
                return result
        
        # If all symbols can derive epsilon, add epsilon to result
        result.add(None)
        return result
    
    def closure(self, items):
        """Compute the closure of a set of LR(1) items."""
        result = set(items)
        changed = True
        
        while changed:
            changed = False
            new_items = set()
            
            for item in result:
                next_symbol = item.get_next_symbol()
                if next_symbol and not next_symbol.is_terminal:
                    # For each production B -> γ
                    for production in self.augmented_grammar.productions:
                        if production.lhs == next_symbol:
                            # Compute lookaheads
                            lookaheads = set()
                            beta = item.production.rhs[item.dot_position + 1:]
                            first_beta_a = self.compute_first_of_sequence(beta + [s for s in item.lookahead])
                            
                            for symbol in first_beta_a:
                                if symbol is not None:
                                    lookaheads.add(symbol)
                            
                            # Create new item and add to closure
                            new_item = Item(production, 0, lookaheads)
                            if new_item not in result:
                                new_items.add(new_item)
                                changed = True
            
            result.update(new_items)
        
        return list(result)
    
    def goto(self, items, symbol):
        """Compute goto(I, X) where I is a set of items and X is a grammar symbol."""
        result = set()
        
        for item in items:
            next_symbol = item.get_next_symbol()
            if next_symbol == symbol:
                result.add(item.advance_dot())
        
        return self.closure(result)
    
    def build_canonical_collection(self):
        """Build the canonical collection of LR(1) items."""
        # Start with the initial item: [S' -> .S, $]
        start_production = self.augmented_grammar.productions[0]
        initial_item = Item(start_production, 0, {self.EOF})
        initial_set = self.closure([initial_item])
        
        self.canonical_collection = [initial_set]
        set_indices = {frozenset(initial_set): 0}
        
        i = 0
        while i < len(self.canonical_collection):
            current_set = self.canonical_collection[i]
            
            # Compute goto for each grammar symbol
            for symbol in self.augmented_grammar.terminals.union(self.augmented_grammar.non_terminals):
                goto_set = self.goto(current_set, symbol)
                
                if goto_set:
                    # Check if this state already exists
                    frozen_goto = frozenset(goto_set)
                    if frozen_goto not in set_indices:
                        self.canonical_collection.append(goto_set)
                        set_indices[frozen_goto] = len(self.canonical_collection) - 1
                    
                    # Update goto table
                    self.goto_table[(i, symbol)] = set_indices[frozen_goto]
            
            i += 1
        
        return self.canonical_collection
    
    def build_lr_parsing_table(self):
        """Build the LR parsing table (action and goto tables) with conflict detection."""
        # Compute FIRST and FOLLOW sets
        self.compute_first_sets()
        self.compute_follow_sets()
        
        # Build canonical collection
        self.build_canonical_collection()
        
        # Track conflicts
        self.conflicts = {
            'shift_reduce': [],
            'reduce_reduce': []
        }
        
        # Build the action and goto tables
        for i, state in enumerate(self.canonical_collection):
            for item in state:
                next_symbol = item.get_next_symbol()
                
                if next_symbol is None:  # Dot at the end
                    # If the item is [S' -> S., $], add accept action
                    if item.production.lhs == self.augmented_grammar.start_symbol and item.production.rhs == [self.grammar.start_symbol]:
                        if (i, self.EOF) in self.action_table:
                            existing_action, _ = self.action_table[(i, self.EOF)]
                            self.conflicts['shift_reduce'].append((i, self.EOF, existing_action, 'accept'))
                        self.action_table[(i, self.EOF)] = ('accept', None)
                    else:
                        # Reduce action for all symbols in the lookahead
                        for lookahead in item.lookahead:
                            if (i, lookahead) in self.action_table:
                                existing_action, existing_value = self.action_table[(i, lookahead)]
                                if existing_action == 'shift':
                                    # Shift-reduce conflict
                                    self.conflicts['shift_reduce'].append(
                                        (i, lookahead, f"{existing_action} {existing_value}", f"reduce {item.production}")
                                    )
                                elif existing_action == 'reduce':
                                    # Reduce-reduce conflict
                                    self.conflicts['reduce_reduce'].append(
                                        (i, lookahead, f"{existing_action} {existing_value}", f"reduce {item.production}")
                                    )
                                # By default, we'll keep the existing action (shift preference)
                            else:
                                self.action_table[(i, lookahead)] = ('reduce', item.production)
                
                elif next_symbol.is_terminal:  # Shift action
                    if (i, next_symbol) in self.goto_table:
                        next_state = self.goto_table[(i, next_symbol)]
                        if (i, next_symbol) in self.action_table:
                            existing_action, existing_value = self.action_table[(i, next_symbol)]
                            if existing_action == 'reduce':
                                # Shift-reduce conflict
                                self.conflicts['shift_reduce'].append(
                                    (i, next_symbol, f"{existing_action} {existing_value}", f"shift {next_state}")
                                )
                        self.action_table[(i, next_symbol)] = ('shift', next_state)
            
            # Add goto actions for non-terminals
            for symbol in self.augmented_grammar.non_terminals:
                if (i, symbol) in self.goto_table:
                    self.goto_table[(i, symbol)] = self.goto_table[(i, symbol)]
        
        return self.action_table, self.goto_table

    def print_conflicts(self):
        """Print all detected conflicts in the LR table."""
        if not hasattr(self, 'conflicts'):
            print("Build the LR table first to detect conflicts.")
            return
        
        print("\n=== LR Table Conflicts ===")
        
        # Print shift-reduce conflicts
        if self.conflicts['shift_reduce']:
            print("\nShift-Reduce Conflicts:")
            print(f"{'State':<6}{'Symbol':<10}{'Existing Action':<20}{'New Action':<20}")
            print("-" * 56)
            for state, symbol, existing, new in self.conflicts['shift_reduce']:
                print(f"{state:<6}{str(symbol):<10}{existing:<20}{new:<20}")
        else:
            print("\nNo shift-reduce conflicts detected.")
        
        # Print reduce-reduce conflicts
        if self.conflicts['reduce_reduce']:
            print("\nReduce-Reduce Conflicts:")
            print(f"{'State':<6}{'Symbol':<10}{'Existing Action':<20}{'New Action':<20}")
            print("-" * 56)
            for state, symbol, existing, new in self.conflicts['reduce_reduce']:
                print(f"{state:<6}{str(symbol):<10}{existing:<20}{new:<20}")
        else:
            print("\nNo reduce-reduce conflicts detected.")
        
        # Summary
        total_conflicts = len(self.conflicts['shift_reduce']) + len(self.conflicts['reduce_reduce'])
        print(f"\nTotal conflicts: {total_conflicts}")
        if total_conflicts > 0:
            print("Note: By default, shift actions take precedence over reduce actions in conflict cases.")
    def print_lr_table(self):
        """Print the LR parsing table in a readable format."""
        # Collect all symbols for table headers
        all_terminals = list(self.augmented_grammar.terminals) + [self.EOF]
        all_non_terminals = list(self.augmented_grammar.non_terminals)
        
        # Print the table header
        header = "State | "
        for terminal in all_terminals:
            header += f"{terminal} | "
        for non_terminal in all_non_terminals:
            header += f"{non_terminal} | "
        print(header)
        print("-" * len(header))
        
        # Print each state's row
        for i in range(len(self.canonical_collection)):
            row = f"{i} | "
            
            # Print action entries for terminals
            for terminal in all_terminals:
                if (i, terminal) in self.action_table:
                    action, value = self.action_table[(i, terminal)]
                    if action == 'shift':
                        row += f"s{value} | "
                    elif action == 'reduce':
                        # Find the index of the production for reduce actions
                        prod_idx = self.augmented_grammar.productions.index(value)
                        row += f"r{prod_idx} | "
                    elif action == 'accept':
                        row += "acc | "
                else:
                    row += "   | "
            
            # Print goto entries for non-terminals
            for non_terminal in all_non_terminals:
                if (i, non_terminal) in self.goto_table:
                    row += f"{self.goto_table[(i, non_terminal)]} | "
                else:
                    row += "  | "
            
            print(row)
        print("-" * len(header))
    def save_lr_table_to_csv(self, filename):
        """Save the LR parsing table to a CSV file."""
        # Collect all symbols for table headers
        all_terminals = list(self.augmented_grammar.terminals) + [self.EOF]
        all_non_terminals = list(self.augmented_grammar.non_terminals)

        # Prepare the header row
        headers = ["State"] + [str(terminal) for terminal in all_terminals] + [str(non_terminal) for non_terminal in all_non_terminals]

        # Prepare the data rows
        rows = []
        for i in range(len(self.canonical_collection)):
            row = [i]

            # Add action entries for terminals
            for terminal in all_terminals:
                if (i, terminal) in self.action_table:
                    action, value = self.action_table[(i, terminal)]
                    if action == 'shift':
                        row.append(f"s{value}")
                    elif action == 'reduce':
                        # Find the index of the production for reduce actions
                        prod_idx = self.augmented_grammar.productions.index(value)
                        row.append(f"r{prod_idx}")
                    elif action == 'accept':
                        row.append("acc")
                else:
                    row.append("")

            # Add goto entries for non-terminals
            for non_terminal in all_non_terminals:
                if (i, non_terminal) in self.goto_table:
                    row.append(self.goto_table[(i, non_terminal)])
                else:
                    row.append("")

            rows.append(row)

        # Write to CSV file
        with open(filename, 'w', newline='') as csvfile:
            writer = csv.writer(csvfile)
            writer.writerow(headers)  # Write the header
            writer.writerows(rows)

    def save_lr_table_to_json(self, filename):
        """Save the LR parsing table to a JSON file."""
        table_data = {
            "action_table": {},
            "goto_table": {},
            "productions": [str(prod) for prod in self.augmented_grammar.productions]
        }

        # Convert action table to a serializable format
        for key, value in self.action_table.items():
            state, symbol = key
            action, val = value
            table_data["action_table"][f"{state},{symbol}"] = {
                "action": action,
                "value": str(val) if val else None
            }

        # Convert goto table to a serializable format
        for key, value in self.goto_table.items():
            state, symbol = key
            table_data["goto_table"][f"{state},{symbol}"] = value

        # Save to JSON file
        with open(filename, 'w') as f:
            json.dump(table_data, f, indent=4)

    def save_lr_table_to_excel(self, filename):
        """Save the LR parsing table to an Excel file."""
        wb = openpyxl.Workbook()
        ws = wb.active

        # Collect all symbols for table headers
        all_terminals = list(self.augmented_grammar.terminals) + [self.EOF]
        all_non_terminals = list(self.augmented_grammar.non_terminals)

        # Write the table header
        headers = ["State"] + [str(terminal) for terminal in all_terminals] + [str(non_terminal) for non_terminal in all_non_terminals]
        ws.append(headers)

        # Write each state's row
        for i in range(len(self.canonical_collection)):
            row = [i]

            # Write action entries for terminals
            for terminal in all_terminals:
                if (i, terminal) in self.action_table:
                    action, value = self.action_table[(i, terminal)]
                    if action == 'shift':
                        row.append(f"s{value}")
                    elif action == 'reduce':
                        # Find the index of the production for reduce actions
                        prod_idx = self.augmented_grammar.productions.index(value)
                        row.append(f"r{prod_idx}")
                    elif action == 'accept':
                        row.append("acc")
                else:
                    row.append("")

            # Write goto entries for non-terminals
            for non_terminal in all_non_terminals:
                if (i, non_terminal) in self.goto_table:
                    row.append(self.goto_table[(i, non_terminal)])
                else:
                    row.append("")

            ws.append(row)

        # Save the workbook
        wb.save(filename)

    def save_lr_table_to_file(self, filename):
        """Save the LR parsing table to a file."""
        with open(filename, 'w') as f:
            # Collect all symbols for table headers
            all_terminals = list(self.augmented_grammar.terminals) + [self.EOF]
            all_non_terminals = list(self.augmented_grammar.non_terminals)
            
            # Write the table header
            header = "State | "
            for terminal in all_terminals:
                header += f"{terminal} | "
            for non_terminal in all_non_terminals:
                header += f"{non_terminal} | "
            f.write(header + '\n')
            f.write("-" * len(header) + '\n')
            
            # Write each state's row
            for i in range(len(self.canonical_collection)):
                row = f"{i} | "
                
                # Write action entries for terminals
                for terminal in all_terminals:
                    if (i, terminal) in self.action_table:
                        action, value = self.action_table[(i, terminal)]
                        if action == 'shift':
                            row += f"s{value} | "
                        elif action == 'reduce':
                            # Find the index of the production for reduce actions
                            prod_idx = self.augmented_grammar.productions.index(value)
                            row += f"r{prod_idx} | "
                        elif action == 'accept':
                            row += "acc | "
                    else:
                        row += "   | "
                
                # Write goto entries for non-terminals
                for non_terminal in all_non_terminals:
                    if (i, non_terminal) in self.goto_table:
                        row += f"{self.goto_table[(i, non_terminal)]} | "
                    else:
                        row += "  | "
                
                f.write(row + '\n')
            f.write("-" * len(header) + '\n')


def parse_ebnf_and_build_lr_table(grammar_file_path):
    """Parse EBNF grammar from file and build LR parsing table."""
    # Read grammar from file
    try:
        with open(grammar_file_path, 'r') as file:
            grammar_text = file.read()
    except FileNotFoundError:
        print(f"Error: File '{grammar_file_path}' not found.")
        return None
    except Exception as e:
        print(f"Error reading file: {e}")
        return None
    
    # Parse grammar and build table
    parser = EBNFParser(grammar_text)
    grammar = parser.parse()
    
    generator = LRTableGenerator(grammar)
    action_table, goto_table = generator.build_lr_parsing_table()
    
    return generator

def main():
    import argparse

    # Set up command-line argument parsing
    parser = argparse.ArgumentParser(description='Generate LR parsing table from EBNF grammar file.')
    parser.add_argument('grammar_file', help='Path to the EBNF grammar file')
    parser.add_argument('-o', '--output', help='Output file for the LR table', default='output.txt')
    parser.add_argument('--log-conflicts', action='store_true', help='Log parsing conflicts to a file')
    args = parser.parse_args()

    # Parse grammar and build table
    generator = parse_ebnf_and_build_lr_table(args.grammar_file)

    if generator:
        print("Grammar parsed successfully.")
        print(f"Found {len(generator.augmented_grammar.non_terminals)} non-terminals and {len(generator.augmented_grammar.terminals)} terminals.")
        print(f"Generated {len(generator.canonical_collection)} LR states.")
        
        # Print conflicts
        generator.print_conflicts()
        
        # Log conflicts to file if requested
        if args.log_conflicts and hasattr(generator, 'conflicts'):
            conflict_file = args.output.rsplit('.', 1)[0] + '_conflicts.txt'
            with open(conflict_file, 'w') as f:
                f.write("=== LR Table Conflicts ===\n")
                
                # Write shift-reduce conflicts
                f.write("\nShift-Reduce Conflicts:\n")
                if generator.conflicts['shift_reduce']:
                    f.write(f"{'State':<6}{'Symbol':<10}{'Existing Action':<20}{'New Action':<20}\n")
                    f.write("-" * 56 + "\n")
                    for state, symbol, existing, new in generator.conflicts['shift_reduce']:
                        f.write(f"{state:<6}{str(symbol):<10}{existing:<20}{new:<20}\n")
                else:
                    f.write("No shift-reduce conflicts detected.\n")
                
                # Write reduce-reduce conflicts
                f.write("\nReduce-Reduce Conflicts:\n")
                if generator.conflicts['reduce_reduce']:
                    f.write(f"{'State':<6}{'Symbol':<10}{'Existing Action':<20}{'New Action':<20}\n")
                    f.write("-" * 56 + "\n")
                    for state, symbol, existing, new in generator.conflicts['reduce_reduce']:
                        f.write(f"{state:<6}{str(symbol):<10}{existing:<20}{new:<20}\n")
                else:
                    f.write("No reduce-reduce conflicts detected.\n")
                
                # Summary
                total_conflicts = len(generator.conflicts['shift_reduce']) + len(generator.conflicts['reduce_reduce'])
                f.write(f"\nTotal conflicts: {total_conflicts}\n")
                if total_conflicts > 0:
                    f.write("Note: By default, shift actions take precedence over reduce actions in conflict cases.\n")
            
            print(f"Conflict log saved to {conflict_file}")

        # Determine the output format based on the file extension
        if args.output.endswith('.txt'):
            generator.save_lr_table_to_file(args.output)
        elif args.output.endswith('.xlsx'):
            generator.save_lr_table_to_excel(args.output)
        elif args.output.endswith('.json'):
            generator.save_lr_table_to_json(args.output)
        elif args.output.endswith('.csv'):
            generator.save_lr_table_to_csv(args.output)
        else:
            print("Unsupported output format. Please use .txt, .xlsx, .json, or .csv.")
            return

        print(f"LR table saved to {args.output}")

if __name__ == "__main__":
    main()