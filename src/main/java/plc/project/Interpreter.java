package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
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
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Declaration ast) {
        throw new UnsupportedOperationException(); //TODO (in lecture)
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Assignment ast) {
        throw new UnsupportedOperationException(); //TODO
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
//        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {
        if(requireType(Iterable.class, visit(ast.getValue())) != null) {
            ((Iterable<Environment.PlcObject>) ast.getValue()).forEach(plcObject -> {
                try {
                    scope = new Scope(scope);
                    scope.defineVariable(ast.getName(), visit(ast.getValue()));
                } finally {
                    scope = scope.getParent();
                }
            });
        }
        return Environment.NIL;
//        throw new UnsupportedOperationException(); //TODO
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
//        throw new UnsupportedOperationException(); //TODO (in lecture)
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Return ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Literal ast) {
        if(ast.getLiteral() == null) {
            return Environment.NIL;
        }
        return Environment.create(ast.getLiteral());
//        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Group ast) {
        return visit(ast.getExpression());
//        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary ast) {
        Environment.PlcObject left = visit(ast.getLeft());
//        Environment.PlcObject visit(ast.getRight()) = visit(ast.getRight());
        switch (ast.getOperator()) {
            case "+":
                if(left.getValue() instanceof BigInteger && visit(ast.getRight()).getValue() instanceof BigInteger) {
                    return Environment.create(
                            requireType(BigInteger.class, left).add(requireType(BigInteger.class, visit(ast.getRight())))
                    );
                }
                if(left.getValue() instanceof BigDecimal && visit(ast.getRight()).getValue() instanceof BigDecimal) {
                    return Environment.create(
                            requireType(BigDecimal.class, left).add(requireType(BigDecimal.class, visit(ast.getRight())))
                    );
                }
                if(left.getValue() instanceof String && visit(ast.getRight()).getValue() instanceof String) {
                    return Environment.create(
                            requireType(String.class, left) + requireType(String.class, visit(ast.getRight()))
                    );
                }
                break;

            case "-":
                if(left.getValue() instanceof BigInteger && visit(ast.getRight()).getValue() instanceof BigInteger) {
                    return Environment.create(
                            requireType(BigInteger.class, left).subtract(requireType(BigInteger.class, visit(ast.getRight())))
                    );
                }
                if(left.getValue() instanceof BigDecimal && visit(ast.getRight()).getValue() instanceof BigDecimal) {
                    return Environment.create(
                            requireType(BigDecimal.class, left).subtract(requireType(BigDecimal.class, visit(ast.getRight())))
                    );
                }
                break;

            case "*":
                if(left.getValue() instanceof BigInteger && visit(ast.getRight()).getValue() instanceof BigInteger) {
                    return Environment.create(
                            requireType(BigInteger.class, left).multiply(requireType(BigInteger.class, visit(ast.getRight())))
                    );
                }
                if(left.getValue() instanceof BigDecimal && visit(ast.getRight()).getValue() instanceof BigDecimal) {
                    return Environment.create(
                            requireType(BigDecimal.class, left).multiply(requireType(BigDecimal.class, visit(ast.getRight())))
                    );
                }
                break;

            case "/":
                if(left.getValue() instanceof BigInteger && visit(ast.getRight()).getValue() instanceof BigInteger) {
                    return Environment.create(
                            requireType(BigInteger.class, left).divide(requireType(BigInteger.class, visit(ast.getRight())))
                    );
                }
                if(left.getValue() instanceof BigDecimal && visit(ast.getRight()).getValue() instanceof BigDecimal) {
                    return Environment.create(
                            requireType(BigDecimal.class, left).divide(requireType(BigDecimal.class, visit(ast.getRight())), 2)
                    );
                }
                break;

            case "AND":
                if(left.getValue() instanceof Boolean && !(Boolean)left.getValue()) {
                    return Environment.create(false);
                }
                if(visit(ast.getRight()).getValue() instanceof Boolean && !(Boolean)visit(ast.getRight()).getValue()) {
                    return Environment.create(false);
                }
                if(left.getValue() instanceof Boolean && visit(ast.getRight()).getValue() instanceof Boolean) {
                    return Environment.create(true);
                }
                break;

            case "OR":
                if(left.getValue() instanceof Boolean && (Boolean)left.getValue()) {
                    return Environment.create(true);
                }
                if(visit(ast.getRight()).getValue() instanceof Boolean && (Boolean)visit(ast.getRight()).getValue()) {
                    return Environment.create(true);
                }
                if(left.getValue() instanceof Boolean && visit(ast.getRight()).getValue() instanceof Boolean) {
                    return Environment.create(false);
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
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Function ast) {
        throw new UnsupportedOperationException(); //TODO
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
