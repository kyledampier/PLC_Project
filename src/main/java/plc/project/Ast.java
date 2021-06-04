package plc.project;

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
        private final Optional<Expr> value;

        public Field(String name, Optional<Expr> value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public Optional<Expr> getValue() {
            return value;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Field &&
                    name.equals(((Field) obj).name) &&
                    value.equals(((Field) obj).value);
        }

        @Override
        public String toString() {
            return "Ast.Field{" +
                    "name='" + name + '\'' +
                    ", value=" + value +
                    '}';
        }

    }

    public static final class Method extends Ast {

        private final String name;
        private final List<String> parameters;
        private final List<Stmt> statements;

        public Method(String name, List<String> parameters, List<Stmt> statements) {
            this.name = name;
            this.parameters = parameters;
            this.statements = statements;
        }

        public String getName() {
            return name;
        }

        public List<String> getParameters() {
            return parameters;
        }

        public List<Stmt> getStatements() {
            return statements;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Method &&
                    name.equals(((Method) obj).name) &&
                    parameters.equals(((Method) obj).parameters) &&
                    statements.equals(((Method) obj).statements);
        }

        @Override
        public String toString() {
            return "Ast.Function{" +
                    "name='" + name + '\'' +
                    ", parameters=" + parameters +
                    ", statements=" + statements +
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

            private String name;
            private Optional<Expr> value;

            public Declaration(String name, Optional<Expr> value) {
                this.name = name;
                this.value = value;
            }

            public String getName() {
                return name;
            }

            public Optional<Expr> getValue() {
                return value;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Declaration &&
                        name.equals(((Declaration) obj).name) &&
                        value.equals(((Declaration) obj).value);
            }

            @Override
            public String toString() {
                return "Ast.Stmt.Declaration{" +
                        "name='" + name + '\'' +
                        ", value=" + value +
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

        public static final class Literal extends Expr {

            private final Object literal;

            public Literal(Object literal) {
                this.literal = literal;
            }

            public Object getLiteral() {
                return literal;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Literal &&
                        Objects.equals(literal, ((Literal) obj).literal);
            }

            @Override
            public String toString() {
                return "Ast.Expr.Literal{" +
                        "literal=" + literal +
                        '}';
            }

        }

        public static final class Group extends Expr {

            private final Expr expression;

            public Group(Expr expression) {
                this.expression = expression;
            }

            public Expr getExpression() {
                return expression;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Group &&
                        expression.equals(((Group) obj).expression);
            }

            @Override
            public String toString() {
                return "Ast.Expr.Group{" +
                        "expression=" + expression +
                        '}';
            }

        }

        public static final class Binary extends Expr {

            private final String operator;
            private final Expr left;
            private final Expr right;

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
            public boolean equals(Object obj) {
                return obj instanceof Binary &&
                        operator.equals(((Binary) obj).operator) &&
                        left.equals(((Binary) obj).left) &&
                        right.equals(((Binary) obj).right);
            }

            @Override
            public String toString() {
                return "Ast.Expr.Binary{" +
                        "operator='" + operator + '\'' +
                        ", left=" + left +
                        ", right=" + right +
                        '}';
            }

        }

        public static final class Access extends Expr {

            private final Optional<Expr> receiver;
            private final String name;

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

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Access &&
                        receiver.equals(((Access) obj).receiver) &&
                        name.equals(((Access) obj).name);
            }

            @Override
            public String toString() {
                return "Ast.Expr.Access{" +
                        "receiver=" + receiver +
                        ", name='" + name + '\'' +
                        '}';
            }

        }

        public static final class Function extends Expr {

            private final Optional<Expr> receiver;
            private final String name;
            private final List<Expr> arguments;

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

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Function &&
                        receiver.equals(((Function) obj).receiver) &&
                        name.equals(((Function) obj).name) &&
                        arguments.equals(((Function) obj).arguments);
            }

            @Override
            public String toString() {
                return "Ast.Expr.Function{" +
                        "receiver=" + receiver +
                        ", name='" + name + '\'' +
                        ", arguments=" + arguments +
                        '}';
            }

        }

    }

}
