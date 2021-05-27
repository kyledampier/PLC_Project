package plc.project;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid or missing.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation a lot easier.
 */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> tokens = new ArrayList<>();
        while(chars.has(0)) {
            if(!(match(" ") || match("\b") || match("\n") || match("\r") || match("\t"))) // checks whitespace
                tokens.add(lexToken());
            else {
                chars.skip();
            }
        }
        return tokens;
//        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        String whitespace = "[ \b\n\r\t]";
        String identifier = "[A-Za-z_]";
        String sign = "[\\+-]?";
        String number = "[0-9]+";
        String character = "'";
        String operator = "([<>!=] '='?|(.))";

        if(peek(identifier)) { // identifier
            return lexIdentifier();
        }
        else if(peek(number)) { // number
            return lexNumber();
        }
        else if(peek(character)) {
            return lexCharacter();
        }
        else if(peek("\"")) {
            return lexString();
        }
        else if(peek(operator)) {
            return lexOperator();
        }
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexIdentifier() {
        System.out.println("Identifier found");
        match("[A-Za-z_]"); // matches the first character for an Identifier
        while(match("[A-Za-z0-9_-]")); // ensures the next character is valid and advances past the current
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        System.out.println("Number found");
        if (peek("[\\+-]")) {
            match("[\\+-]");
        }

        if (peek("[0-9]")) {
            while (match("[0-9]")); // Match until decimal

            if (peek("\\.", "[0-9]+")) {
                match("\\."); // match decimal
                while (match("[0-9]")); // Match until NaN
                return chars.emit(Token.Type.DECIMAL);
            } else {
                return chars.emit(Token.Type.INTEGER);
            }
        }
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexCharacter() {
        System.out.println("Character found");
        match("'");
        String acceptedChars = "([^'\\n\\r])";

        if (peek("\\\\") || peek(acceptedChars)) {
            if(peek("\\\\"))
                lexEscape();
            else
                match(acceptedChars);
            match("'");
            return chars.emit(Token.Type.CHARACTER);
        }

        throw new plc.project.ParseException("invalid character", chars.index); //TODO
    }

    public Token lexString() {
        System.out.println("String found");
        String acceptedChars = "[^\\\\\"\\n\\r]";

        match("\""); // Start of String
        while (!peek("\"")) {
            if (peek("\\\\")) { // escape sequence started
                lexEscape();
            }
            else if (peek(acceptedChars)) { // regular characters
                match(acceptedChars);
            }
            else {
                throw new plc.project.ParseException("invalid string", chars.index);
            }
        }
        match("\""); // End of String found

        return chars.emit(Token.Type.STRING);
    }

    public void lexEscape() {
        System.out.println("Escape found");
        if(!match("\\\\", "[bnrt'\"\\\\]")) {
            throw new plc.project.ParseException("invalid escape character", chars.index);
        }
    }

    public Token lexOperator() {
        System.out.println("Operator found");
        if(match("<", "=") || match(">", "=") || match("!", "=") || match("=", "=")) {
            return chars.emit(Token.Type.OPERATOR);
        } else if(match("([<>!=] '='?|(.))")) {
            return chars.emit(Token.Type.OPERATOR);
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);

        if (peek) {
            for (int i =0; i < patterns.length; i++) {
                chars.advance();
            }
        }

        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
