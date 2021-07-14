package plc.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * See the Parser assignment specification for specific notes on each AST class
 * and how to use it.
 */
public abstract class Ast {

    public static final class Source extends Ast {

        private final List<Field> fields;
        private final List<Method> methods;

        public Source(List<Field> fields, List<Method> methods) {
            this.fields = fields;
            this.methods = methods;
        }

        public List<Field> getFields() {
            return fields;
        }

        public List<Method> getMethods() {
            return methods;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Source &&
                    fields.equals(((Source) obj).fields) &&
                    methods.equals(((Source) obj).methods);
        }

        @Override
        public String toString() {
            return "Ast.Source{" +
                    "fields=" + fields +
                    "functions=" + methods +
                    '}';
        }

    }

    public static final class Field extends Ast {

        private final String name;
        private final String typeName;
        private final Optional<Expr> value;
        private Environment.Variable variable = null;

        public Field(String name, Optional<Expr> value) {
            this(name, "Any", value);
        }

        public Field(String name, String typeName, Optional<Expr> value) {
            this.name = name;
            this.typeName = typeName;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getTypeName() {
            return typeName;
        }

        public Optional<Expr> getValue() {
            return value;
        }

        public Environment.Variable getVariable() {
            if (variable == null) {
                throw new IllegalStateException("variable is uninitialized");
            }
            return variable;
        }

        public void setVariable(Environment.Variable variable) {
            this.variable = variable;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Field &&
                    name.equals(((Field) obj).name) &&
                    typeName.equals(((Field) obj).typeName) &&
                    value.equals(((Field) obj).value) &&
                    Objects.equals(variable, ((Field) obj).variable);
        }

        @Override
        public String toString() {
            return "Field{" +
                    "name='" + name + '\'' +
                    ", typeName=" + typeName +
                    ", value=" + value +
                    ", variable=" + variable +
                    '}';
        }

    }

    public static final class Method extends Ast {

        private final String name;
        private final List<String> parameters;
        private final List<String> parameterTypeNames;
        private final Optional<String> returnTypeName;
        private final List<Stmt> statements;
        private Environment.Function function = null;

        public Method(String name, List<String> parameters, List<Stmt> statements) {
            this(name, parameters, new ArrayList<>(), Optional.of("Any"), statements);
            for (int i = 0; i < parameters.size(); i++) {
                parameterTypeNames.add("Any");
            }
        }

        public Method(String name, List<String> parameters, List<String> parameterTypeNames, Optional<String> returnTypeName, List<Stmt> statements) {
            this.name = name;
            this.parameters = parameters;
            this.parameterTypeNames = parameterTypeNames;
            this.returnTypeName = returnTypeName;
            this.statements = statements;
        }

        public String getName() {
            return name;
        }

        public List<String> getParameters() {
            return parameters;
        }

        public List<String> getParameterTypeNames() {
            return parameterTypeNames;
        }

        public Optional<String> getReturnTypeName() {
            return returnTypeName;
        }

        public List<Stmt> getStatements() {
            return statements;
        }

        public Environment.Function getFunction() {
            if (function == null) {
                throw new IllegalStateException("function is uninitialized");
            }
            return function;
        }

        public void setFunction(Environment.Function function) {
            this.function = function;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Method &&
                    name.equals(((Method) obj).name) &&
                    parameters.equals(((Method) obj).parameters) &&
                    parameterTypeNames.equals(((Method) obj).parameterTypeNames) &&
                    returnTypeName.equals(((Method) obj).returnTypeName) &&
                    statements.equals(((Method) obj).statements) &&
                    Objects.equals(function, ((Method) obj).function);
        }

        @Override
        public String toString() {
            return "Method{" +
                    "name='" + name + '\'' +
                    ", parameters=" + parameters +
                    ", parameterTypeNames=" + parameterTypeNames +
                    ", returnTypeName='" + returnTypeName + '\'' +
                    ", statements=" + statements +
                    ", function=" + function +
                    '}';
        }

    }

    public static abstract class Stmt extends Ast {

        public static final class Expression extends Stmt {

            private final Expr expression;

            public Expression(Expr expression) {
                this.expression = expression;
            }

            public Expr getExpression() {
                return expression;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Expression &&
                        expression.equals(((Expression) obj).expression);
            }

            @Override
            public String toString() {
                return "Ast.Stmt.Expression{" +
                        "expression=" + expression +
                        '}';
            }

        }

        public static final class Declaration extends Stmt {

            private final String name;
            private final Optional<String> typeName;
            private final Optional<Expr> value;
            private Environment.Variable variable;

            public Declaration(String name, Optional<Expr> value) {
                this(name, Optional.empty(), value);
            }

            public Declaration(String name, Optional<String> typeName, Optional<Expr> value) {
                this.name = name;
                this.typeName = typeName;
                this.value = value;
            }

            public String getName() {
                return name;
            }

            public Optional<String> getTypeName() {
                return typeName;
            }

            public Optional<Expr> getValue() {
                return value;
            }

            public Environment.Variable getVariable() {
                if (variable == null) {
                    throw new IllegalStateException("variable is uninitialized");
                }
                return variable;
            }

            public void setVariable(Environment.Variable variable) {
                this.variable = variable;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Declaration &&
                        name.equals(((Declaration) obj).name) &&
                        typeName.equals(((Declaration) obj).typeName) &&
                        value.equals(((Declaration) obj).value) &&
                        Objects.equals(variable, ((Declaration) obj).variable);
            }

            @Override
            public String toString() {
                return "Declaration{" +
                        "name='" + name + '\'' +
                        ", typeName=" + typeName +
                        ", value=" + value +
                        ", variable=" + variable +
                        '}';
            }

        }

        public static final class Assignment extends Stmt {

            private final Expr receiver;
            private final Expr value;

            public Assignment(Expr receiver, Expr value) {
                this.receiver = receiver;
                this.value = value;
            }

            public Expr getReceiver() {
                return receiver;
            }

            public Expr getValue() {
                return value;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Assignment &&
                        receiver.equals(((Assignment) obj).receiver) &&
                        value.equals(((Assignment) obj).value);
            }

            @Override
            public final String toString() {
                return "Ast.Stmt.Assignment{" +
                        "receiver=" + receiver +
                        ", value=" + value +
                        '}';
            }

        }

        public static final class If extends Stmt {

            private final Expr condition;
            private final List<Stmt> thenStatements;
            private final List<Stmt> elseStatements;


            public If(Expr condition, List<Stmt> thenStatements, List<Stmt> elseStatements) {
                this.condition = condition;
                this.thenStatements = thenStatements;
                this.elseStatements = elseStatements;
            }

            public Expr getCondition() {
                return condition;
            }

            public List<Stmt> getThenStatements() {
                return thenStatements;
            }

            public List<Stmt> getElseStatements() {
                return elseStatements;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof If &&
                        condition.equals(((If) obj).condition) &&
                        thenStatements.equals(((If) obj).thenStatements) &&
                        elseStatements.equals(((If) obj).elseStatements);
            }

            @Override
            public String toString() {
                return "Ast.Stmt.If{" +
                        "condition=" + condition +
                        ", thenStatements=" + thenStatements +
                        ", elseStatements=" + elseStatements +
                        '}';
            }

        }

        public static final class For extends Stmt {

            private final String name;
            private final Expr value;
            private final List<Stmt> statements;

            public For(String name, Expr value, List<Stmt> statements) {
                this.name = name;
                this.value = value;
                this.statements = statements;
            }

            public String getName() {
                return name;
            }

            public Expr getValue() {
                return value;
            }

            public List<Stmt> getStatements() {
                return statements;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof For &&
                        name.equals(((For) obj).name) &&
                        value.equals(((For) obj).value) &&
                        statements.equals(((For) obj).statements);
            }

            @Override
            public String toString() {
                return "For{" +
                        "name='" + name + '\'' +
                        ", value=" + value +
                        ", statements=" + statements +
                        '}';
            }

        }

        public static final class While extends Stmt {

            private final Expr condition;
            private final List<Stmt> statements;

            public While(Expr condition, List<Stmt> statements) {
                this.condition = condition;
                this.statements = statements;
            }

            public Expr getCondition() {
                return condition;
            }

            public List<Stmt> getStatements() {
                return statements;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof While &&
                        condition.equals(((While) obj).condition) &&
                        statements.equals(((While) obj).statements);
            }

            @Override
            public String toString() {
                return "Ast.Stmt.While{" +
                        "condition=" + condition +
                        ", statements=" + statements +
                        '}';
            }

        }

        public static final class Return extends Stmt {

            private final Expr value;

            public Return(Expr value) {
                this.value = value;
            }

            public Expr getValue() {
                return value;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Return &&
                        value.equals(((Return) obj).value);
            }

            @Override
            public String toString() {
                return "Ast.Stmt.Return{" +
                        "value=" + value +
                        '}';
            }

        }

    }

    public static abstract class Expr extends Ast {

        public abstract Environment.Type getType();

        public static final class Literal extends Expr {

            private final Object literal;
            private Environment.Type type = null;

            public Literal(Object literal) {
                this.literal = literal;
            }

            public Object getLiteral() {
                return literal;
            }

            @Override
            public Environment.Type getType() {
                if (type == null) {
                    throw new IllegalStateException("type is uninitialized");
                }
                return type;
            }

            public void setType(Environment.Type type) {
                this.type = type;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Literal &&
                        Objects.equals(literal, ((Literal) obj).literal) &&
                        Objects.equals(type, ((Literal) obj).type);
            }

            @Override
            public String toString() {
                return "Ast.Expr.Literal{" +
                        "literal=" + literal +
                        ", type=" + type +
                        '}';
            }

        }

        public static final class Group extends Expr {

            private final Expr expression;
            private Environment.Type type = null;

            public Group(Expr expression) {
                this.expression = expression;
            }

            public Expr getExpression() {
                return expression;
            }

            @Override
            public Environment.Type getType() {
                if (type == null) {
                    throw new IllegalStateException("type is uninitialized");
                }
                return type;
            }

            public void setType(Environment.Type type) {
                this.type = type;
            }
            @Override
            public boolean equals(Object obj) {
                return obj instanceof Group &&
                        expression.equals(((Group) obj).expression) &&
                        Objects.equals(type, ((Group) obj).type);
            }

            @Override
            public String toString() {
                return "Ast.Expr.Group{" +
                        "expression=" + expression +
                        ", type=" + type +
                        '}';
            }

        }

        public static final class Binary extends Expr {

            private final String operator;
            private final Expr left;
            private final Expr right;
            private Environment.Type type = null;

            public Binary(String operator, Expr left, Expr right) {
                this.operator = operator;
                this.left = left;
                this.right = right;
            }

            public String getOperator() {
                return operator;
            }

            public Expr getLeft() {
                return left;
            }

            public Expr getRight() {
                return right;
            }

            @Override
            public Environment.Type getType() {
                if (type == null) {
                    throw new IllegalStateException("type is uninitialized");
                }
                return type;
            }

            public void setType(Environment.Type type) {
                this.type = type;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Binary &&
                        operator.equals(((Binary) obj).operator) &&
                        left.equals(((Binary) obj).left) &&
                        right.equals(((Binary) obj).right) &&
                        Objects.equals(type, ((Binary) obj).type);
            }

            @Override
            public String toString() {
                return "Ast.Expr.Binary{" +
                        "operator='" + operator + '\'' +
                        ", left=" + left +
                        ", right=" + right +
                        ", type=" + type +
                        '}';
            }

        }

        public static final class Access extends Expr {

            private final Optional<Expr> receiver;
            private final String name;
            private Environment.Variable variable = null;

            public Access(Optional<Expr> receiver, String name) {
                this.receiver = receiver;
                this.name = name;
            }

            public Optional<Expr> getReceiver() {
                return receiver;
            }

            public String getName() {
                return name;
            }

            public Environment.Variable getVariable() {
                if (variable == null) {
                    throw new IllegalStateException("variable is uninitialized");
                }
                return variable;
            }

            public void setVariable(Environment.Variable variable) {
                this.variable = variable;
            }

            @Override
            public Environment.Type getType() {
                return getVariable().getType();
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Access &&
                        receiver.equals(((Access) obj).receiver) &&
                        name.equals(((Access) obj).name) &&
                        Objects.equals(variable, ((Access) obj).variable);
            }

            @Override
            public String toString() {
                return "Ast.Expr.Access{" +
                        "receiver=" + receiver +
                        ", name='" + name + '\'' +
                        ", variable=" + variable +
                        '}';
            }

        }

        public static final class Function extends Expr {

            private final Optional<Expr> receiver;
            private final String name;
            private final List<Expr> arguments;
            private Environment.Function function = null;

            public Function(Optional<Expr> receiver, String name, List<Expr> arguments) {
                this.receiver = receiver;
                this.name = name;
                this.arguments = arguments;
            }

            public Optional<Expr> getReceiver() {
                return receiver;
            }

            public String getName() {
                return name;
            }

            public List<Expr> getArguments() {
                return arguments;
            }

            public Environment.Function getFunction() {
                if (function == null) {
                    throw new IllegalStateException("function is uninitialized");
                }
                return function;
            }

            public void setFunction(Environment.Function function) {
                this.function = function;
            }

            @Override
            public Environment.Type getType() {
                return getFunction().getReturnType();
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Function &&
                        receiver.equals(((Function) obj).receiver) &&
                        name.equals(((Function) obj).name) &&
                        arguments.equals(((Function) obj).arguments) &&
                        Objects.equals(function, ((Function) obj).function);
            }

            @Override
            public String toString() {
                return "Ast.Expr.Function{" +
                        "receiver=" + receiver +
                        ", name='" + name + '\'' +
                        ", arguments=" + arguments +
                        ", function=" + function +
                        '}';
            }

        }

    }

    public interface Visitor<T> {

        default T visit(Ast ast) {
            if (ast instanceof Source) {
                return visit((Source) ast);
            } else if (ast instanceof Field) {
                return visit((Field) ast);
            } else if (ast instanceof Method) {
                return visit((Method) ast);
            } else if (ast instanceof Stmt.Expression) {
                return visit((Stmt.Expression) ast);
            } else if (ast instanceof Stmt.Declaration) {
                return visit((Stmt.Declaration) ast);
            } else if (ast instanceof Stmt.Assignment) {
                return visit((Stmt.Assignment) ast);
            } else if (ast instanceof Stmt.If) {
                return visit((Stmt.If) ast);
            } else if (ast instanceof Stmt.For) {
                return visit((Stmt.For) ast);
            } else if (ast instanceof Stmt.While) {
                return visit((Stmt.While) ast);
            } else if (ast instanceof Stmt.Return) {
                return visit((Stmt.Return) ast);
            } else if (ast instanceof Expr.Literal) {
                return visit((Expr.Literal) ast);
            } else if (ast instanceof Expr.Group) {
                return visit((Expr.Group) ast);
            } else if (ast instanceof Expr.Binary) {
                return visit((Expr.Binary) ast);
            } else if (ast instanceof Expr.Access) {
                return visit((Expr.Access) ast);
            } else if (ast instanceof Expr.Function) {
                return visit((Expr.Function) ast);
            } else {
                throw new AssertionError("Unimplemented AST type: " + ast.getClass().getName() + ".");
            }
        }

        T visit(Source ast);

        T visit(Field ast);

        T visit(Method ast);

        T visit(Stmt.Expression ast);

        T visit(Stmt.Declaration ast);

        T visit(Stmt.Assignment ast);

        T visit(Stmt.If ast);

        T visit(Stmt.For ast);

        T visit(Stmt.While ast);

        T visit(Stmt.Return ast);

        T visit(Expr.Literal ast);

        T visit(Expr.Group ast);

        T visit(Expr.Binary ast);

        T visit(Expr.Access ast);

        T visit(Expr.Function ast);

    }

}
