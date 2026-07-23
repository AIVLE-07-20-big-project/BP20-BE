package com.bp20.backend.api.iam.admin.bootstrap;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;

public final class PasswordHashGeneratorCli {

    private PasswordHashGeneratorCli() {
    }

    public static void main(String[] args) throws Exception {
        String password = readPasswordTwice();
        if (password.length() < 12 || password.length() > 72) {
            throw new IllegalArgumentException("The password must be 12 to 72 characters.");
        }

        String passwordHash = new BCryptPasswordEncoder().encode(password);
        System.out.println("BCrypt hash: " + passwordHash);
    }

    private static String readPasswordTwice() throws Exception {
        Console console = System.console();
        String password;
        String confirmation;

        if (console != null) {
            password = new String(console.readPassword("Password to hash: "));
            confirmation = new String(console.readPassword("Confirm password: "));
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Password to hash (input may be visible): ");
            password = reader.readLine();
            System.out.print("Confirm password: ");
            confirmation = reader.readLine();
        }

        if (password == null || !password.equals(confirmation)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        return password;
    }
}
