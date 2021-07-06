package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Pattern;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    private ParseException errorHandle(String message) {
        if (tokens.has(0)) {
            return new ParseException(message, tokens.get(0).getIndex());
        } else {
            return new ParseException(message, (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }
    }

    /**
     * Parses the {@code source} rule.
     */
    // source ::= field* method*
    public Ast.Source parseSource() throws ParseException {
        try {
            List<Ast.Field> fields = new ArrayList<>();
            List<Ast.Method> methods = new ArrayList<>();
            while (tokens.has(0)) {
                if (match("LET")) {
                    fields.add(parseField());
                } else if (match("DEF")) {
                    methods.add(parseMethod());
                }
            }
            return new Ast.Source(fields, methods);
        } catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses the {@code f     * next tokens start a field, aka {@code LET}.ield} rule. This method should only be called if the
     */
    // field ::= 'LET' identifier ('=' expression)? ';'
    public Ast.Field parseField() throws ParseException {
        try {
            Ast.Stmt.Declaration declaration = parseDeclarationStatement();
            return new Ast.Field(declaration.getName(), declaration.getValue());
        } catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    // method ::= 'DEF' identifier '(' (identifier (',' identifier)*)? ')' 'DO' statement* 'END'
    public Ast.Method parseMethod() throws ParseException {
        try {
            if (match(Token.Type.IDENTIFIER)) {
                String functionName = tokens.get(-1).getLiteral();
                if (match("(")) {
                    List<String> params = new ArrayList<>();
                    // get all params
                    while (match(Token.Type.IDENTIFIER)) {
                        params.add(tokens.get(-1).getLiteral());
                        if (!match(",")) {
                            if (!peek(")")) {
                                throw errorHandle("Expected comma between identifiers");
                            }
                        }
                    }

                    // check for closing parenthesis
                    if (!match(")")) {
                        throw errorHandle("Expected Parenthesis");
                    }

                    // check for DO
                    if (!match("DO")) {
                        throw errorHandle("Expected DO statement");
                    }

                    // get all statements
                    List<Ast.Stmt> statements = new ArrayList<>();
                    while (!match("END") && tokens.has(0)) {
                        statements.add(parseStatement());
                    }
                    if(!tokens.get(-1).getLiteral().equals("END")) {
                        throw new ParseException("missing END", tokens.get(-1).getIndex());
                    }
                    return new Ast.Method(functionName, params, statements);
                } else {
                    throw errorHandle("Expected Parenthesis");
                }
            } else {
                throw errorHandle("Expected Identifier");
            }
        } catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    //statement ::=
    //    'LET' identifier ('=' expression)? ';' |
    //    'IF' expression 'DO' statement* ('ELSE' statement*)? 'END' |
    //    'FOR' identifier 'IN' expression 'DO' statement* 'END' |
    //    'WHILE' expression 'DO' statement* 'END' |
    //    'RETURN' expression ';' |
    //    expression ('=' expression)? ';'
    public Ast.Stmt parseStatement() throws ParseException {
        try {
            if (match("LET")) {
                return parseDeclarationStatement();
            } else if (match("IF")) {
                return parseIfStatement();
            } else if (match("FOR")) {
                return parseForStatement();
            } else if (match("WHILE")) {
                return parseWhileStatement();
            } else if (match("RETURN")) {
                return parseReturnStatement();
            } else {
                Ast.Stmt.Expr lhs = parseExpression();
                if (!match("=")) {
                    if (!match(";")) {
                        throw new ParseException("Expected semicolon", tokens.get(-1).getIndex());
                    }
                    return new Ast.Stmt.Expression(lhs);
                }

                Ast.Stmt.Expr rhs = parseExpression();

                if (!match(";")) {
                    throw new ParseException("Expected semicolon", tokens.get(-1).getIndex());
                }
                return new Ast.Stmt.Assignment(lhs, rhs);
            }

        } catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    //    'LET' identifier ('=' expression)? ';'
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {
        if (match(Token.Type.IDENTIFIER)) {
            String identifier = tokens.get(-1).getLiteral();
            if (match("=")) {
                Ast.Expr rhs = parseExpression();
                if (match(";")) {
                    return new Ast.Stmt.Declaration(identifier, Optional.of(rhs));
                }
            } else {
                if (match(";")) {
                    return new Ast.Stmt.Declaration(identifier, Optional.empty());
                }
            }
        } else {
            throw errorHandle("Invalid Identifier");
        }
        throw errorHandle("Expected semicolon");
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    // 'IF' expression 'DO' statement* ('ELSE' statement*)? 'END'
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        Ast.Expr expression = parseExpression();
        if (match("DO")) {
            boolean isElse = false;
            List<Ast.Stmt> doStatements = new ArrayList<>();
            List<Ast.Stmt> elseStatements = new ArrayList<>();

            while (!match("END") && tokens.has(0)) {
                if (match("ELSE")) {
                    if (!isElse) {
                        isElse = true;
                    } else {
                        throw errorHandle("Too many ELSE Statements");
                    }
                }
                if (isElse) {
                    elseStatements.add(parseStatement());
                } else {
                    doStatements.add(parseStatement());
                }
            }
            if(!tokens.get(-1).getLiteral().equals("END")) {
                throw new ParseException("Missing END", tokens.get(-1).getIndex());
            }

            return new Ast.Stmt.If(expression, doStatements, elseStatements);
        }
        throw errorHandle("Expected DO");
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    //    'FOR' identifier 'IN' expression 'DO' statement* 'END'
    public Ast.Stmt.For parseForStatement() throws ParseException {
        if (match(Token.Type.IDENTIFIER)) {
            String name = tokens.get(-1).getLiteral();
            if (!match("IN")) {
                throw errorHandle("Expected IN");
            }

            Ast.Expr expression = parseExpression();
            if (!match("DO")) {
                throw errorHandle("Expected DO");
            }

            List<Ast.Stmt> statements = new ArrayList<>();

            while (!match("END") && tokens.has(0)) {
                statements.add(parseStatement());
            }
            if(!tokens.get(-1).getLiteral().equals("END")) {
                throw errorHandle("Missing END");
            }

            return new Ast.Stmt.For(name, expression, statements);
        }
        throw errorHandle("Expected Token");
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        Ast.Expr expression = parseExpression();
        if (!match("DO")) {
            throw errorHandle("Expected DO");
        }

        List<Ast.Stmt> statements = new ArrayList<>();

        while (!match("END") && tokens.has(0)) {
            statements.add(parseStatement());
        }
        if(!tokens.get(-1).getLiteral().equals("END")) {
            throw new ParseException("missing END", tokens.get(-1).getIndex());
        }

        return new Ast.Stmt.While(expression, statements);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {
        Ast.Expr expression = parseExpression();
        if (!match(";")) {
            throw errorHandle("Expected semicolon.");
        }
        return new Ast.Stmt.Return(expression);
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expr parseExpression() throws ParseException {
        try {
            return parseLogicalExpression();
        } catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    // logical_expression ::= comparison_expression (('AND' | 'OR') comparison_expression)*
    public Ast.Expr parseLogicalExpression() throws ParseException {
        try {

            Ast.Expr output = parseEqualityExpression();

            while (match("AND") || match("OR")) { // right

                String operation = tokens.get(-1).getLiteral();
                Ast.Expr rightExpr = parseEqualityExpression();
                output = new Ast.Expr.Binary(operation, output, rightExpr);

            }
            return output;

        } catch(ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    // comparison_expression ::= additive_expression (('<' | '<=' | '>' | '>=' | '==' | '!=') additive_expression)*
    public Ast.Expr parseEqualityExpression() throws ParseException {
        try {
            Ast.Expr output = parseAdditiveExpression();
            while (match("!=")
                    || match("==")
                    || match(">=")
                    || match(">")
                    || match("<=")
                    || match("<")) {
                String operation = tokens.get(-1).getLiteral();
                Ast.Expr rightExpr = parseEqualityExpression();
                output = new Ast.Expr.Binary(operation, output, rightExpr);
            }
            return output;
        } catch(ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expr parseAdditiveExpression() throws ParseException {
        try {
            Ast.Expr output = parseMultiplicativeExpression();

            while (match("+") || match("-")) {
                String operation = tokens.get(-1).getLiteral();
                Ast.Expr rightExpr = parseMultiplicativeExpression();
                output = new Ast.Expr.Binary(operation, output, rightExpr);
            }
            return output;

        } catch(ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() throws ParseException {
        try {
            Ast.Expr output = parseSecondaryExpression();

            while (match("/") || match("*")) { // right
                String operation = tokens.get(-1).getLiteral();
                Ast.Expr rightExpr = parseSecondaryExpression();
                output = new Ast.Expr.Binary(operation, output, rightExpr);
            }
            return output;
        } catch(ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    // secondary_expression ::= primary_expression ('.' identifier ('(' (expression (',' expression)*)? ')')?)*
    public Ast.Expr parseSecondaryExpression() throws ParseException {
        try {
          Ast.Expr initialExpr = parsePrimaryExpression();

          while (match(".")) {
              if (!match(Token.Type.IDENTIFIER)) {
                  throw new ParseException("Invalid Identifier", tokens.get(0).getIndex());
              }
              // Identifier found
              String receiver = tokens.get(-1).getLiteral();
              if (!match("(")) { // No expression after
                  initialExpr = new Ast.Expr.Access(Optional.of(initialExpr), receiver);
              } else {
                  // Found '('
                  List<Ast.Expr> args = new ArrayList<>();
                  if(!match(")")) { // Found expression after
                      args.add(parseExpression());
                      while (match(",")) {
                          args.add(parseExpression());
                      }
                      if(!match(")")) { // Check closing parentheses
                          throw new ParseException("Invalid function closing parentheses not found", tokens.get(0).getIndex());
                      }
                  }
                  initialExpr = new Ast.Expr.Function(Optional.of(initialExpr), receiver, args);
              }
          }
          return initialExpr;

        } catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expr parsePrimaryExpression() throws ParseException {
        if (match("NIL")) {
            return new Ast.Expr.Literal(null);
        }
        else if (match("TRUE")) {
            return new Ast.Expr.Literal(true);
        }
        else if (match("FALSE")) {
            return new Ast.Expr.Literal(false);
        }
        else if (match(Token.Type.INTEGER)) { // INTEGER LITERAL FOUND
            return new Ast.Expr.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        }
        else if (match(Token.Type.DECIMAL)) { // DECIMAL LITERAL FOUND
            return new Ast.Expr.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        }
        else if (match(Token.Type.CHARACTER)) { // CHARACTER LITERAL FOUND
            String str = tokens.get(-1).getLiteral();
//            str = str.replace("\\n", "\n")
//                    .replace("\\t", "\t")
//                    .replace("\\b", "\b")
//                    .replace("\\r", "\r")
//                    .replace("\\'", "'")
//                    .replace("\\\\", "\\")
//                    .replace("\\\"", "\"");
//            if(str.contains("\n") || str.contains("\t") || str.contains("\b") || str.contains("\r") || str.contains("\\") || str.contains("\"") || str.charAt(1) == '\'')
//                return new Ast.Expr.Literal(str)
            return new Ast.Expr.Literal(str.charAt(1));
        }
        else if (match(Token.Type.STRING)) { // STRING LITERAL FOUND
            String str = tokens.get(-1).getLiteral();
            str = str.substring(1, str.length() - 1);
            if(str.contains("\\")) {
                str = str.replace("\\n", "\n")
                        .replace("\\t", "\t")
                        .replace("\\b", "\b")
                        .replace("\\r", "\r")
                        .replace("\\'", "'")
                        .replace("\\\\", "\\")
                        .replace("\\\"", "\"");
            }
            return new Ast.Expr.Literal(str);
        }
        else if (match(Token.Type.IDENTIFIER)) { // IDENTIFIER FOUND
            String name = tokens.get(-1).getLiteral();
            if (!match("(")) { // no expression after identifier
                return new Ast.Expr.Access(Optional.empty(), name);
            }
            else { // expression after identifier
                if (!match(")")) { // expression arguments found
                    Ast.Expr initalExpr = parseExpression();
                    List<Ast.Expr> args = new ArrayList<>();
                    args.add(initalExpr);

                    while (match(",")) {
                        args.add(parseExpression());
                    }

                    if (match(")")) { // Check closing parentheses
                        return new Ast.Expr.Function(Optional.empty(), name, args);
                    } else {
                        throw new ParseException("Closing parentheses expected", tokens.get(-1).getIndex());
                    }
                } else {
                    if (!tokens.get(-1).getLiteral().equals(")")) {
                        throw new ParseException("Closing parentheses expected", tokens.get(-1).getIndex());
                    } else {
                        return new Ast.Expr.Function(Optional.empty(), name, Collections.emptyList());
                    }
                }


            }

        } else if (match("(")) {
            Ast.Expr expr = parseExpression();
            if (!match(")")) {
                throw new ParseException("Expected closing parenthesis", tokens.get(-1).getIndex());
            }
            return new Ast.Stmt.Expr.Group(expr);
        } else {
            throw new ParseException("Invalid Primary Expression", tokens.get(-1).getIndex());
            // TODO: handle storing the actual character index instead of I
        }
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
           if (!tokens.has(i)) {
               return false;
           } else if (patterns[i] instanceof Token.Type) {
               if (patterns[i] != tokens.get(i).getType()) {
                   return false;
               }
           } else if (patterns[i] instanceof String) {
               if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                   return false;
               }
           } else {
               throw new AssertionError("Invalid pattern object: "
                                            + patterns[i].getClass());
           }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++)
                tokens.advance();
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
