package com.bp20.backend.global.security.password;

import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 72;

    public void validate(String password) {
        if (!isValid(password)) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_PASSWORD);
        }
    }

    public boolean isValid(String password) {
        if (password == null || password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            return false;
        }
        if (password.chars().anyMatch(Character::isWhitespace) || hasTripleRepeatedCharacter(password)) {
            return false;
        }

        int characterGroups = 0;
        characterGroups += password.chars().anyMatch(Character::isUpperCase) ? 1 : 0;
        characterGroups += password.chars().anyMatch(Character::isLowerCase) ? 1 : 0;
        characterGroups += password.chars().anyMatch(Character::isDigit) ? 1 : 0;
        characterGroups += password.chars().anyMatch(value -> !Character.isLetterOrDigit(value)) ? 1 : 0;
        return characterGroups >= 3;
    }

    private boolean hasTripleRepeatedCharacter(String password) {
        for (int index = 2; index < password.length(); index++) {
            if (password.charAt(index) == password.charAt(index - 1)
                    && password.charAt(index) == password.charAt(index - 2)) {
                return true;
            }
        }
        return false;
    }
}
