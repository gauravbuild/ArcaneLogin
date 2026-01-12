package me.gauravbuilds.arcanelogin.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {

    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public static boolean checkPassword(String candidate, String hashed) {
        if (hashed == null || candidate == null) return false;
        try {
            return BCrypt.checkpw(candidate, hashed);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
