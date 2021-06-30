package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

final class InterpreterTests {

    @ParameterizedTest
    @MethodSource
    void testSource(String test, Ast.Source ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
                Arguments.of("Main", new Ast.Source(
                        Arrays.asList(),
                        Arrays.asList(new Ast.Method("main", Arrays.asList(), Arrays.asList(
                                new Ast.Stmt.Return(new Ast.Expr.Literal(BigInteger.ZERO)))
                        ))
                ), BigInteger.ZERO),
                Arguments.of("Fields & No Return", new Ast.Source(
                        Arrays.asList(
                                new Ast.Field("x", Optional.of(new Ast.Expr.Literal(BigInteger.ONE))),
                                new Ast.Field("y", Optional.of(new Ast.Expr.Literal(BigInteger.TEN)))
                        ),
                        Arrays.asList(new Ast.Method("main", Arrays.asList(), Arrays.asList(
                                new Ast.Stmt.Expression(new Ast.Expr.Binary("+",
                                        new Ast.Expr.Access(Optional.empty(), "x"),
                                        new Ast.Expr.Access(Optional.empty(), "y")                                ))
                        )))
                ), Environment.NIL.getValue())
        );
    }

    @ParameterizedTest
    @MethodSource
    void testField(String test, Ast.Field ast, Object expected) {
        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupVariable(ast.getName()).getValue().getValue());
    }

    private static Stream<Arguments> testField() {
        return Stream.of(
                Arguments.of("Declaration", new Ast.Field("name", Optional.empty()), Environment.NIL.getValue()),
                Arguments.of("Initialization", new Ast.Field("name", Optional.of(new Ast.Expr.Literal(BigInteger.ONE))), BigInteger.ONE)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testMethod(String test, Ast.Method ast, List<Environment.PlcObject> args, Object expected) {
        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupFunction(ast.getName(), args.size()).invoke(args).getValue());
    }

    private static Stream<Arguments> testMethod() {
        return Stream.of(
                Arguments.of("Main",
                        new Ast.Method("main", Arrays.asList(), Arrays.asList(
                                new Ast.Stmt.Return(new Ast.Expr.Literal(BigInteger.ZERO)))
                        ),
                        Arrays.asList(),
                        BigInteger.ZERO
                ),
                Arguments.of("Arguments",
                        new Ast.Method("main", Arrays.asList("x"), Arrays.asList(
                                new Ast.Stmt.Return(new Ast.Expr.Binary("*",
                                        new Ast.Expr.Access(Optional.empty(), "x"),
                                        new Ast.Expr.Access(Optional.empty(), "x")
                                ))
                        )),
                        Arrays.asList(Environment.create(BigInteger.TEN)),
                        BigInteger.valueOf(100)
                )
        );
    }

    @Test
    void testExpressionStatement() {
        PrintStream sysout = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        try {
            test(new Ast.Stmt.Expression(
                    new Ast.Expr.Function(Optional.empty(), "print", Arrays.asList(new Ast.Expr.Literal("Hello, World!")))
            ), Environment.NIL.getValue(), new Scope(null));
            Assertions.assertEquals("Hello, World!" + System.lineSeparator(), out.toString());
        } finally {
            System.setOut(sysout);
        }
    }

    @ParameterizedTest
    @MethodSource
    void testDeclarationStatement(String test, Ast.Stmt.Declaration ast, Object expected) {
        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupVariable(ast.getName()).getValue().getValue());
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of("Declaration",
                        new Ast.Stmt.Declaration("name", Optional.empty()),
                        Environment.NIL.getValue()
                ),
                Arguments.of("Initialization",
                        new Ast.Stmt.Declaration("name", Optional.of(new Ast.Expr.Literal(BigInteger.ONE))),
                        BigInteger.ONE
                )
        );
    }

    @Test
    void testVariableAssignmentStatement() {
        Scope scope = new Scope(null);
        scope.defineVariable("variable", Environment.create("variable"));
        test(new Ast.Stmt.Assignment(
                new Ast.Expr.Access(Optional.empty(),"variable"),
                new Ast.Expr.Literal(BigInteger.ONE)
        ), Environment.NIL.getValue(), scope);
        Assertions.assertEquals(BigInteger.ONE, scope.lookupVariable("variable").getValue().getValue());
    }

    @Test
    void testFieldAssignmentStatement() {
        Scope scope = new Scope(null);
        Scope object = new Scope(null);
        object.defineVariable("field", Environment.create("object.field"));
        scope.defineVariable("object", new Environment.PlcObject(object, "object"));
        test(new Ast.Stmt.Assignment(
                new Ast.Expr.Access(Optional.of(new Ast.Expr.Access(Optional.empty(), "object")),"field"),
                new Ast.Expr.Literal(BigInteger.ONE)
        ), Environment.NIL.getValue(), scope);
        Assertions.assertEquals(BigInteger.ONE, object.lookupVariable("field").getValue().getValue());
    }

    @ParameterizedTest
    @MethodSource
    void testIfStatement(String test, Ast.Stmt.If ast, Object expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("num", Environment.NIL);
        test(ast, Environment.NIL.getValue(), scope);
        Assertions.assertEquals(expected, scope.lookupVariable("num").getValue().getValue());
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                Arguments.of("True Condition",
                        new Ast.Stmt.If(
                                new Ast.Expr.Literal(true),
                                Arrays.asList(new Ast.Stmt.Assignment(new Ast.Expr.Access(Optional.empty(),"num"), new Ast.Expr.Literal(BigInteger.ONE))),
                                Arrays.asList()
                        ),
                        BigInteger.ONE
                ),
                Arguments.of("False Condition",
                        new Ast.Stmt.If(
                                new Ast.Expr.Literal(false),
                                Arrays.asList(),
                                Arrays.asList(new Ast.Stmt.Assignment(new Ast.Expr.Access(Optional.empty(),"num"), new Ast.Expr.Literal(BigInteger.TEN)))
                        ),
                        BigInteger.TEN
                )
        );
    }

    @Test
    void testForStatement() {
        Scope scope = new Scope(null);
        scope.defineVariable("sum", Environment.create(BigInteger.ZERO));
        scope.defineVariable("list", Environment.create(IntStream.range(0, 5)
                .mapToObj(i -> Environment.create(BigInteger.valueOf(i)))
                .collect(Collectors.toList())));
        test(new Ast.Stmt.For("num",
                new Ast.Expr.Access(Optional.empty(), "list"),
                Arrays.asList(new Ast.Stmt.Assignment(
                        new Ast.Expr.Access(Optional.empty(),"sum"),
                        new Ast.Expr.Binary("+",
                                new Ast.Expr.Access(Optional.empty(),"sum"),
                                new Ast.Expr.Access(Optional.empty(),"num")
                        )
                ))
        ), Environment.NIL.getValue(), scope);
        Assertions.assertEquals(BigInteger.TEN, scope.lookupVariable("sum").getValue().getValue());
    }

    @Test
    void testWhileStatement() {
        Scope scope = new Scope(null);
        scope.defineVariable("num", Environment.create(BigInteger.ZERO));
        test(new Ast.Stmt.While(
                new Ast.Expr.Binary("<",
                        new Ast.Expr.Access(Optional.empty(),"num"),
                        new Ast.Expr.Literal(BigInteger.TEN)
                ),
                Arrays.asList(new Ast.Stmt.Assignment(
                        new Ast.Expr.Access(Optional.empty(),"num"),
                        new Ast.Expr.Binary("+",
                                new Ast.Expr.Access(Optional.empty(),"num"),
                                new Ast.Expr.Literal(BigInteger.ONE)
                        )
                ))
        ),Environment.NIL.getValue(), scope);
        Assertions.assertEquals(BigInteger.TEN, scope.lookupVariable("num").getValue().getValue());
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpression(String test, Ast ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                Arguments.of("Nil", new Ast.Expr.Literal(null), Environment.NIL.getValue()), //remember, special case
                Arguments.of("Boolean", new Ast.Expr.Literal(true), true),
                Arguments.of("Integer", new Ast.Expr.Literal(BigInteger.ONE), BigInteger.ONE),
                Arguments.of("Decimal", new Ast.Expr.Literal(BigDecimal.ONE), BigDecimal.ONE),
                Arguments.of("Character", new Ast.Expr.Literal('c'), 'c'),
                Arguments.of("String", new Ast.Expr.Literal("string"), "string")
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpression(String test, Ast ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                Arguments.of("Literal", new Ast.Expr.Group(new Ast.Expr.Literal(BigInteger.ONE)), BigInteger.ONE),
                Arguments.of("Binary",
                        new Ast.Expr.Group(new Ast.Expr.Binary("+",
                                new Ast.Expr.Literal(BigInteger.ONE),
                                new Ast.Expr.Literal(BigInteger.TEN)
                        )),
                        BigInteger.valueOf(11)
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpression(String test, Ast ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("And",
                        new Ast.Expr.Binary("AND",
                                new Ast.Expr.Literal(true),
                                new Ast.Expr.Literal(false)
                        ),
                        false
                ),
                Arguments.of("Or (Short Circuit)",
                        new Ast.Expr.Binary("OR",
                                new Ast.Expr.Literal(true),
                                new Ast.Expr.Access(Optional.empty(), "undefined")
                        ),
                        true
                ),
                Arguments.of("Less Than",
                        new Ast.Expr.Binary("<",
                                new Ast.Expr.Literal(BigInteger.ONE),
                                new Ast.Expr.Literal(BigInteger.TEN)
                        ),
                        true
                ),
                Arguments.of("Greater Than or Equal",
                        new Ast.Expr.Binary(">=",
                                new Ast.Expr.Literal(BigInteger.ONE),
                                new Ast.Expr.Literal(BigInteger.TEN)
                        ),
                        false
                ),
                Arguments.of("Equal",
                        new Ast.Expr.Binary("==",
                                new Ast.Expr.Literal(BigInteger.ONE),
                                new Ast.Expr.Literal(BigInteger.TEN)
                        ),
                        false
                ),
                Arguments.of("Concatenation",
                        new Ast.Expr.Binary("+",
                                new Ast.Expr.Literal("a"),
                                new Ast.Expr.Literal("b")
                        ),
                        "ab"
                ),
                Arguments.of("Addition",
                        new Ast.Expr.Binary("+",
                                new Ast.Expr.Literal(BigInteger.ONE),
                                new Ast.Expr.Literal(BigInteger.TEN)
                        ),
                        BigInteger.valueOf(11)
                ),
                Arguments.of("Division",
                        new Ast.Expr.Binary("/",
                                new Ast.Expr.Literal(new BigDecimal("1.2")),
                                new Ast.Expr.Literal(new BigDecimal("3.4"))
                        ),
                        new BigDecimal("0.4")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpression(String test, Ast ast, Object expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("variable", Environment.create("variable"));
        Scope object = new Scope(null);
        object.defineVariable("field", Environment.create("object.field"));
        scope.defineVariable("object", new Environment.PlcObject(object, "object"));
        test(ast, expected, scope);
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
                Arguments.of("Variable",
                        new Ast.Expr.Access(Optional.empty(), "variable"),
                        "variable"
                ),
                Arguments.of("Field",
                        new Ast.Expr.Access(Optional.of(new Ast.Expr.Access(Optional.empty(), "object")), "field"),
                        "object.field"
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpression(String test, Ast ast, Object expected) {
        Scope scope = new Scope(null);
        scope.defineFunction("function", 0, args -> Environment.create("function"));
        Scope object = new Scope(null);
        object.defineFunction("method", 1, args -> Environment.create("object.method"));
        scope.defineVariable("object", new Environment.PlcObject(object, "object"));
        test(ast, expected, scope);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Function",
                        new Ast.Expr.Function(Optional.empty(), "function", Arrays.asList()),
                        "function"
                ),
                Arguments.of("Method",
                        new Ast.Expr.Function(Optional.of(new Ast.Expr.Access(Optional.empty(), "object")), "method", Arrays.asList()),
                        "object.method"
                ),
                Arguments.of("Print",
                        new Ast.Expr.Function(Optional.empty(), "print", Arrays.asList(new Ast.Expr.Literal("Hello, World!"))),
                        Environment.NIL.getValue()
                )
        );
    }

    private static Scope test(Ast ast, Object expected, Scope scope) {
        Interpreter interpreter = new Interpreter(scope);
        if (expected != null) {
            Assertions.assertEquals(expected, interpreter.visit(ast).getValue());
        } else {
            Assertions.assertThrows(RuntimeException.class, () -> interpreter.visit(ast));
        }
        return interpreter.getScope();
    }

}
