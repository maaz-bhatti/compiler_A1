package src;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ManualScanner {
    private String input;
    private int currentPos;
    private int line;
    private int column;
    private SymbolTable symbolTable;

    // Statistics
    private int totalTokens = 0;
    private int linesProcessed = 0; // Will calculate at end or track dynamically
    private int commentsRemoved = 0; // Count of comment tokens skipped
    private Map<TokenType, Integer> tokenCounts = new java.util.EnumMap<>(TokenType.class);

    // definition of the token patterns
    private static class TokenPattern {
        TokenType type;
        Pattern pattern;

        TokenPattern(TokenType type, String regex) {
            this.type = type;
            this.pattern = Pattern.compile(regex);
        }
    }

    private List<TokenPattern> patterns;

    public ManualScanner(String filePath, SymbolTable symbolTable) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(filePath));
        this.input = new String(bytes);
        this.currentPos = 0;
        this.line = 1;
        this.column = 1;
        this.symbolTable = symbolTable;

        // Count lines roughly for stats
        this.linesProcessed = this.input.length() - this.input.replace("\n", "").length() + 1;

        initializePatterns();
    }

    private void initializePatterns() {
        patterns = new ArrayList<>();

        // Order matters! 3.12 Pattern Matching Priority

        // 1. Multi-line comments: #* ... *#
        // pattern: #\*([^*]|\*+[^#*])*\*+#
        patterns.add(new TokenPattern(TokenType.COMMENT, "#\\*([^*]|\\*+[^#*])*\\*+#"));

        // 2. Single-line comments: ## ...
        // pattern: ##[^\n]*
        patterns.add(new TokenPattern(TokenType.COMMENT, "##[^\n\r]*"));

        // 3. Multi-character operators
        // (\*\*|[+\-*/%]) is given in pdf but split into multi and single for priority
        // Multi: **, ==, !=, <=, >=, &&, ||, ++, --, +=, -=, *=, /=
        patterns.add(new TokenPattern(TokenType.OPERATOR, "\\*\\*|==|!=|<=|>=|&&|\\|\\||\\+\\+|--|\\+=|-=|\\*=|/="));

        // 4. Keywords
        patterns.add(new TokenPattern(TokenType.KEYWORD,
                "start|finish|loop|condition|declare|output|input|function|return|break|continue|else"));

        // 5. Boolean Literal
        patterns.add(new TokenPattern(TokenType.BOOLEAN_LITERAL, "true|false"));

        // 6. Identifiers
        // Regex: [A-Z][a-z0-9 ]{0,30}
        patterns.add(new TokenPattern(TokenType.IDENTIFIER, "[A-Z][a-z0-9 ]{0,30}"));

        // 7. Floating-Point Literals
        // Regex: [+-]?[0-9]+\.[0-9]{1,6}([eE][+-]?[0-9]+)?
        patterns.add(new TokenPattern(TokenType.FLOAT_LITERAL, "[+-]?[0-9]+\\.[0-9]{1,6}([eE][+-]?[0-9]+)?"));

        // 8. Integer Literals
        // Regex: [+-]?[0-9]+
        patterns.add(new TokenPattern(TokenType.INTEGER_LITERAL, "[+-]?[0-9]+"));

        // 9. String Literals
        // Regex: "([ ^"\\\n]|\\["\\ntr])*"
        // Java string for regex: "([^\"\\\\\n]|\\\\[\"\\\\ntr])*"
        // Note: PDF says [ ^...] which we interpret as negate [^...]
        patterns.add(new TokenPattern(TokenType.STRING_LITERAL, "\"([^\"\\\\\n]|\\\\[\"\\\\ntr])*\""));

        // 9b. Character Literals (Included in 9 in pdf list but separate type)
        // Regex: '([ ^'\\\n]|\\[\ntr])'
        patterns.add(new TokenPattern(TokenType.CHAR_LITERAL, "'([^'\\\\\n]|\\\\[\\\\'ntr])'"));

        // 10. Single-character operators
        // +, -, *, /, %, =, <, >, !
        patterns.add(new TokenPattern(TokenType.OPERATOR, "[+\\-*/%=<>!]"));

        // 11. Punctuators
        // (){}[] , ; :
        patterns.add(new TokenPattern(TokenType.PUNCTUATOR, "[(){}\\[\\],;:]"));

        // 12. Whitespace
        patterns.add(new TokenPattern(TokenType.UNKNOWN, "[ \t\r\n]+"));
    }

    public Token nextToken() {
        if (currentPos >= input.length()) {
            return new Token(TokenType.EOF, "", line, column);
        }

        // Longest match logic
        TokenType bestType = null;
        String bestMatch = null;
        int bestLength = 0;

        String subInput = input.substring(currentPos);

        // Check all patterns
        for (TokenPattern tp : patterns) {
            Matcher matcher = tp.pattern.matcher(subInput);
            if (matcher.lookingAt()) {
                String match = matcher.group();
                int length = match.length();

                // Longest match takes precedence
                if (length > bestLength) {
                    bestLength = length;
                    bestMatch = match;
                    bestType = tp.type;
                }
                // If equal length, priority list order (already sorted in list) wins?
                // Actually, strict priority means first match in list wins IF lengths are
                // equal?
                // Generally Longest Match is rule #1.
                // Priority list is usually for detecting ambiguity (like keyword vs
                // identifier).
                // But if they are same length and one is earlier in priority, we should
                // probably keep the earlier one?
                // Example: 'start' is keyword (priority 4). 'Start' is ID (priority 6).
                // They don't overlap.
                // Example: 'true' is Boolean (priority 5). 'true' is not ID (ID needs
                // uppercase).
                // Overlap check: Integers inside Floats? `1.2`. `1` is int (len 1). `1.2` is
                // float (len 3). Float wins.
                // So strict Longest Match is safest.
                // If equal length: use the one that appeared first in the Pattern List (since
                // list is sorted by Priority).
            }
        }

        if (bestMatch == null) {
            // No match found - Advance one char as unknown/error
            String invalidChar = String.valueOf(input.charAt(currentPos));

            // Log error or just return unknown
            System.out.println("Error: Unrecognized token at Line " + line + ", Col " + column + ": " + invalidChar);

            Token t = new Token(TokenType.UNKNOWN, invalidChar, line, column);
            updatePosition(invalidChar);
            return t;
        }

        // We have a match
        // 1. Handle Whitespace (SKIP)
        // Note: My pattern for Whitespace was put as UNKNOWN or I can use a dummy Type
        // Actually, if it matched Whitespace regex (last in list), we should skip and
        // recurse.
        // Wait, "Removes unnecessary whitespace". "Preserves whitespace in string
        // literals".
        // If the Best Match is Whitespace (by checking regex or value), we skip.
        // The Whitespace regex is known to be the last one.

        // Let's identify if it was the Whitespace pattern.
        // I can make a specific check.
        if (bestMatch.matches("[ \t\r\n]+")) {
            // It is whitespace
            updatePosition(bestMatch);
            return nextToken(); // Recursively call to get next real token
        }

        // 2. Handle Comments (SKIP but count)
        if (bestType == TokenType.COMMENT) {
            commentsRemoved++;
            updatePosition(bestMatch);
            return nextToken();
        }

        // 3. Normal Token
        Token token = new Token(bestType, bestMatch, line, column);

        // Update Symbol Table for Identifiers
        if (bestType == TokenType.IDENTIFIER) {
            symbolTable.addIdentifier(bestMatch, line);
        }

        // Update Stats
        totalTokens++;
        tokenCounts.put(bestType, tokenCounts.getOrDefault(bestType, 0) + 1);

        updatePosition(bestMatch);
        return token;
    }

    private void updatePosition(String text) {
        for (char c : text.toCharArray()) {
            if (c == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        currentPos += text.length();
    }

    public void printStats() {
        System.out.println("\nScanner Statistics:");
        System.out.println("Total Tokens: " + totalTokens);
        System.out.println("Lines Processed: " + linesProcessed);
        System.out.println("Comments Removed: " + commentsRemoved);
        for (TokenType t : tokenCounts.keySet()) {
            System.out.println(t + ": " + tokenCounts.get(t));
        }
    }
}
