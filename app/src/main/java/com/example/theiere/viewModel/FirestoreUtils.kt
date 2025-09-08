package com.example.theiere.viewModel

import android.util.Log
import androidx.compose.runtime.Composable
import com.example.theiere.model.EN
import com.example.theiere.model.FR
import com.example.theiere.model.PasswordManager
import com.google.firebase.firestore.FirebaseFirestore
import org.mindrot.jbcrypt.BCrypt
import java.util.UUID

object FirestoreUtils {

    // Variables globales
    var userId: String? = null
    var languageEN: Boolean = false
    var bluetoothOn: Boolean = false

    // Instance de Firestore
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun switchLanguage()
    {
        languageEN = !languageEN
    }

    //fonction de translation EN ou FR
    fun translate(textKey: String): String {
        val enTranslations = EN().translations
        val frTranslations = FR().translations

        // On choisit les traductions en fonction de la langue
        val translations = if (languageEN) enTranslations else frTranslations

        // On récupère la traduction ou un message par défaut
        return translations[textKey] ?: "TRANSLATION KEY NOT FOUND"
    }



    //modifier un utilisateur avec son id
    fun updateUser(
        userId: String,
        newFirstName: String?,
        newLastName: String?,
        newBorn: String?,
        newMdp: String?,
        newTeaPot: String?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userRef = db.collection("users").document(userId)

        val updates = mutableMapOf<String, Any>()

        // On ne met à jour que les champs non nulls
        newFirstName?.let { updates["first"] = it }
        newLastName?.let { updates["last"] = it }
        newBorn?.let { updates["born"] = it }

        // Mettre à jour le mot de passe seulement si on a une nouvelle valeur
        if (!newMdp.isNullOrEmpty()) {
            val passwordManager = PasswordManager()
            updates["mdp"] = passwordManager.hashPassword(newMdp) // Hash du mot de passe
        }

        if (!newMdp.isNullOrEmpty()) {
            newTeaPot?.let { updates["teaPot"] = it }
        }

        if (updates.isNotEmpty()) {
            userRef.update(updates)
                .addOnSuccessListener {
                    Log.d("Firestore", "User updated successfully")
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    Log.w("Firestore", "Error updating user", exception)
                    onFailure(exception)
                }
        } else {
            Log.w("Firestore", "No fields to update")
        }
    }



    // Récupérer un utilisateur par ID
    fun getUserById(userId: String, onSuccess: (User?) -> Unit, onFailure: (Exception) -> Unit) {
        val docRef = db.collection("users").document(userId)

        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val user = User(
                        firstName = document.getString("first") ?: "",
                        lastName = document.getString("last") ?: "",
                        born = document.getString("born") ?: "",
                        hashedPassword = document.getString("mdp") ?: "",
                        teaPot = document.getString("teaPot") ?: "ESP32-BT-Theiere"
                    )
                    Log.d("FirestoreUtils", "User data: $user")
                    onSuccess(user)
                } else {
                    Log.d("FirestoreUtils", "No such document")
                    onSuccess(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.w("FirestoreUtils", "Get failed with ", exception)
                onFailure(exception)
            }
    }

    // Créer un document dans la collection "thes" avec l'ID utilisateur
    fun createThesWithUserId(thesData: Map<String, Any>, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        if (userId == null) {
            Log.w("FirestoreUtils", "User ID is null, cannot create thes.")
            return
        }

        // Ajouter l'ID de l'utilisateur au document
        val thes = hashMapOf(
            "userId" to userId!!, // Utiliser l'ID utilisateur global
            "data" to thesData // Autres données à ajouter
        )

        db.collection("thes")
            .add(thes)
            .addOnSuccessListener { documentReference ->
                Log.d("FirestoreUtils", "Thes document added with ID: ${documentReference.id}")
                onSuccess(documentReference.id)
            }
            .addOnFailureListener { e ->
                Log.w("FirestoreUtils", "Error adding thes document", e)
                onFailure(e)
            }
    }

    // Créer un nouvel utilisateur
    fun createUser(firstName: String, lastName: String, born: String, mdp: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        // Hachage du mot de passe
        val hashedPassword = BCrypt.hashpw(mdp, BCrypt.gensalt())

        // Créez un nouvel utilisateur
        val user = hashMapOf(
            "first" to firstName,
            "last" to lastName,
            "born" to born,
            "mdp" to hashedPassword
        )

        db.collection("users")
            .add(user)
            .addOnSuccessListener { documentReference ->
                Log.d("FirestoreUtils", "DocumentSnapshot added with ID: ${documentReference.id}")
                userId = documentReference.id // Enregistrer l'ID de l'utilisateur créé
                onSuccess(documentReference.id)
            }
            .addOnFailureListener { e ->
                Log.w("FirestoreUtils", "Error adding document", e)
                onFailure(e)
            }
    }

    // Fonction de connexion
    fun login(firstName: String, password: String, callback: (Boolean) -> Unit) {
        val cleanedFirstName = firstName.trim()

        // Accéder à la collection "users" et chercher l'utilisateur avec le prénom
        db.collection("users")
            .whereEqualTo("first", cleanedFirstName)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("FirestoreUtils", "No user found with first name: $cleanedFirstName")
                    userId = null // Aucun utilisateur trouvé
                    callback(false) // Connexion échouée
                } else {
                    // Parcourir les documents retournés
                    for (document in documents) {
                        // Vérifier le mot de passe haché
                        val hashedPassword = document.getString("mdp") ?: ""
                        if (BCrypt.checkpw(password, hashedPassword)) {
                            Log.d("FirestoreUtils", "Login successful for user: $cleanedFirstName")

                            // Si le mot de passe correspond, mettre à jour l'ID de l'utilisateur
                            userId = document.id // L'ID du document (userId) est l'ID unique du document Firestore
                            callback(true) // Connexion réussie
                            return@addOnSuccessListener // Sortir de la boucle dès que la connexion est réussie
                        } else {
                            Log.d("FirestoreUtils", "Password mismatch for user: $cleanedFirstName")
                        }
                    }
                    userId = null // Mot de passe incorrect
                    callback(false) // Connexion échouée
                }
            }
            .addOnFailureListener { exception ->
                Log.w("FirestoreUtils", "Error getting documents: ", exception)
                userId = null // Réinitialiser l'ID utilisateur en cas d'erreur
                callback(false) // Connexion échouée
            }
    }

    // Exemple d'appel pour récupérer un thé par son ID
    fun getTeaById(teaId: String, onSuccess: (Tea?) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("thes")
            .whereEqualTo("TeaId", teaId) // Rechercher par attribut teaId
            // Vous n'avez même pas besoin d'ajouter `whereEqualTo("UserId", userId)`
            // La règle de sécurité Firestore vérifiera automatiquement si `UserId == request.auth.uid`
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0] // Prendre le premier résultat
                    val tea = Tea(
                        teaId = document.getString("TeaId") ?: "",
                        favoris = document.getBoolean("Favoris") ?: false,
                        name = document.getString("Name") ?: "",
                        temperature = document.getDouble("Temperature") ?: 0.0,
                        temps = document.getDouble("Temps") ?: 0.0
                    )
                    Log.d("FirestoreUtils", "Tea data: $tea")
                    onSuccess(tea) // Appeler le callback de succès
                } else {
                    Log.d("FirestoreUtils", "No document found with teaId: $teaId")
                    onSuccess(null) // Aucun document trouvé
                }
            }
            .addOnFailureListener { exception ->
                Log.w("FirestoreUtils", "Error getting documents: ", exception)
                onFailure(exception) // Appeler le callback d'échec
            }
    }

    fun deleteTeaById(teaId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("thes")
            .whereEqualTo("TeaId", teaId) // Rechercher par attribut teaId
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val documentId = querySnapshot.documents[0].id // Obtenir l'ID du document
                    db.collection("thes").document(documentId)
                        .delete()
                        .addOnSuccessListener {
                            Log.d("FirestoreUtils", "Tea with teaId: $teaId deleted successfully!")
                            onSuccess() // Appeler le callback de succès
                        }
                        .addOnFailureListener { exception ->
                            Log.w("FirestoreUtils", "Error deleting tea: ", exception)
                            onFailure(exception) // Appeler le callback d'erreur
                        }
                } else {
                    Log.d("FirestoreUtils", "No document found with teaId: $teaId")
                    onSuccess() // Appeler le callback de succès si aucun document n'est trouvé
                }
            }
            .addOnFailureListener { exception ->
                Log.w("FirestoreUtils", "Error getting documents: ", exception)
                onFailure(exception) // Appeler le callback d'erreur
            }
    }

    fun updateTea(teaId: String, updatedTea: Tea, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        // Rechercher le document par teaId
        db.collection("thes")
            .whereEqualTo("TeaId", teaId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Prendre le premier document trouvé
                    val documentRef = querySnapshot.documents[0].reference

                    // Créer un map pour mettre à jour les champs
                    val updatedData = mapOf(
                        "Favoris" to updatedTea.favoris,
                        "Name" to updatedTea.name,
                        "Temperature" to updatedTea.temperature,
                        "Temps" to updatedTea.temps
                    )

                    // Mettre à jour le document
                    documentRef.update(updatedData)
                        .addOnSuccessListener {
                            Log.d("FirestoreUtils", "Tea successfully updated!")
                            onSuccess() // Appel de la fonction de callback en cas de succès
                        }
                        .addOnFailureListener { exception ->
                            Log.w("FirestoreUtils", "Error updating tea: ", exception)
                            onFailure(exception) // Appel de la fonction de callback en cas d'échec
                        }
                } else {
                    Log.d("FirestoreUtils", "No document found with teaId: $teaId")
                    onFailure(Exception("No document found with teaId: $teaId"))
                }
            }
            .addOnFailureListener { exception ->
                Log.w("FirestoreUtils", "Error finding tea by teaId: ", exception)
                onFailure(exception) // En cas d'échec lors de la recherche
            }
    }

    fun updateTeaWithNameEn(teaName: String, favorite: Boolean) {
        val teaCollection = db.collection("thes")

        // Rechercher le thé par son nom
        teaCollection.whereEqualTo("Name_EN", teaName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                // Si le thé est trouvé
                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        // Mettre à jour le document avec la nouvelle valeur de "favoris"
                        teaCollection.document(document.id)
                            .update("favoris", favorite)
                            .addOnSuccessListener {
                                Log.d("FirestoreUtils", "Tea '$teaName' updated successfully!")
                            }
                            .addOnFailureListener { e ->
                                Log.e("FirestoreUtils", "Error updating tea: ${e.message}")
                            }
                    }
                } else {
                    Log.d("FirestoreUtils", "Tea with name '$teaName' not found.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreUtils", "Error getting documents: ${e.message}")
            }
    }

    fun convertToFahrenheit(temperature: Any?): String {
        return when (temperature) {
            is Number -> {
                val fahrenheit = (temperature.toDouble() * 9 / 5) + 32
                String.format(" / %.1f°F", fahrenheit)
            }
            is String -> {
                temperature.toDoubleOrNull()?.let {
                    val fahrenheit = (it * 9 / 5) + 32
                    String.format(" / %.1f°F", fahrenheit)
                } ?: ""
            }
            else -> {
                ""
            }
        }
    }

    fun generateUID(): Any {
        return UUID.randomUUID().toString()
    }

    fun getValueFromMessage(message: String?, key: String): String? {
        if (message.isNullOrEmpty()){return null}
        val parts = message.split("|") // Séparer les instructions par |
        for (part in parts) {
            val keyValue = part.split(">") // Séparer clé et valeur par >
            if (keyValue.size == 2 && keyValue[0] == key) {
                return keyValue[1] // Retourne la valeur associée à la clé
            }
        }
        return null // Retourne null si la clé n'est pas trouvée
    }


}

data class User(
    val firstName: String,
    val lastName: String?,
    val born: String?,
    val hashedPassword: String,
    val teaPot: String
)

data class Tea(
    val teaId:String,
    val favoris: Boolean,
    val name: String,
    val temperature: Double,
    val temps: Double
)
