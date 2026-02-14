package src;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String testFile = "test_input.txt";

        // If argument provided
        if (args.length > 0) {
            testFile = args[0];
        }

        SymbolTable symbolTable = new SymbolTable();

        try {
            System.out.println("Scanning file: " + testFile);
            ManualScanner scanner = new ManualScanner(testFile, symbolTable);

            Token token;
            while ((token = scanner.nextToken()).getType() != TokenType.EOF) {
                if (token.getType() != TokenType.UNKNOWN) {
                    System.out.println(token);
                }
            }

            // Print Statistics
            scanner.printStats();

            // Print Symbol Table
            symbolTable.display();

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
