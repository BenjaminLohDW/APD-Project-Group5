// File: src/main/java/org/example/model/CrackedCredential.java
package org.example.model;

// Simple immutable record for a successfully cracked password
public record CrackedCredential(String username, String hashedPassword, String plainPassword) {}