# Programming Language Concepts Project
Class project for: **COP4020**

## Part 1 | Lexer

### Token Types

 - **IDENTIFIER**
   - Represents keywords and names used for variables, functions, etc. Allows alphanumeric characters, underscores, and hyphens (`[A-Za-z0-9_-]`), but cannot start with a digit or a hyphen.
 - **INTEGER/DECIMAL**
   - Numbers, naturally either integers or decimals. As in our Regex homework, all numbers start with an optional sign (`+`/`-`) followed by one or more digits. Decimal numbers are determined by a decimal point `.` *and* one or more digits.
 - **CHARACTER**
   - A character literal. Similar to string literals below, however start and end with a single quote (`'`) and must contain one and only one character. Escape characters are also supported starting with a backslash (`\`), which must be followed by one of `bnrt'"\` (and are considered one character). The character cannot be a single quote (`'`), since that ends a character literal, or a line ending (`\n/\r`), to avoid character literals spanning multiple lines.
 - **STRING**
   - A string literal. As in our Regex homework, strings start and end with a double quote (`"`) and support escape characters starting with a backslash (`\`), which must be followed by one of `bnrt'"\`. Characters cannot be a double quote (`"`), since that ends a string literal, or a line ending (`\n/\r`), to avoid string literals spanning multiple lines. This is particularly important for strings, which could cause cascading errors if they covered multiple lines (try an unterminated string vs an unterminated block comment and see what happens).
 - **OPERATOR**
   - Any other character, excluding whitespace. Comparison operators (`<=`, `>=`, `!=`, `==`) are a special case and will be combined in to a single token, for all other characters an OPERATOR token is only that character.
 - **WHITESPACE**
   - Whitespace characters (` \b\n\r\t`) should be skipped by the lexer and not emitted as tokens. However, they are still meaningful when determining where a token starts/ends (12 is one INTEGER token, but 1 2 is two INTEGER tokens).
 
#### Examples

LET x = 5;
 - Token(IDENTIFIER, "LET", 0)
 - Token(IDENTIFIER, "x", 4)
 - Token(OPERATOR, "=", 6)
 - Token(INTEGER, "5", 8)
 - Token(OPERATOR, ";", 9)

print("Hello, World!");
 - Token(IDENTIFIER, "print", 0)
 - Token(OPERATOR, "(", 5)
 - Token(STRING, "\"Hello, World!\"", 6)
 - Token(OPERATOR, ")", 21)
 - Token(OPERATOR, ";", 22)
