package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Standard JUnit5 parameterized tests. See the RegexTests file from Homework 1
 * or the LexerTests file from the last project part for more information.
 */
final class ParserTests {

    @ParameterizedTest
    @MethodSource
    void testSource(String test, List<Token> tokens, Ast.Source expected) {
        test(tokens, expected, Parser::parseSource);
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
                Arguments.of("Zero Statements",
                        Arrays.asList(),
                        new Ast.Source(Arrays.asList(), Arrays.asList())
                ),
                Arguments.of("Field",
                        Arrays.asList(
                                //LET name: Type = expr;
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, ":", 9),
                                new Token(Token.Type.IDENTIFIER, "Type", 11),
                                new Token(Token.Type.OPERATOR, "=", 15),
                                new Token(Token.Type.IDENTIFIER, "expr", 17),
                                new Token(Token.Type.OPERATOR, ";", 21)
                        ),
                        new Ast.Source(
                                Arrays.asList(new Ast.Field("name", "Type", Optional.of(new Ast.Expr.Access(Optional.empty(), "expr")))),
                                Arrays.asList()
                        )
                ),
                Arguments.of("Method",
                        Arrays.asList(
                                //DEF name(): Type DO stmt; END
                                new Token(Token.Type.IDENTIFIER, "DEF", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "(", 8),
                                new Token(Token.Type.OPERATOR, ")", 9),
                                new Token(Token.Type.OPERATOR, ":", 10),
                                new Token(Token.Type.IDENTIFIER, "Type", 12),
                                new Token(Token.Type.IDENTIFIER, "DO", 17),
                                new Token(Token.Type.IDENTIFIER, "stmt", 20),
                                new Token(Token.Type.OPERATOR, ";", 24),
                                new Token(Token.Type.IDENTIFIER, "END", 26)
                        ),
                        new Ast.Source(
                                Arrays.asList(),
                                Arrays.asList(new Ast.Method("name", Arrays.asList(), Arrays.asList(), Optional.of("Type"), Arrays.asList(
                                        new Ast.Stmt.Expression(new Ast.Expr.Access(Optional.empty(), "stmt"))
                                )))
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExpressionStatement(String test, List<Token> tokens, Ast.Stmt.Expression expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testExpressionStatement() {
        return Stream.of(
                Arguments.of("Function Expression",
                        Arrays.asList(
                                //name();
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5),
                                new Token(Token.Type.OPERATOR, ";", 6)
                        ),
                        new Ast.Stmt.Expression(new Ast.Expr.Function(Optional.empty(), "name", Arrays.asList()))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDeclarationStatement(String test, List<Token> tokens, Ast.Stmt.Declaration expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of("Definition",
                        Arrays.asList(
                                //LET name: Type;
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, ":", 8),
                                new Token(Token.Type.IDENTIFIER, "Type", 10),
                                new Token(Token.Type.OPERATOR, ";", 14)
                        ),
                        new Ast.Stmt.Declaration("name", Optional.of("Type"), Optional.empty())
                ),
                Arguments.of("Initialization",
                        Arrays.asList(
                                //LET name = expr;
                                new Token(Token.Type.IDENTIFIER, "LET", 0),
                                new Token(Token.Type.IDENTIFIER, "name", 4),
                                new Token(Token.Type.OPERATOR, "=", 9),
                                new Token(Token.Type.IDENTIFIER, "expr", 11),
                                new Token(Token.Type.OPERATOR, ";", 15)
                        ),
                        new Ast.Stmt.Declaration("name", Optional.empty(), Optional.of(new Ast.Expr.Access(Optional.empty(), "expr")))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAssignmentStatement(String test, List<Token> tokens, Ast.Stmt.Assignment expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testAssignmentStatement() {
        return Stream.of(
                Arguments.of("Assignment",
                        Arrays.asList(
                                //name = value;
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.IDENTIFIER, "value", 7),
                                new Token(Token.Type.OPERATOR, ";", 12)
                        ),
                        new Ast.Stmt.Assignment(
                                new Ast.Expr.Access(Optional.empty(), "name"),
                                new Ast.Expr.Access(Optional.empty(), "value")
                        )
                )
        );
    }


    @ParameterizedTest
    @MethodSource
    void testIfStatement(String test, List<Token> tokens, Ast.Stmt.If expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                Arguments.of("If",
                        Arrays.asList(
                                //IF expr DO stmt; END
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "DO", 8),
                                new Token(Token.Type.IDENTIFIER, "stmt", 11),
                                new Token(Token.Type.OPERATOR, ";", 15),
                                new Token(Token.Type.IDENTIFIER, "END", 17)
                        ),
                        new Ast.Stmt.If(
                                new Ast.Expr.Access(Optional.empty(), "expr"),
                                Arrays.asList(new Ast.Stmt.Expression(new Ast.Expr.Access(Optional.empty(), "stmt"))),
                                Arrays.asList()
                        )
                ),
                Arguments.of("Else",
                        Arrays.asList(
                                //IF expr DO stmt1; ELSE stmt2; END
                                new Token(Token.Type.IDENTIFIER, "IF", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 3),
                                new Token(Token.Type.IDENTIFIER, "DO", 8),
                                new Token(Token.Type.IDENTIFIER, "stmt1", 11),
                                new Token(Token.Type.OPERATOR, ";", 16),
                                new Token(Token.Type.IDENTIFIER, "ELSE", 18),
                                new Token(Token.Type.IDENTIFIER, "stmt2", 23),
                                new Token(Token.Type.OPERATOR, ";", 28),
                                new Token(Token.Type.IDENTIFIER, "END", 30)
                        ),
                        new Ast.Stmt.If(
                                new Ast.Expr.Access(Optional.empty(), "expr"),
                                Arrays.asList(new Ast.Stmt.Expression(new Ast.Expr.Access(Optional.empty(), "stmt1"))),
                                Arrays.asList(new Ast.Stmt.Expression(new Ast.Expr.Access(Optional.empty(), "stmt2")))
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testForStatement(String test, List<Token> tokens, Ast.Stmt.For expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testForStatement() {
        return Stream.of(
                Arguments.of("For",
                        Arrays.asList(
                                //FOR elem IN list DO stmt; END
                                new Token(Token.Type.IDENTIFIER, "FOR", 0),
                                new Token(Token.Type.IDENTIFIER, "elem", 6),
                                new Token(Token.Type.IDENTIFIER, "IN", 9),
                                new Token(Token.Type.IDENTIFIER, "list", 12),
                                new Token(Token.Type.IDENTIFIER, "DO", 17),
                                new Token(Token.Type.IDENTIFIER, "stmt", 20),
                                new Token(Token.Type.OPERATOR, ";", 24),
                                new Token(Token.Type.IDENTIFIER, "END", 26)
                        ),
                        new Ast.Stmt.For(
                                "elem",
                                new Ast.Expr.Access(Optional.empty(), "list"),
                                Arrays.asList(new Ast.Stmt.Expression(new Ast.Expr.Access(Optional.empty(), "stmt")))
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testWhileStatement(String test, List<Token> tokens, Ast.Stmt.While expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testWhileStatement() {
        return Stream.of(
                Arguments.of("While",
                        Arrays.asList(
                                //WHILE expr DO stmt; END
                                new Token(Token.Type.IDENTIFIER, "WHILE", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 6),
                                new Token(Token.Type.IDENTIFIER, "DO", 11),
                                new Token(Token.Type.IDENTIFIER, "stmt", 14),
                                new Token(Token.Type.OPERATOR, ";", 18),
                                new Token(Token.Type.IDENTIFIER, "END", 20)
                        ),
                        new Ast.Stmt.While(
                                new Ast.Expr.Access(Optional.empty(), "expr"),
                                Arrays.asList(new Ast.Stmt.Expression(new Ast.Expr.Access(Optional.empty(), "stmt")))
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testReturnStatement(String test, List<Token> tokens, Ast.Stmt.Return expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testReturnStatement() {
        return Stream.of(
                Arguments.of("Return Statement",
                        Arrays.asList(
                                //RETURN expr;
                                new Token(Token.Type.IDENTIFIER, "RETURN", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 7),
                                new Token(Token.Type.OPERATOR, ";", 11)
                        ),
                        new Ast.Stmt.Return(new Ast.Expr.Access(Optional.empty(), "expr"))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpression(String test, List<Token> tokens, Ast.Expr.Literal expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                Arguments.of("Boolean Literal",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "TRUE", 0)),
                        new Ast.Expr.Literal(Boolean.TRUE)
                ),
                Arguments.of("Integer Literal",
                        Arrays.asList(new Token(Token.Type.INTEGER, "1", 0)),
                        new Ast.Expr.Literal(new BigInteger("1"))
                ),
                Arguments.of("Decimal Literal",
                        Arrays.asList(new Token(Token.Type.DECIMAL, "2.0", 0)),
                        new Ast.Expr.Literal(new BigDecimal("2.0"))
                ),
                Arguments.of("Character Literal",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'c'", 0)),
                        new Ast.Expr.Literal('c')
                ),
                Arguments.of("String Literal",
                        Arrays.asList(new Token(Token.Type.STRING, "\"string\"", 0)),
                        new Ast.Expr.Literal("string")
                ),
                Arguments.of("Escape Character",
                        Arrays.asList(new Token(Token.Type.STRING, "\"Hello,\\nWorld!\"", 0)),
                        new Ast.Expr.Literal("Hello,\nWorld!")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpression(String test, List<Token> tokens, Ast.Expr.Group expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                Arguments.of("Grouped Variable",
                        Arrays.asList(
                                //(expr)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expr.Group(new Ast.Expr.Access(Optional.empty(), "expr"))
                ),
                Arguments.of("Grouped Binary",
                        Arrays.asList(
                                //(expr1 + expr2)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr1", 1),
                                new Token(Token.Type.OPERATOR, "+", 7),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9),
                                new Token(Token.Type.OPERATOR, ")", 14)
                        ),
                        new Ast.Expr.Group(new Ast.Expr.Binary("+",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        ))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpression(String test, List<Token> tokens, Ast.Expr.Binary expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("Binary And",
                        Arrays.asList(
                                //expr1 AND expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.IDENTIFIER, "AND", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 10)
                        ),
                        new Ast.Expr.Binary("AND",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Equality",
                        Arrays.asList(
                                //expr1 == expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9)
                        ),
                        new Ast.Expr.Binary("==",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Addition",
                        Arrays.asList(
                                //expr1 + expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "+", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expr.Binary("+",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Multiplication",
                        Arrays.asList(
                                //expr1 * expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "*", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expr.Binary("*",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpression(String test, List<Token> tokens, Ast.Expr.Access expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
                Arguments.of("Variable",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "name", 0)),
                        new Ast.Expr.Access(Optional.empty(), "name")
                ),
                Arguments.of("Field Access",
                        Arrays.asList(
                                //obj.field
                                new Token(Token.Type.IDENTIFIER, "obj", 0),
                                new Token(Token.Type.OPERATOR, ".", 3),
                                new Token(Token.Type.IDENTIFIER, "field", 4)
                        ),
                        new Ast.Expr.Access(Optional.of(new Ast.Expr.Access(Optional.empty(), "obj")), "field")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpression(String test, List<Token> tokens, Ast.Expr.Function expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Zero Arguments",
                        Arrays.asList(
                                //name()
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expr.Function(Optional.empty(), "name", Arrays.asList())
                ),
                Arguments.of("Multiple Arguments",
                        Arrays.asList(
                                //name(expr1, expr2, expr3)
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "expr1", 5),
                                new Token(Token.Type.OPERATOR, ",", 10),
                                new Token(Token.Type.IDENTIFIER, "expr2", 12),
                                new Token(Token.Type.OPERATOR, ",", 17),
                                new Token(Token.Type.IDENTIFIER, "expr3", 19),
                                new Token(Token.Type.OPERATOR, ")", 24)
                        ),
                        new Ast.Expr.Function(Optional.empty(), "name", Arrays.asList(
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2"),
                                new Ast.Expr.Access(Optional.empty(), "expr3")
                        ))
                ),
                Arguments.of("Method Call",
                        Arrays.asList(
                                //obj.method()
                                new Token(Token.Type.IDENTIFIER, "obj", 0),
                                new Token(Token.Type.OPERATOR, ".", 3),
                                new Token(Token.Type.IDENTIFIER, "method", 4),
                                new Token(Token.Type.OPERATOR, "(", 10),
                                new Token(Token.Type.OPERATOR, ")", 11)
                        ),
                        new Ast.Expr.Function(Optional.of(new Ast.Expr.Access(Optional.empty(), "obj")), "method", Arrays.asList())
                )
        );
    }

    @Test
    void testExample1() {
        List<Token> input = Arrays.asList(
                /* LET first: Integer = 1;
                 * DEF main(): Integer DO
                 *     WHILE first != 10 DO
                 *         print(first);
                 *         first = first + 1;
                 *     END
                 * END
                 */
                //LET first: Integer = 1;
                new Token(Token.Type.IDENTIFIER, "LET", 0),
                new Token(Token.Type.IDENTIFIER, "first", 4),
                new Token(Token.Type.OPERATOR, ":", 10),
                new Token(Token.Type.IDENTIFIER, "Integer", 11),
                new Token(Token.Type.OPERATOR, "=", 19),
                new Token(Token.Type.INTEGER, "1", 21),
                new Token(Token.Type.OPERATOR, ";", 22),
                //DEF main(): Integer DO
                new Token(Token.Type.IDENTIFIER, "DEF", 24),
                new Token(Token.Type.IDENTIFIER, "main", 28),
                new Token(Token.Type.OPERATOR, "(", 32),
                new Token(Token.Type.OPERATOR, ")", 33),
                new Token(Token.Type.OPERATOR, ":", 34),
                new Token(Token.Type.IDENTIFIER, "Integer", 36),
                new Token(Token.Type.IDENTIFIER, "DO", 44),
                //    WHILE first != 10 DO
                new Token(Token.Type.IDENTIFIER, "WHILE", 51),
                new Token(Token.Type.IDENTIFIER, "first", 57),
                new Token(Token.Type.OPERATOR, "!=", 63),
                new Token(Token.Type.INTEGER, "10", 66),
                new Token(Token.Type.IDENTIFIER, "DO", 69),
                //        print(first);
                new Token(Token.Type.IDENTIFIER, "print", 80),
                new Token(Token.Type.OPERATOR, "(", 85),
                new Token(Token.Type.IDENTIFIER, "first", 86),
                new Token(Token.Type.OPERATOR, ")", 91),
                new Token(Token.Type.OPERATOR, ";", 92),
                //        first = first + 1;
                new Token(Token.Type.IDENTIFIER, "first", 102),
                new Token(Token.Type.OPERATOR, "=", 108),
                new Token(Token.Type.IDENTIFIER, "first", 110),
                new Token(Token.Type.OPERATOR, "+", 116),
                new Token(Token.Type.INTEGER, "1", 118),
                new Token(Token.Type.OPERATOR, ";", 119),
                //    END
                new Token(Token.Type.IDENTIFIER, "END", 125),
                //END
                new Token(Token.Type.IDENTIFIER, "END", 129)
        );
        Ast.Source expected = new Ast.Source(
                Arrays.asList(new Ast.Field("first", "Integer", Optional.of(new Ast.Expr.Literal(BigInteger.ONE)))),
                Arrays.asList(new Ast.Method("main", Arrays.asList(), Arrays.asList(), Optional.of("Integer"), Arrays.asList(
                        new Ast.Stmt.While(
                                new Ast.Expr.Binary("!=",
                                        new Ast.Expr.Access(Optional.empty(), "first"),
                                        new Ast.Expr.Literal(BigInteger.TEN)
                                ),
                                Arrays.asList(
                                        new Ast.Stmt.Expression(
                                                new Ast.Expr.Function(Optional.empty(), "print", Arrays.asList(
                                                        new Ast.Expr.Access(Optional.empty(), "first"))
                                                )
                                        ),
                                        new Ast.Stmt.Assignment(
                                                new Ast.Expr.Access(Optional.empty(), "first"),
                                                new Ast.Expr.Binary("+",
                                                        new Ast.Expr.Access(Optional.empty(), "first"),
                                                        new Ast.Expr.Literal(BigInteger.ONE)
                                                )
                                        )
                                )
                        )
                ))
        ));
        test(input, expected, Parser::parseSource);
    }

    /**
     * Standard test function. If expected is null, a ParseException is expected
     * to be thrown (not used in the provided tests).
     */
    private static <T extends Ast> void test(List<Token> tokens, T expected, Function<Parser, T> function) {
        Parser parser = new Parser(tokens);
        if (expected != null) {
            Assertions.assertEquals(expected, function.apply(parser));
        } else {
            Assertions.assertThrows(ParseException.class, () -> function.apply(parser));
        }
    }

}
