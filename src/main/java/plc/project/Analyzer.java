package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
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
    private Ast.Expr returnType;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        if(scope.lookupFunction("main", 0) != null && scope.lookupFunction("main", 0).getReturnType() == Environment.Type.INTEGER) {
            for(Ast.Field field : ast.getFields()) {
                visit(field);
            }
            for(Ast.Method method : ast.getMethods()) {
                visit(method);
            }
        } else {
            throw new RuntimeException();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        if (ast.getValue().isPresent()) { // if present, visit before defining variable
            Ast.Expr value = ast.getValue().get();
            Environment.Type target = Environment.getType(ast.getTypeName());

            // check if the value is assignable
            if (value.getType() == target ||
                    target == Environment.Type.ANY ||
                    (target == Environment.Type.COMPARABLE && (value.getType() == Environment.Type.INTEGER ||
                            value.getType() == Environment.Type.DECIMAL ||
                            value.getType() == Environment.Type.STRING ||
                            value.getType() == Environment.Type.CHARACTER)
                    )
            ) {
                // field value is assignable
                visit(ast.getValue().get());
            } else {
                throw new RuntimeException();
            }
        }
        // defines variable
        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()), Environment.NIL));
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        List<Environment.Type> paramTypes = new ArrayList<>();
        ast.getParameterTypeNames().forEach(s -> {
            paramTypes.add(Environment.getType(s));
        });

        Environment.Type returnType = Environment.Type.NIL;
        if (ast.getReturnTypeName().isPresent()) {
            returnType = Environment.getType(ast.getReturnTypeName().get());
        }

        ast.setFunction(scope.defineFunction(ast.getName(), ast.getName(), paramTypes, returnType, args -> Environment.NIL));
        try {
            scope = new Scope(scope);

            for (int i = 0; i < ast.getParameters().size(); i++) {
                scope.defineVariable(ast.getParameters().get(i), ast.getParameters().get(i), paramTypes.get(i), Environment.NIL);
            }

            for (Ast.Stmt stmt : ast.getStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        if (!(ast.getExpression() instanceof Ast.Expr.Function)) {
            throw new RuntimeException("Expected Ast.Expr.Function");
        }
        visit(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {

        if (!ast.getTypeName().isPresent() && !ast.getValue().isPresent()) {
            throw new RuntimeException("Expected type or value when declaring a variable.");
        }

        Environment.Type type = null;

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
        if (!(ast.getReceiver() instanceof Ast.Expr.Access)) {
            // is not an Access, throw error
            throw new RuntimeException("Expected Access Expression");
        }

        visit(ast.getReceiver());
        visit(ast.getValue());

        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        visit(ast.getCondition());
        if (ast.getCondition().getType() != Environment.Type.BOOLEAN || ast.getThenStatements().isEmpty()) {
            throw new RuntimeException();
        }

        for (Ast.Stmt then : ast.getThenStatements()) {
            try {
                scope = new Scope(scope);
                visit(then);
            } finally {
                scope = scope.getParent();
            }
        }
        for (Ast.Stmt elseStmt : ast.getElseStatements()) {
            try {
                scope = new Scope(scope);
                visit(elseStmt);
            } finally {
                scope = scope.getParent();
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        visit(ast.getValue());
        if (ast.getValue().getType() != Environment.Type.INTEGER_ITERABLE || ast.getStatements().isEmpty()) {
            throw new RuntimeException();
        }
        try {
            scope = new Scope(scope);
            scope.defineVariable(ast.getName(), ast.getName(), Environment.Type.INTEGER, Environment.NIL);
            for (Ast.Stmt stmt : ast.getStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        visit(ast.getCondition());
        if (ast.getCondition().getType() != Environment.Type.BOOLEAN) {
            scope = new Scope(scope);
            for (Ast.Stmt stmt : ast.getStatements()) {
                visit(stmt);
            }
        }
        return null;

    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        if (ast.getValue().getType() != returnType.getType()) {
            throw new RuntimeException();
        }
        return null;
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

        if (literal instanceof BigInteger) {
            // Check for size
            BigInteger value = (BigInteger) literal;
            if (value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0 &&
                    value.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0)
                ast.setType(Environment.Type.INTEGER);
            else
                throw new RuntimeException("Value of Integer is not in the range of and Integer.");

            return null;
        }


        if (literal instanceof BigDecimal) {
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
            case "AND":
            case "OR":
                if (left.getType() == Environment.Type.BOOLEAN &&
                        right.getType() == Environment.Type.BOOLEAN) {
                    ast.setType(Environment.Type.BOOLEAN);
                    return null;
                }
                throw new RuntimeException("Boolean Type Expected");
            case "<":
            case "<=":
            case ">":
            case ">=":
            case "==":
            case "!=":
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
            case "-":
            case "*":
            case "/": // case '+': if not a STRING

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

        if (ast.getReceiver().isPresent()) {
            // is Method
            Ast.Expr expr = ast.getReceiver().get();
            visit(expr);

            Environment.Function func = expr.getType().getMethod(ast.getName(), ast.getArguments().size());

            List<Ast.Expr> args = ast.getArguments();
            List<Environment.Type> argTypes = func.getParameterTypes();

            // starts at 1
            for (int i = 1; i < args.size(); i++) {
                visit(args.get(i));
                requireAssignable(argTypes.get(i), args.get(i).getType());
            }

            ast.setFunction(func);
        } else {
            // is Function

            Environment.Function func = scope.lookupFunction(ast.getName(), ast.getArguments().size());

            List<Ast.Expr> args = ast.getArguments();
            List<Environment.Type> argTypes = func.getParameterTypes();

            // starts at 0
            for (int i = 0; i < args.size(); i++) {
                visit(args.get(i));
                requireAssignable(argTypes.get(i), args.get(i).getType());
            }

            ast.setFunction(func);
        }

        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (target.getName().equals(type.getName()))
            return;

        switch (target.getName()) {
            case "Any":
                return;
            case "Comparable":
                if (type.getName().equals("Integer") ||
                        type.getName().equals("Decimal") ||
                        type.getName().equals("Character") ||
                        type.getName().equals("String"))
                    return;
        }

        throw new RuntimeException("Wrong Type");
    }

}
