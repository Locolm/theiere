package com.example.theiere.view

import android.util.Log
import android.widget.DatePicker
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.theiere.viewModel.FirestoreUtils
import com.example.theiere.ui.theme.MainColor
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen(navController: NavController) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var born by remember { mutableStateOf("") }
    var mdp by remember { mutableStateOf("") }
    var newmdp by remember { mutableStateOf("") }
    var teaPot by remember {mutableStateOf("")}
    var newteaPot by remember {mutableStateOf("")}
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var errorMessageMdp by remember { mutableStateOf<String?>(null) }
    var modifPassword by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val keyboardController = LocalSoftwareKeyboardController.current

    // DatePickerDialog State
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
            born = "$selectedDay/${selectedMonth + 1}/$selectedYear"
        }, year, month, day
    )

    val userId = FirestoreUtils.userId // Récupération de l'ID utilisateur stocké

    // Charger les données utilisateur à partir de Firestore
    LaunchedEffect(userId) {
        if (userId != null) {
            FirestoreUtils.getUserById(
                userId = userId,
                onSuccess = { user ->
                    if (user != null) {
                        firstName = user.firstName
                        lastName = user.lastName.toString()
                        born = user.born.toString()
                        mdp = user.hashedPassword
                        teaPot = user.teaPot
                    } else {
                        errorMessage = FirestoreUtils.translate("USER_NOT_FOUND")
                    }
                    isLoading = false
                },
                onFailure = { exception ->
                    Log.w("Firestore", "Erreur lors de la récupération de l'utilisateur", exception)
                    errorMessage =
                        FirestoreUtils.translate("CANNOT_LOAD_USER")
                    isLoading = false
                }
            )
        } else {
            errorMessage = FirestoreUtils.translate("USER_NOT_FOUND")
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(FirestoreUtils.translate("MODIF_INFO"),color= Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = FirestoreUtils.translate("BACK"), tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        FirestoreUtils.userId=null
                        navController.navigate("connexion")
                    }) {
                        AsyncImage(
                            model = "https://cdn.vectorstock.com/i/500p/16/93/log-out-sign-shut-down-icon-exit-vector-50351693.jpg",
                            contentDescription = "Changer la langue",
                            modifier = Modifier.size(24.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MainColor
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text(FirestoreUtils.translate("FIRSTNAME")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                            }
                        )
                    )

                    TextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text(FirestoreUtils.translate("LASTNAME")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                            }
                        )
                    )

                    // DatePickerDialog
                    TextField(
                        value = born,
                        onValueChange = {}, // handled by date picker dialog
                        label = { Text(FirestoreUtils.translate("BEARTHDAY")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        readOnly = true, // Prevent keyboard input
                        interactionSource = remember { MutableInteractionSource() }
                            .also { interactionSource ->
                                // Handle click interactions to show DatePickerDialog
                                LaunchedEffect(interactionSource) {
                                    interactionSource.interactions.collect { interaction ->
                                        if (interaction is PressInteraction.Release) {
                                            datePickerDialog.show() // Show date picker when field is clicked
                                        }
                                    }
                                }
                            },
                    )

                    TextField(
                        value = teaPot,
                        onValueChange = { teaPot = it },
                        label = { Text(FirestoreUtils.translate("TEAPOT")) },
                        placeholder = { Text("ESP32-BT-Theiere") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                            }
                        )
                    )

                    // Bouton pour activer/désactiver la modification du mot de passe
                    Button(
                        onClick = { modifPassword = !modifPassword },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MainColor)
                    ) {
                        Text(
                            text = if (modifPassword) FirestoreUtils.translate("CANCEL_PASSWORD") else FirestoreUtils.translate("EDIT_PASSWORD"),
                            color = Color.White
                        )
                    }

                    var confirmMdp by remember { mutableStateOf("") }
                    if (modifPassword) {
                        // Champ pour le nouveau mot de passe
                        TextField(
                            value = newmdp,
                            onValueChange = { newmdp = it },
                            label = { Text(FirestoreUtils.translate("NEW_PASSWORD")) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            visualTransformation = PasswordVisualTransformation(), // Masquer le texte
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            )
                        )

                        TextField(
                            value = confirmMdp,
                            onValueChange = { confirmMdp = it },
                            label = { Text(FirestoreUtils.translate("CONFIRM_PASSWORD")) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            visualTransformation = PasswordVisualTransformation(), // Masquer le texte
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (errorMessageMdp != null) {
                        Text(
                            text = errorMessageMdp ?: "",
                            color = Color.Red,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Bouton pour enregistrer les modifications
                    Button(
                        onClick = {
                            if (firstName.isNotEmpty() && newmdp == confirmMdp)
                            {
                                FirestoreUtils.updateUser(
                                    userId = FirestoreUtils.userId ?: "",
                                    newFirstName = firstName,
                                    newLastName = lastName,
                                    newBorn = born,
                                    newMdp = if (modifPassword && newmdp.isNotEmpty()) newmdp else null, // Update the password only if modifPassword is true
                                    newTeaPot = teaPot,
                                    onSuccess = {
                                        Log.d("UserInfoScreen", "Utilisateur mis à jour avec succès.")
                                        navController.popBackStack()
                                    },
                                    onFailure = { exception ->
                                        Log.e("UserInfoScreen", "Erreur lors de la mise à jour de l'utilisateur", exception)
                                        errorMessageMdp = FirestoreUtils.translate("USER_UPDATE_FAIL")
                                    }
                                )
                            }
                            else if (modifPassword)
                            {
                                errorMessageMdp= FirestoreUtils.translate("PASSWORDS_DONT_MATCH")
                            }
                            else if (firstName==""){
                                errorMessageMdp= FirestoreUtils.translate("CANNOT_BE_EMPTY_FIRSTNAME")
                            }

                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MainColor)
                    ) {
                        Text(FirestoreUtils.translate("SAVE"), color = Color.White)
                    }
                }
            }
        }
    }
}