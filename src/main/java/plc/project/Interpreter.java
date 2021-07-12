package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        for(Ast.Field field : ast.getFields()) {
            visit(field);
        }
        for(Ast.Method method : ast.getMethods()) {
            visit(method);
        }

        return scope.lookupFunction("main", 0).invoke(Collections.emptyList());
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        Environment.PlcObject value = Environment.NIL;
        if(ast.getValue().isPresent()) {
            value = visit(ast.getValue().get());
        }
        scope.defineVariable(ast.getName(), value);
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) { // defines function in current scope
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            try {
                scope = new Scope(scope);
                for(int i = 0; i < args.size(); i++) { // define arguments
                    scope.defineVariable(ast.getParameters().get(i), args.get(i));
                }
                for(Ast.Stmt stmt : ast.getStatements()) { // evaluate statements
                    visit(stmt);
                }
            }
            catch(Return r) {
                return r.value;
            }
            finally { // restore scope
                scope = scope.getParent();
            }
            return Environment.NIL;
        });
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Declaration ast) {
        Environment.PlcObject value = Environment.NIL;
        if(ast.getValue().isPresent()) {
            value = visit(ast.getValue().get());
        }
        scope.defineVariable(ast.getName(), value);
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Assignment ast) {
        Ast.Expr access = ast.getReceiver();
        if(access instanceof Ast.Expr.Access) {

            if(((Ast.Expr.Access) access).getReceiver().isPresent()) {
                visit(((Ast.Expr.Access) access).getReceiver().get()) // evaluates the receiver
                        .setField(((Ast.Expr.Access) access).getName(), visit(ast.getValue())); // sets the access as a field for the receiver
            } else {
                Environment.Variable variable = scope.lookupVariable(((Ast.Expr.Access) access).getName());
                variable.setValue(visit(ast.getValue()));
            }
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.If ast) {
        if(requireType(Boolean.class, visit(ast.getCondition())) != null) {
            try {
                scope = new Scope(scope);
                if((Boolean) visit(ast.getCondition()).getValue()) {
                    ast.getThenStatements().forEach(this::visit);
                } else {
                    ast.getElseStatements().forEach(this::visit);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {
        Iterable<Environment.PlcObject> value = requireType(Iterable.class, visit(ast.getValue()));
        if(value != null) {
            value.forEach(plcObject -> {
                try {
                    scope = new Scope(scope);
                    scope.defineVariable(ast.getName(), plcObject);

                    for(Ast.Stmt stmt : ast.getStatements()) {
                        visit(stmt);
                    }
                } finally {
                    scope = scope.getParent();
                }
            });
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.While ast) {
        while(requireType(Boolean.class, visit(ast.getCondition()))) {
            try { // enter new scope
                scope = new Scope(scope);
                ast.getStatements().forEach(this::visit);
            } finally { // restore scope
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Return ast) {

        Environment.PlcObject value = visit(ast.getValue());
        throw new Return(value);

//        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Literal ast) {
        if(ast.getLiteral() == null) {
            return Environment.NIL;
        }
        return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary ast) {
        Environment.PlcObject left = visit(ast.getLeft());

        switch (ast.getOperator()) {
            case "+":
                if(left.getValue() instanceof BigInteger) { // integer addition
                    if(visit(ast.getRight()).getValue() instanceof BigInteger) {
                        return Environment.create(
                                requireType(BigInteger.class, left).add(requireType(BigInteger.class, visit(ast.getRight())))
                        );
                    }
                    throw new RuntimeException();
                }
                if(left.getValue() instanceof BigDecimal) { // decimal addition
                    if(visit(ast.getRight()).getValue() instanceof BigDecimal) {
                        return Environment.create(
                                requireType(BigDecimal.class, left).add(requireType(BigDecimal.class, visit(ast.getRight())))
                        );
                    }
                    throw new RuntimeException();
                }
                if(left.getValue() instanceof String) {
                    if(visit(ast.getRight()).getValue() instanceof String) { // string concatenation
                        return Environment.create(
                                requireType(String.class, left) + requireType(String.class, visit(ast.getRight()))
                        );
                    }
                    throw new RuntimeException();
                }
                break;

            case "-":
                if(left.getValue() instanceof BigInteger) {
                    if(visit(ast.getRight()).getValue() instanceof BigInteger) { // integer subtraction
                        return Environment.create(
                                requireType(BigInteger.class, left).subtract(requireType(BigInteger.class, visit(ast.getRight())))
                        );
                    }
                    throw new RuntimeException();
                }
                if(left.getValue() instanceof BigDecimal) {
                    if(visit(ast.getRight()).getValue() instanceof BigDecimal) {
                        return Environment.create(
                                requireType(BigDecimal.class, left).subtract(requireType(BigDecimal.class, visit(ast.getRight())))
                        );
                    }
                    throw new RuntimeException();
                }
                break;

            case "*":
                if(left.getValue() instanceof BigInteger) { // integer multiplication
                    if(visit(ast.getRight()).getValue() instanceof BigInteger) {
                        return Environment.create(
                                requireType(BigInteger.class, left).multiply(requireType(BigInteger.class, visit(ast.getRight())))
                        );
                    }
                    throw new RuntimeException();
                }
                if(left.getValue() instanceof BigDecimal) { // decimal multiplication
                    if(visit(ast.getRight()).getValue() instanceof BigDecimal) {
                        return Environment.create(
                                requireType(BigDecimal.class, left).multiply(requireType(BigDecimal.class, visit(ast.getRight())))
                        );
                    }
                    throw new RuntimeException();
                }
                break;

            case "/":
                if(left.getValue() instanceof BigInteger) { // integer division
                    if(visit(ast.getRight()).getValue() instanceof BigInteger) {
                        if(((BigInteger) visit(ast.getRight()).getValue()).intValue() == 0) {
                            throw new RuntimeException();
                        }
                        return Environment.create(
                                requireType(BigInteger.class, left).divide(requireType(BigInteger.class, visit(ast.getRight())))
                        );
                    }
                    throw new RuntimeException();
                }
                if(left.getValue() instanceof BigDecimal) { // decimal division
                    if(visit(ast.getRight()).getValue() instanceof BigDecimal) {
                        if(((BigDecimal) visit(ast.getRight()).getValue()).doubleValue() == 0) { // divide by 0 error
                            throw new RuntimeException();
                        }
                        return Environment.create(
                                requireType(BigDecimal.class, left).divide(requireType(BigDecimal.class, visit(ast.getRight())), RoundingMode.HALF_EVEN)
                        );
                    }
                    throw new RuntimeException();
                }
                break;

            case "AND":
                if(left.getValue() instanceof Boolean && !(Boolean)left.getValue()) {
                    return Environment.create(false);
                }
                if(visit(ast.getRight()).getValue() instanceof Boolean && !(Boolean)visit(ast.getRight()).getValue()) {
                    return Environment.create(false);
                }
                if(left.getValue() instanceof Boolean) {
                    if(visit(ast.getRight()).getValue() instanceof Boolean) {
                        return Environment.create(true);
                    }
                    throw new RuntimeException();
                }
                break;

            case "OR":
                if(left.getValue() instanceof Boolean && (Boolean)left.getValue()) {
                    return Environment.create(true);
                }
                if(visit(ast.getRight()).getValue() instanceof Boolean && (Boolean)visit(ast.getRight()).getValue()) {
                    return Environment.create(true);
                }
                if(left.getValue() instanceof Boolean) {
                    if(visit(ast.getRight()).getValue() instanceof Boolean) {
                        return Environment.create(false);
                    }
                    throw new RuntimeException();
                }
                break;

            case "==":
                return Environment.create(
                        Objects.equals(left.getValue(), visit(ast.getRight()).getValue())
                );

            case "!=":
                return Environment.create(
                        !Objects.equals(left.getValue(), visit(ast.getRight()).getValue())
                );

            case "<":
                if(left.getValue() instanceof Comparable) {
                    Environment.PlcObject right = visit(ast.getRight());
                    if(requireType(left.getValue().getClass(), right) != null) {
                        return Environment.create(((Comparable) left.getValue()).compareTo(right.getValue()) < 0);
                    }
                }
                break;

            case "<=":
                if(left.getValue() instanceof Comparable) {
                    Environment.PlcObject right = visit(ast.getRight());
                    if(requireType(left.getValue().getClass(), right) != null) {
                        return Environment.create(((Comparable) left.getValue()).compareTo(right.getValue()) <= 0);
                    }
                }
                break;

            case ">":
                if(left.getValue() instanceof Comparable) {
                    Environment.PlcObject right = visit(ast.getRight());
                    if(requireType(left.getValue().getClass(), right) != null) {
                        return Environment.create(((Comparable) left.getValue()).compareTo(right.getValue()) > 0);
                    }
                }
                break;

            case ">=":
                if(left.getValue() instanceof Comparable) {
                    Environment.PlcObject right = visit(ast.getRight());
                    if(requireType(left.getValue().getClass(), right) != null) {
                        return Environment.create(((Comparable) left.getValue()).compareTo(right.getValue()) >= 0);
                    }
                }
                break;
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {

        if(ast.getReceiver().isPresent()) {
            return visit(ast.getReceiver().get()).getField(ast.getName()).getValue();
        }
        return scope.lookupVariable(ast.getName()).getValue();
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Function ast) {
        // Convert args
        List<Environment.PlcObject> arguments = new ArrayList<>();
        for (Ast.Expr argument : ast.getArguments()) {
            arguments.add(visit(argument));
        }

        if (!ast.getReceiver().isPresent()) {
            // Is a function
            Environment.Function function = scope.lookupFunction(ast.getName(), ast.getArguments().size());
            return function.invoke(arguments);
        } else {
            // Is a Method
            Environment.PlcObject obj = visit(ast.getReceiver().get());
            return obj.callMethod(ast.getName(), arguments);
        }

    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
