package com.example.theiere.view

import android.util.Log
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.theiere.viewModel.FirestoreUtils
import com.example.theiere.model.PasswordManager
import com.example.theiere.ui.theme.MainColor
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar

private const val TAG = "AddUserScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserScreen(navController: NavController) {
    val db = Firebase.firestore
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var born by remember { mutableStateOf("") }
    var mdp by remember { mutableStateOf("") }
    val passwordManager = PasswordManager()

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

    // Image de fond
    val painter = rememberImagePainter("https://img.leboncoin.fr/api/v1/lbcpb1/images/47/24/d4/4724d4291f7dfc3d2cb63a7a623b622d6e5f5dc0.jpg?rule=ad-large")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(FirestoreUtils.translate("ADD_USER"), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = FirestoreUtils.translate("BACK"), tint = Color.White) // Flèche blanche
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MainColor
                )
            )
        }
    ) { innerPadding ->
        // Utilisation de Box pour centrer le contenu
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Image de fond
            Image(
                painter = painter,
                contentDescription = "Background Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center, // Centrer verticalement
                horizontalAlignment = Alignment.CenterHorizontally // Centrer horizontalement
            ) {
                TextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("${FirestoreUtils.translate("FIRSTNAME")} *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                        }
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text(FirestoreUtils.translate("LASTNAME")) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                        }
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                // DatePickerDialog
                TextField(
                    value = born,
                    onValueChange = {}, // handled by date picker dialog
                    label = { Text(FirestoreUtils.translate("BEARTHDAY")) },
                    modifier = Modifier
                        .fillMaxWidth(),
                    readOnly = true, // Prevent keyboard input
                    interactionSource = remember { MutableInteractionSource() }
                        .also { interactionSource ->
                            LaunchedEffect(interactionSource) {
                                interactionSource.interactions.collect { interaction ->
                                    if (interaction is PressInteraction.Release) {
                                        datePickerDialog.show()
                                    }
                                }
                            }
                        },
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = mdp,
                    onValueChange = { mdp = it },
                    label = { Text("${FirestoreUtils.translate("PASSWORD")} *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onClick@{
                    if (firstName.isBlank() || mdp.isBlank()) {
                        Toast.makeText(context, FirestoreUtils.translate("MUST_FILL"), Toast.LENGTH_SHORT).show()
                        return@onClick // Ne pas continuer si les champs obligatoires ne sont pas remplis
                    }
                    // Créez un nouvel utilisateur
                    val user = hashMapOf(
                        "first" to firstName,
                        "last" to lastName,
                        "born" to born,
                        "mdp" to passwordManager.hashPassword(mdp),
                    )

                    db.collection("users")
                        .add(user)
                        .addOnSuccessListener { documentReference ->
                            Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                            navController.popBackStack() // Retour à l'écran d'accueil après l'ajout
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error adding document", e)
                        }
                }) {
                    Text(FirestoreUtils.translate("ADD_USER"))
                }
            }
        }
    }
}