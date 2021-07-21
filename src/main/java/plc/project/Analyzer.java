package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Field ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Method ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        if (!(ast.getExpression() instanceof Ast.Expr.Function)) {
            throw new RuntimeException("Expected Ast.Expr.Function");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {

        if (!ast.getTypeName().isPresent() && !ast.getValue().isPresent()) {
            throw new RuntimeException("Expected type or value when declaring a variable.");
        }

        Environment.Type type =  null;

        if (ast.getTypeName().isPresent()) {
            type = Environment.getType(ast.getTypeName().get());
        }

        if (ast.getValue().isPresent()) {
            // has a value
            visit(ast.getValue().get());

            if (type == null) {
                type = ast.getValue().get().getType();
            }

            requireAssignable(type, ast.getValue().get().getType());
        }

        Environment.Variable var = scope.defineVariable(ast.getName(), ast.getName(), type, Environment.NIL);
        ast.setVariable(var);

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {

        Object literal = ast.getLiteral();

        if (literal == null)
            ast.setType(Environment.Type.NIL);

        if (literal instanceof Boolean)
            ast.setType(Environment.Type.BOOLEAN);

        if (literal instanceof Character)
            ast.setType(Environment.Type.CHARACTER);

        if (literal instanceof String)
            ast.setType(Environment.Type.STRING);

        if (literal instanceof BigInteger){
            // Check for size
            BigInteger value = (BigInteger) literal;
            if (value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0 &&
                    value.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0)
                ast.setType(Environment.Type.INTEGER);
            else
                throw new RuntimeException("Value of Integer is not in the range of and Integer.");

            return null;
        }


        if (literal instanceof BigDecimal){
            // Check for size
            BigDecimal value = (BigDecimal) literal;
            if (value.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) <= 0 &&
                    value.compareTo(BigDecimal.valueOf(Double.MIN_VALUE)) >= 0)
                ast.setType(Environment.Type.DECIMAL);
            else
                throw new RuntimeException("Value of Decimal is not in the range of and Double.");

            return null;
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        if (ast.getExpression() instanceof Ast.Expr.Binary) {
            visit(ast.getExpression());
            ast.setType(ast.getExpression().getType());
            return null;
        }
        throw new RuntimeException("Expected an Ast.Expr.Binary");
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        Ast.Expr left = ast.getLeft();
        visit(left);
        Ast.Expr right = ast.getRight();
        visit(right);

        switch (ast.getOperator()) {
            case "AND": case "OR":
                if (left.getType() == Environment.Type.BOOLEAN &&
                        right.getType() == Environment.Type.BOOLEAN) {
                    ast.setType(Environment.Type.BOOLEAN);
                    return null;
                }
                throw new RuntimeException("Boolean Type Expected");
            case "<": case "<=": case ">": case ">=": case "==": case "!=":
                requireAssignable(Environment.Type.COMPARABLE, left.getType());
                requireAssignable(Environment.Type.COMPARABLE, right.getType());
                requireAssignable(left.getType(), right.getType());
                ast.setType(Environment.Type.BOOLEAN);
                break;
            case "+":
                // Either side is a string
                if (left.getType() == Environment.Type.STRING ||
                        right.getType() == Environment.Type.STRING) {
                    ast.setType(Environment.Type.STRING);
                    break;
                }
            case "-": case "*": case "/": // case '+': if not a STRING

                // The left hand side must be an Integer/Decimal and
                // both the right hand side and result type are the same as the left.

                if (left.getType() == Environment.Type.INTEGER ||
                        left.getType() == Environment.Type.DECIMAL) {
                    if (left.getType() == right.getType()) {
                        ast.setType(left.getType());
                        return null;
                    }
                }
                throw new RuntimeException("Expected Integer or Decimal");
            default:
                return null;
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        if (ast.getReceiver().isPresent()) {
            // is field
            Ast.Expr expr = ast.getReceiver().get();
            visit(expr);
            ast.setVariable(expr.getType().getField(ast.getName()));
        } else {
            // is not field
            ast.setVariable(scope.lookupVariable(ast.getName()));
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (target.getName().equals(type.getName()))
            return;

        switch (target.getName()) {
            case "Any":
                return;
            case "Comparable":
                if (type.getName().equals("Integer") ||
                        type.getName().equals("Decimal")||
                        type.getName().equals("Character") ||
                        type.getName().equals("String"))
                    return;
        }

        throw new RuntimeException("Wrong Type");
    }

}
