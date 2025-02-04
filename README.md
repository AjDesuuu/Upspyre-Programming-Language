# Upspyre Programming Language

Upspyre is a custom-built programming language designed to introduce kids to coding. It combines **Java's strictness** with **Python's writability**, providing a structured yet easy-to-understand syntax that fosters logical thinking and problem-solving skills.

## Features
- **Beginner-friendly syntax** with intuitive keywords.
- **Supports loops, conditionals, and functions** for structured learning.
- **Type inference** for simplified variable handling.
- **List and pair-map support** for basic data structures.
- **Readable control structures** like `repeat`, `choose-what`, and `method`.
- **Simple input/output system** using `get` and `show`.

---

## Installation

### **Prerequisites**
Before using Upspyre, ensure you have Python and PLY installed on your system.

#### **1. Install Python (If Not Installed)**
Download and install Python from the official website: [Python.org](https://www.python.org/downloads/)

Verify Python installation:
```sh
python --version
```
(You should see Python 3.x.x)

#### **2. Install PLY (Python Lex-Yacc)**
PLY is required for lexing and parsing Upspyre programs.

Run the following command:
```sh
pip install ply
```

Verify installation:
```sh
python -c "import ply; print('PLY installed successfully!')"
```

---

## Getting Started

### **1. Clone the Repository**
```sh
git clone https://github.com/yourusername/Upspyre-Programming-Language.git
cd Upspyre-Programming-Language
```

### **2. Run the Lexer**
The lexer scans the code and extracts tokens.
```sh
python upspyre_lexer.py
```
You can modify `test_code` inside `upspyre_lexer.py` to test different inputs.

### **3. Run the Parser**
The parser checks if the Upspyre program follows the correct syntax.
```sh
python upspyre_parser.py
```
This will parse a test Upspyre program and validate its structure.

---

## Usage

### **Writing an Upspyre Program**
Save your Upspyre code in a `.up` file and ensure it follows the syntax rules.

Example **Upspyre Program:**
```up
start
    x = 5;
    y = 3.14;
    show "Hello, World!";
    if (x > 3) {
        output x + y;
    }
end
```

### **Running an Upspyre Program**
Modify `upspyre_parser.py` to read and parse `.up` files:
```sh
python upspyre_parser.py yourfile.up
```

---

## Roadmap & Future Features
- Basic syntax validation (✔ Done)
- Arithmetic & logical expressions (✔ Done)

---

