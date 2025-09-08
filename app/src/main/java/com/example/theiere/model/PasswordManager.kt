package com.example.theiere.model

import org.mindrot.jbcrypt.BCrypt

class PasswordManager {

    // Hachage du mot de passe
    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    // Vérification du mot de passe
    fun checkPassword(password: String, hashed: String): Boolean {
        return BCrypt.checkpw(password, hashed)
    }
}