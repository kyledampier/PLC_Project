package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    public static String getJvmNameFromString(String typeName) {
        switch (typeName) {
            case "Boolean":
                return "boolean";
            case "Character":
                return "char";
            case "Decimal":
                return "double";
            case "Integer":
                return "int";
            case "String":
                return  "String";
            default:
                throw new RuntimeException("Unknown Type");
        }
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        throw new UnsupportedOperationException(); //TODO
//        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        print(getJvmNameFromString(ast.getTypeName()));
        print(" ", ast.getName());

        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }

        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        throw new UnsupportedOperationException(); //TODO
//        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        // print and check type
        if (ast.getTypeName().isPresent()) {
            print(getJvmNameFromString(ast.getTypeName().get()));
        } else {
            if (ast.getValue().isPresent()) {
                print(ast.getValue().get().getType().getJvmName());
            } else {
                throw new RuntimeException("Expected Value or Type Declaration.");
            }
        }

        // print variable name
        print(" ", ast.getName());

        // check for value
        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }

        // end statement
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        // TODO: write tests for assignment
        visit(ast.getReceiver());
        print(" = ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        print("if (");
        visit(ast.getCondition());
        print(") {");
        indent++;

        // visit all statements
        for (Ast.Stmt statement : ast.getThenStatements()) {
            newline(indent);
            visit(statement);
        }

        if (ast.getElseStatements().size() > 0) {
            // has else statements
            newline(--indent);
            print("} else {");
            indent++;
            for (Ast.Stmt statement : ast.getElseStatements()) {
                newline(indent);
                visit(statement);
            }

        }

        // end if statements
        newline(--indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        print("for (");
        // for (jvmName name : value) {
        print(ast.getValue().getType().getJvmName(), " ", ast.getName());
        print(" : ");
        visit(ast.getValue());
        print(") {");
        indent++;

        // all statements
        for (Ast.Stmt stmt : ast.getStatements()) {
            newline(indent);
            visit(stmt);
        }

        newline(--indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        print("while (");
        visit(ast.getCondition());
        print(") {");
        indent++;

        // visit all statements
        for (Ast.Stmt stmt : ast.getStatements()) {
            newline(indent);
            visit(stmt);
        }

        newline(--indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        print ("return ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        if (ast.getType() == Environment.Type.BOOLEAN) {
            // IS BOOLEAN
            Boolean loc = (Boolean) ast.getLiteral();
            print(loc);
        } else if (ast.getType() == Environment.Type.INTEGER) {
            // IS INTEGER
            BigInteger loc = (BigInteger) ast.getLiteral();
            print(loc);
        } else if (ast.getType() == Environment.Type.STRING) {
            // IS STRING
            String loc = (String) ast.getLiteral();
            print("\"", loc, "\"");
        } else if (ast.getType() == Environment.Type.CHARACTER) {
            // IS CHARACTER
            char loc = (char) ast.getLiteral();
            print(loc);
        } else if (ast.getType() == Environment.Type.DECIMAL) {
            // IS DECIMAL
            // TODO: check for decimal precision
            BigDecimal loc = (BigDecimal) ast.getLiteral();
            print(loc.toString());
        } else {
            throw new RuntimeException("Type Error");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        // TODO: write tests
        writer.write("(");
        visit(ast.getExpression());
        writer.write(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        visit(ast.getLeft());
        switch (ast.getOperator()) {
            case "AND" -> print(" && ");
            case "+" -> print(" + ");
            case "-" -> print(" - ");
            case "*" -> print(" * ");
            case "/" -> print(" / ");
        }
        visit(ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        // TODO: write tests for access
        if (ast.getReceiver().isPresent()) {
            // HAS RECEIVER
            visit(ast.getReceiver().get());
            print(".");
        }

        print(ast.getName());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        Environment.Function func = ast.getFunction();
        if (ast.getReceiver().isPresent()) {
            // HAS RECEIVER
            visit(ast.getReceiver().get());
            print(".");
        }

        print(func.getJvmName(), "(");
        // print out arguments
        List<Ast.Expr> args = ast.getArguments();
        if (args.size() > 0) {
            // HAS ARGUMENTS
            for (int i = 0; i < args.size() - 1; i++) {
                visit(args.get(i));
                print(", ");
            }
            visit(args.get(args.size() - 1));
        }
        print(")");

        return null;
    }

}