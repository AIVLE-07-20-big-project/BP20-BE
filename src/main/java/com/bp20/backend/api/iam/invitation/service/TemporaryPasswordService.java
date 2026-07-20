package com.bp20.backend.api.iam.invitation.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

@Service
public class TemporaryPasswordService {

    private static final String UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL = "!@#$%";
    private static final String ALL = UPPERCASE + LOWERCASE + DIGITS + SPECIAL;
    private static final int PASSWORD_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generate() {
        List<Character> characters = new ArrayList<>(PASSWORD_LENGTH);
        characters.add(randomCharacter(UPPERCASE));
        characters.add(randomCharacter(LOWERCASE));
        characters.add(randomCharacter(DIGITS));
        characters.add(randomCharacter(SPECIAL));

        while (characters.size() < PASSWORD_LENGTH) {
            characters.add(randomCharacter(ALL));
        }
        Collections.shuffle(characters, SECURE_RANDOM);

        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        characters.forEach(password::append);
        return password.toString();
    }

    public String hash(String temporaryPassword) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(temporaryPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }

    private char randomCharacter(String source) {
        return source.charAt(SECURE_RANDOM.nextInt(source.length()));
    }
}
