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
//            String current = String.valueOf((chars.get(chars.index)));

            if(peek(" ") || peek("\b") || peek("\n") || peek("\r") || peek("\t")) { // whitespace
                chars.skip();
            } else {
                tokens.add(lexToken());
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
        String number = "[\\+\\-]? [0-9]+";
        String escape = "\\[bnrt'\"\\]";
        String character = "['] ([^'\n\r\\] | "+escape+") [']";
        String operator = "[^"+whitespace+identifier+number+escape+character+"]";
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
        int index = chars.index; // token starts at this index
        StringBuilder literal = new StringBuilder();

        literal.append(chars.get(0));
        match("[A-Za-z_]"); // matches the first character for an Identifier

        if(peek("[A-Za-z0-9_-]")) { // ensures the next character is valid for an Identifier without advancing
            do {
                if(chars.has(0))
                    literal.append(chars.get(0)); // builds the token
            }
            while(match("[A-Za-z0-9_-]")); // ensures the next character is valid and advances past the current
        }
        return new Token(Token.Type.IDENTIFIER, literal.toString(), index);
//        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexNumber() {
        System.out.println("Number found");
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexCharacter() {
        System.out.println("Character found");
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexString() {
        System.out.println("String found");
        throw new UnsupportedOperationException(); //TODO
    }

    public void lexEscape() {
        System.out.println("Escape found");
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexOperator() {
        System.out.println("Operator found");
        throw new UnsupportedOperationException(); //TODO
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
