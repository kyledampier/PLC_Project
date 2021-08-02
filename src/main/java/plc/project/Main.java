package plc.project;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        Lexer lex = new Lexer("DEF main(): Integer DO");
        Parser parser = new Parser(lex.lex());
        Scope scope = new Scope(null);
        Interpreter interptr = new Interpreter(scope);
        Analyzer analyzer = new Analyzer(scope);
    }
}
