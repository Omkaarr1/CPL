package com.example.lang;

import java.util.ArrayList;
import java.util.List;

abstract class Node {
}

// Expression Nodes
class BinaryNode extends Node {
    Node left, right;
    TokenType op;

    BinaryNode(Node left, TokenType op, Node right) {
        this.left = left;
        this.op = op;
        this.right = right;
    }

    @Override
    public String toString() {
        return "BinaryNode(" + left + " " + op + " " + right + ")";
    }
}

class UnaryNode extends Node {
    TokenType op;
    Node expr;
    boolean postfix;

    UnaryNode(TokenType op, Node expr, boolean postfix) {
        this.op = op;
        this.expr = expr;
        this.postfix = postfix;
    }
}

class LiteralNode extends Node {
    Object value;

    LiteralNode(Object value) {
        this.value = value;
    }
}

class VariableNode extends Node {
    String name;

    VariableNode(String name) {
        this.name = name;
    }
}

class AssignNode extends Node {
    String name;
    TokenType op;
    Node value;

    AssignNode(String name, TokenType op, Node value) {
        this.name = name;
        this.op = op;
        this.value = value;
    }
}

class PrintNode extends Node {
    Node expr;

    PrintNode(Node expr) {
        this.expr = expr;
    }
}

class IfNode extends Node {
    Node condition;
    List<Node> ifBranch;
    List<Node> elseBranch;

    IfNode(Node condition, List<Node> ifBranch, List<Node> elseBranch) {
        this.condition = condition;
        this.ifBranch = ifBranch;
        this.elseBranch = elseBranch;
    }
}

class ExpressionStatement extends Node {
    Node expr;

    ExpressionStatement(Node expr) {
        this.expr = expr;
    }
}

class ForNode extends Node {
    Node initialization;
    Node condition;
    Node increment;
    List<Node> body;

    ForNode(Node initialization, Node condition, Node increment, List<Node> body) {
        this.initialization = initialization;
        this.condition = condition;
        this.increment = increment;
        this.body = body;
    }
}

class InputNode extends Node {
    Node prompt;
    Node variable;

    InputNode(Node prompt, Node variable) {
        this.prompt = prompt;
        this.variable = variable;
    }
}

class WhileNode extends Node {
    Node condition;
    List<Node> body;

    WhileNode(Node condition, List<Node> body) {
        this.condition = condition;
        this.body = body;
    }
}

class FunctionDefNode extends Node {
    String name;
    List<String> parameters;
    List<Node> body;

    FunctionDefNode(String name, List<String> parameters, List<Node> body) {
        this.name = name;
        this.parameters = parameters;
        this.body = body;
    }
}

class FunctionCallNode extends Node {
    String name;
    List<Node> arguments;

    FunctionCallNode(String name, List<Node> arguments) {
        this.name = name;
        this.arguments = arguments;
    }
}

class ReturnNode extends Node {
    Node value;

    ReturnNode(Node value) {
        this.value = value;
    }
}

class ArrayLiteralNode extends Node {
    List<Node> elements;

    ArrayLiteralNode(List<Node> elements) {
        this.elements = elements;
    }
}

// New node for event triggers
class EventTriggerNode extends Node {
    Node timeExpr;
    String unit; // null if datetime trigger
    Node action;
    Node timesExpr; // null if not provided

    EventTriggerNode(Node timeExpr, String unit, Node action) {
        this(timeExpr, unit, action, null);
    }

    EventTriggerNode(Node timeExpr, String unit, Node action, Node timesExpr) {
        this.timeExpr = timeExpr;
        this.unit = unit;
        this.action = action;
        this.timesExpr = timesExpr;
    }
}

class UseNode extends Node {
    String libraryName;

    UseNode(String libraryName) {
        this.libraryName = libraryName;
    }
}

class ObjectMethodCallNode extends Node {
    Node target;
    String methodName;
    List<Node> arguments;

    ObjectMethodCallNode(Node target, String methodName, List<Node> arguments) {
        this.target = target;
        this.methodName = methodName;
        this.arguments = arguments;
    }
}

// Represents an array indexing operation, e.g., employees[i]
class IndexNode extends Node {
    Node target; // The array or list being indexed
    Node index; // The index expression

    IndexNode(Node target, Node index) {
        this.target = target;
        this.index = index;
    }
}

// Represents an assignment to an array element, e.g., employees[i] = value;
class AssignIndexNode extends Node {
    Node target; // The array or list being indexed
    Node index; // The index expression
    TokenType op; // The assignment operator (e.g., ASSIGN, PLUS_EQ)
    Node value; // The value to assign

    AssignIndexNode(Node target, Node index, TokenType op, Node value) {
        this.target = target;
        this.index = index;
        this.op = op;
        this.value = value;
    }
}

public class Parser {   
    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // Helper Methods

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token advanceToken() {
        if (!isAtEnd())
            current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advanceToken();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd())
            return false;
        return peek().type == type;
    }

    private Token consume(TokenType type, String message) {
        if (check(type))
            return advanceToken();
        Token token = peek();
        throw error(token, message);
    }

    private RuntimeException error(Token token, String message) {
        return new RuntimeException("Parse Error at " + token.line + ":" + token.column + " - " + message);
    }

    // Parsing Methods

    public List<Node> parse() {
        List<Node> statements = new ArrayList<>();
        while (!isAtEnd()) {
            try {
                statements.add(parseStatement());
            } catch (RuntimeException e) {
                System.err.println("Parse Error: " + e.getMessage());
                break; // Stop parsing further if there's an error
            }
        }
        return statements;
    }

    // Helper to convert the AST to a String representation (optional)
    public String getParseResultAsString() {
        List<Node> nodes = parse();
        StringBuilder sb = new StringBuilder();
        for (Node node : nodes) {
            sb.append(node.toString()).append("\n");
        }
        return sb.toString();
    }

    public Node parseStatement() {
        if (match(TokenType.FUNCTION)) {
            return parseFunctionDef();
        }

        if (match(TokenType.RETURN)) {
            return parseReturn();
        }

        if (match(TokenType.FOR)) {
            return parseFor();
        }

        if (match(TokenType.WHILE)) {
            return parseWhile();
        }

        if (match(TokenType.IF)) {
            return parseIf();
        }

        if (match(TokenType.PRINT)) {
            return parsePrint();
        }

        if (match(TokenType.INPUT)) {
            return parseInput();
        }

        if (match(TokenType.EVENT_TRIGGER)) {
            return parseEventTrigger();
        }

        if (match(TokenType.USE)) {
            return parseUseStatement();
        }

        Node expr = parseExpression();

        // Handle standalone function calls, method calls, or indexing without being
        // part of an assignment
        if (expr instanceof FunctionCallNode || expr instanceof ObjectMethodCallNode || expr instanceof IndexNode) {
            consume(TokenType.SEMICOLON, "Expect ';' after expression.");
            return new ExpressionStatement(expr);
        }

        consume(TokenType.SEMICOLON, "Expect ';' after expression.");
        return new ExpressionStatement(expr);
    }

    private Node parseUseStatement() {
        Token libName = consume(TokenType.IDENTIFIER, "Expect library name after 'use'.");
        consume(TokenType.SEMICOLON, "Expect ';' after library name.");
        return new UseNode(libName.lexeme);
    }

    private Node parseFunctionDef() {
        Token nameToken = consume(TokenType.IDENTIFIER, "Expect function name.");
        String name = nameToken.lexeme;

        consume(TokenType.LPAREN, "Expect '(' after function name.");
        List<String> parameters = new ArrayList<>();
        if (!match(TokenType.RPAREN)) {
            do {
                Token param = consume(TokenType.IDENTIFIER, "Expect parameter name.");
                parameters.add(param.lexeme);
            } while (match(TokenType.COMMA));
            consume(TokenType.RPAREN, "Expect ')' after parameters.");
        }

        consume(TokenType.LBRACE, "Expect '{' to start function body.");
        List<Node> body = parseBlock();
        consume(TokenType.RBRACE, "Expect '}' after function body.");

        // Optional semicolon after function definition
        if (match(TokenType.SEMICOLON)) {
            // Optional semicolon consumed
        }

        return new FunctionDefNode(name, parameters, body);
    }

    private Node parseReturn() {
        Node value = null;
        if (!check(TokenType.SEMICOLON)) {
            value = parseExpression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after return value.");
        return new ReturnNode(value);
    }

    private Node parseFor() {
        consume(TokenType.LPAREN, "Expect '(' after 'for'.");
        Node initialization = parseExpression();
        consume(TokenType.SEMICOLON, "Expect ';' after for initialization.");
        Node condition = parseExpression();
        consume(TokenType.SEMICOLON, "Expect ';' after for condition.");
        Node increment = parseExpression();
        consume(TokenType.RPAREN, "Expect ')' after for clauses.");
        consume(TokenType.LBRACE, "Expect '{' to start for loop body.");
        List<Node> body = parseBlock();
        consume(TokenType.RBRACE, "Expect '}' after for loop body.");
        return new ForNode(initialization, condition, increment, body);
    }

    private Node parseWhile() {
        consume(TokenType.LPAREN, "Expect '(' after 'while'.");
        Node condition = parseExpression();
        consume(TokenType.RPAREN, "Expect ')' after while condition.");
        consume(TokenType.LBRACE, "Expect '{' to start while body.");
        List<Node> body = parseBlock();
        consume(TokenType.RBRACE, "Expect '}' after while body.");
        return new WhileNode(condition, body);
    }

    private Node parseIf() {
        consume(TokenType.LPAREN, "Expect '(' after 'if'.");
        Node condition = parseExpression();
        consume(TokenType.RPAREN, "Expect ')' after if condition.");

        consume(TokenType.LBRACE, "Expect '{' to start 'if' branch.");
        List<Node> ifBranch = parseBlock();
        consume(TokenType.RBRACE, "Expect '}' after 'if' branch.");

        List<Node> elseBranch = null;
        if (match(TokenType.ELSE)) {
            consume(TokenType.LBRACE, "Expect '{' to start 'else' branch.");
            elseBranch = parseBlock();
            consume(TokenType.RBRACE, "Expect '}' after 'else' branch.");
        }
        return new IfNode(condition, ifBranch, elseBranch);
    }

    private Node parsePrint() {
        consume(TokenType.ARROW, "Expect '->' after 'print'.");
        Node expr = parseExpression();
        consume(TokenType.SEMICOLON, "Expect ';' after print statement.");
        return new PrintNode(expr);
    }

    private Node parseInput() {
        consume(TokenType.ARROW, "Expect '->' after 'input'.");
        Node prompt = parseExpression();
        consume(TokenType.ARROW, "Expect '->' before variable in input statement.");
        Node variable = parseExpression();
        consume(TokenType.SEMICOLON, "Expect ';' after input statement.");
        return new InputNode(prompt, variable);
    }

    private Node parseEventTrigger() {
        // @EVENT_TRIGGER(duration,"seconds") -> statement;
        // @EVENT_TRIGGER("YYYY-MM-DD HH:MM:SS") -> statement;

        consume(TokenType.LPAREN, "Expect '(' after @EVENT_TRIGGER.");

        Node timeNode = parseExpression();
        String unit = null;
        Node timesNode = null;

        // Check if we have a comma indicating a unit
        if (match(TokenType.COMMA)) {
            Token unitToken = consume(TokenType.STRING, "Expect a time unit as a string.");
            unit = unitToken.lexeme;

            // Check if we have another comma indicating the times parameter
            if (match(TokenType.COMMA)) {
                // times should be a number
                timesNode = parseExpression();
            }
        }

        consume(TokenType.RPAREN, "Expect ')' after event trigger time.");
        consume(TokenType.ARROW, "Expect '->' after event trigger declaration.");

        Node action = parseStatement();

        return new EventTriggerNode(timeNode, unit, action, timesNode);
    }

    private List<Node> parseBlock() {
        List<Node> statements = new ArrayList<>();
        while (!isAtEnd() && peek().type != TokenType.RBRACE) {
            statements.add(parseStatement());
        }
        return statements;
    }

    public Node parseExpression() {
        return parseAssignment();
    }

    private Node parseAssignment() {
        Node left = parseLogicalOr();

        if (match(TokenType.ASSIGN, TokenType.PLUS_EQ, TokenType.MINUS_EQ, TokenType.STAR_EQ, TokenType.SLASH_EQ)) {
            Token op = previous();
            Node right = parseAssignment();

            if (left instanceof VariableNode) {
                return new AssignNode(((VariableNode) left).name, op.type, right);
            } else if (left instanceof IndexNode) {
                // Handle assignment to array elements, e.g., employees[i] = value;
                IndexNode indexNode = (IndexNode) left;
                return new AssignIndexNode(indexNode.target, indexNode.index, op.type, right);
            } else {
                throw error(op, "Invalid assignment target.");
            }
        }
        return left;
    }

    private Node parseLogicalOr() {
        Node left = parseLogicalAnd();
        while (match(TokenType.OR_OR)) {
            Token op = previous();
            Node right = parseLogicalAnd();
            left = new BinaryNode(left, op.type, right);
        }
        return left;
    }

    private Node parseLogicalAnd() {
        Node left = parseEquality();
        while (match(TokenType.AND_AND)) {
            Token op = previous();
            Node right = parseEquality();
            left = new BinaryNode(left, op.type, right);
        }
        return left;
    }

    private Node parseEquality() {
        Node left = parseComparison();
        while (match(TokenType.EQ_EQ, TokenType.NOT_EQ)) {
            Token op = previous();
            Node right = parseComparison();
            left = new BinaryNode(left, op.type, right);
        }
        return left;
    }

    private Node parseComparison() {
        Node left = parseTerm();
        while (match(TokenType.GT, TokenType.GT_EQ, TokenType.LT, TokenType.LT_EQ)) {
            Token op = previous();
            Node right = parseTerm();
            left = new BinaryNode(left, op.type, right);
        }
        return left;
    }

    private Node parseTerm() {
        Node left = parseFactor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token op = previous();
            Node right = parseFactor();
            left = new BinaryNode(left, op.type, right);
        }
        return left;
    }

    private Node parseFactor() {
        Node left = parseUnary();
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.MOD)) {
            Token op = previous();
            Node right = parseUnary();
            left = new BinaryNode(left, op.type, right);
        }
        return left;
    }

    private Node parseUnary() {
        if (match(TokenType.PLUS_PLUS, TokenType.MINUS_MINUS, TokenType.NOT)) {
            Token op = previous();
            Node expr = parseUnary();
            return new UnaryNode(op.type, expr, false);
        }

        Node primary = parsePrimary();

        while (match(TokenType.PLUS_PLUS, TokenType.MINUS_MINUS)) {
            Token op = previous();
            primary = new UnaryNode(op.type, primary, true);
        }

        return primary;
    }

    private Node parsePrimary() {
        if (match(TokenType.NUMBER)) {
            String numStr = previous().lexeme;
            if (numStr.contains(".")) {
                return new LiteralNode(Double.parseDouble(numStr));
            } else {
                return new LiteralNode(Integer.parseInt(numStr));
            }
        }

        if (match(TokenType.STRING)) {
            return new LiteralNode(previous().lexeme);
        }

        if (match(TokenType.BOOLEAN)) {
            String boolStr = previous().lexeme;
            return new LiteralNode(Boolean.parseBoolean(boolStr));
        }

        if (match(TokenType.IDENTIFIER)) {
            String identifier = previous().lexeme;
            Node expr = new VariableNode(identifier);

            // Handle function calls, method calls, and array indexing
            while (true) {
                if (match(TokenType.LPAREN)) {
                    // Function call
                    List<Node> args = new ArrayList<>();
                    if (!check(TokenType.RPAREN)) {
                        do {
                            args.add(parseExpression());
                        } while (match(TokenType.COMMA));
                    }
                    consume(TokenType.RPAREN, "Expect ')' after function arguments.");
                    expr = new FunctionCallNode(identifier, args);
                } else if (match(TokenType.DOT)) {
                    // Method call
                    Token methodNameToken = consume(TokenType.IDENTIFIER, "Expect method name after '.'");
                    String methodName = methodNameToken.lexeme;
                    consume(TokenType.LPAREN, "Expect '(' after method name.");
                    List<Node> args = new ArrayList<>();
                    if (!check(TokenType.RPAREN)) {
                        do {
                            args.add(parseExpression());
                        } while (match(TokenType.COMMA));
                    }
                    consume(TokenType.RPAREN, "Expect ')' after method arguments.");
                    expr = new ObjectMethodCallNode(expr, methodName, args);
                } else if (match(TokenType.LBRACKET)) {
                    // Array indexing
                    Node indexExpr = parseExpression();
                    consume(TokenType.RBRACKET, "Expect ']' after index expression.");
                    expr = new IndexNode(expr, indexExpr);
                } else {
                    break;
                }
            }

            return expr;
        }

        if (match(TokenType.LPAREN)) {
            Node expr = parseExpression();
            consume(TokenType.RPAREN, "Expect ')' after expression.");
            return expr;
        }

        if (match(TokenType.LBRACKET)) {
            List<Node> elements = new ArrayList<>();
            if (!match(TokenType.RBRACKET)) {
                do {
                    elements.add(parseExpression());
                } while (match(TokenType.COMMA));
                consume(TokenType.RBRACKET, "Expect ']' after array elements.");
            }
            return new ArrayLiteralNode(elements);
        }

        Token token = peek();
        throw error(token, "Expect expression.");
    }
}
