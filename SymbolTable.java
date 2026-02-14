package src;

import java.util.LinkedHashMap;
import java.util.Map;

class SymbolEntry {
    String name;
    String type;
    int firstLine;
    int frequency;

    public SymbolEntry(String name, String type, int firstLine) {
        this.name = name;
        this.type = type;
        this.firstLine = firstLine;
        this.frequency = 1;
    }
}

public class SymbolTable {
    private Map<String, SymbolEntry> table;

    public SymbolTable() {
        // LinkedHashMap to maintain insertion order for display if needed
        this.table = new LinkedHashMap<>();
    }

    public void addIdentifier(String name, int line) {
        // Check if identifier already exists
        if (table.containsKey(name)) {
            SymbolEntry entry = table.get(name);
            entry.frequency++;
        } else {
            // Default type "ID" as scanner doesn't know semantic types
            SymbolEntry entry = new SymbolEntry(name, "ID", line);
            table.put(name, entry);
        }
    }

    public void display() {
        System.out.println("\nSymbol Table:");
        System.out.printf("%-20s %-10s %-10s %-10s%n", "Name", "Type", "First Line", "Frequency");
        System.out.println("-------------------------------------------------------");
        for (SymbolEntry entry : table.values()) {
            System.out.printf("%-20s %-10s %-10d %-10d%n",
                    entry.name, entry.type, entry.firstLine, entry.frequency);
        }
    }
}
