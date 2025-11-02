// File: src/main/java/org/example/io/HashManager.java
package org.example.io;

import org.example.model.User;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HashManager {

    // Using Map in the signature for flexibility instead of concrete HashMap
    // Using Streams API to load and transform data concisely 
    public Map<String, User> loadUsers(String filename) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filename));
        return lines.stream()
                .map(line -> line.split(","))
                .filter(parts -> parts.length >= 2)
                .collect(Collectors.toConcurrentMap(
                        parts -> parts[0], 
                        parts -> new User(parts[0], parts[1].trim())
                ));
    }
}