// File: src/main/java/org/example/io/OutputWriter.java
package org.example.io;

import org.example.model.CrackedCredential;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class OutputWriter {
    public static void writeCrackedPasswordsToCSV(String filePath, ConcurrentLinkedQueue<CrackedCredential> crackedQueue) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("user_name,hashed_password,plain_password\n");

            for (CrackedCredential credential : crackedQueue) {
                String line = String.format("%s,%s,%s\n",
                        credential.username(),
                        credential.hashedPassword(),
                        credential.plainPassword());
                writer.write(line);
            }
            System.out.println("\nCracked password details have been written to " + filePath);
        } catch (IOException e) {
            System.err.println("Error: Could not write to CSV file: " + e.getMessage());
        }
    }
}