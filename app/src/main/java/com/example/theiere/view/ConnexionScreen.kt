package com.example.theiere.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.theiere.viewModel.FirestoreUtils

@Composable
fun ConnexionScreen(navController: NavController) {
    // État pour les champs de texte et le message d'erreur
    var firstName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // Image de fond
    val painter = rememberImagePainter("https://media.tiffany.com/is/image/tco/HA-BG-Hero-8-Mobile-3")

    // État pour l'animation
    val offsetXB = remember { Animatable(500f) } // Commence hors de l'écran à droite
    val offsetXW = remember { Animatable(-500f) } // Commence hors de l'écran à gauche

    val keyboardController = LocalSoftwareKeyboardController.current

    //Animations
    LaunchedEffect(Unit) {
        offsetXW.animateTo(0f, animationSpec = tween(durationMillis = 1000))
    }
    LaunchedEffect(Unit) {
        offsetXB.animateTo(2f, animationSpec = tween(durationMillis = 1000))
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Image de fond
        Image(
            painter = painter,
            contentDescription = "Background Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Contenu principal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // Titre avec style
            Box(
                modifier = Modifier
                    .padding(bottom = 110.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                // Texte noir décalé
                Text(
                    text = FirestoreUtils.translate("APP_NAME"),
                    color = Color.Black,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .offset(x = offsetXB.value.dp, y = 2.dp) // Décalage léger
                )

                // Texte blanc au-dessus
                Text(
                    text = FirestoreUtils.translate("APP_NAME"),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .offset(x = offsetXW.value.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            TextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text(FirestoreUtils.translate("USERNAME")) },
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
                value = password,
                onValueChange = { password = it },
                label = { Text(FirestoreUtils.translate("PASSWORD")) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(), // Pour cacher le mot de passe
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // Appeler la fonction de connexion
                    FirestoreUtils.login(firstName, password) { isSuccess ->
                        if (isSuccess) {
                            navController.navigate("home") // Rediriger vers l'écran d'accueil
                        } else {
                            errorMessage = FirestoreUtils.translate("USER_OR_PASSWORD_INCORRECT")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(FirestoreUtils.translate("CONNEXION"))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Affichage du message d'erreur
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { navController.navigate("addUser") }) {
                Text(FirestoreUtils.translate("CREATE_USER"))
            }
        }
    }
}