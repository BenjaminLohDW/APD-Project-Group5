// File: src/main/java/org/example/model/User.java
package org.example.model;

import java.util.concurrent.atomic.AtomicBoolean;

// Using a Java Record to reduce boilerplate for the data class
public record User(String username, String hashedPassword, AtomicBoolean isCracked) {
    public User(String username, String hashedPassword) {
        // Initialize isCracked to false
        this(username, hashedPassword, new AtomicBoolean(false)); 
    }
}