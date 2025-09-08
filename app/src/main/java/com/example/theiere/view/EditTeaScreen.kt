package com.example.theiere.view

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.theiere.ui.theme.MainColor
import com.example.theiere.viewModel.FirestoreUtils
import com.example.theiere.viewModel.Tea

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTeaScreen(navController: NavController, teaId: String?) {
    val (teaData, setTeaData) = remember { mutableStateOf<Tea?>(null) }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    // Récupérer les données du thé si teaId n'est pas nul
    LaunchedEffect(teaId) {
        if (!teaId.isNullOrEmpty()) { // Vérifiez si teaId n'est pas vide
            FirestoreUtils.getTeaById(
                teaId = teaId,
                onSuccess = { tea ->
                    if (tea != null) {
                        setTeaData(tea)
                    } else {
                        errorMessage.value = FirestoreUtils.translate("TEA_NOT_FOUND")
                    }
                },
                onFailure = { exception ->
                    errorMessage.value = ": ${exception.message}"
                }
            )
        } else {
            errorMessage.value = FirestoreUtils.translate("INVALID_TEA_ID")
        }
    }

    Column(modifier = Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text(FirestoreUtils.translate("UPDATE_TEA"), color=Color.White) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = FirestoreUtils.translate("BACK"), tint=Color.White)
                }
            },
            actions = {
                if (!teaId.isNullOrEmpty()) {
                    IconButton(onClick = {
                        FirestoreUtils.deleteTeaById(teaId.toString(),
                            onSuccess = {
                                Log.d("FirestoreUtils", "Tea successfully deleted.")
                                navController.navigate("home")
                            },
                            onFailure = { exception ->
                                Log.e("FirestoreUtils", "Error deleting tea: ${exception.message}")
                            }
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Delete, // Utiliser l'icône de suppression
                            contentDescription = "Supprimer le thé",
                            tint = Color.White, // Définir la couleur de l'icône à blanc
                            modifier = Modifier.size(24.dp) // Taille de l'icône
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MainColor)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (errorMessage.value != null) {
                    Text(errorMessage.value ?: "", color = Color.Red)
                } else if (teaData != null) {
                    // Variables d'état pour les TextField
                    val teaIdDisplayed by remember { mutableStateOf(teaData.teaId) }
                    var name by remember { mutableStateOf(teaData.name) }
                    var temperature by remember { mutableStateOf(teaData.temperature.toString()) }
                    var temps by remember { mutableStateOf(teaData.temps.toString()) }
                    var favoris by remember { mutableStateOf(teaData.favoris) }

                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(FirestoreUtils.translate("NAME")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = temperature,
                        onValueChange = { temperature = it },
                        label = { Text("${FirestoreUtils.translate("TEMPERATURE")} (°C)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = temps,
                        onValueChange = { temps = it },
                        label = { Text("${FirestoreUtils.translate("TEMPS")} (minutes)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number
                        )
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(FirestoreUtils.translate("FAVORIS"))
                        Checkbox(
                            checked = favoris,
                            onCheckedChange = { favoris = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val updatedTea = Tea(
                                teaId = teaIdDisplayed,
                                favoris = favoris,
                                name = name,
                                temperature = temperature.toDoubleOrNull() ?: 0.0,
                                temps = temps.toDoubleOrNull() ?: 0.0
                            )
                            FirestoreUtils.updateTea(teaIdDisplayed, updatedTea,
                                onSuccess = {
                                    Log.d("FirestoreUtils", "Tea updated successfully!")
                                    navController.popBackStack()
                                },
                                onFailure = { exception ->
                                    Log.e("FirestoreUtils", "Error updating tea: ${exception.message}")
                                    errorMessage.value = FirestoreUtils.translate("ERROR_UPDATE_TEA")
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(FirestoreUtils.translate("UPDATE"))
                    }

                    Spacer(modifier = Modifier.height(128.dp))

                    Text(FirestoreUtils.translate("CONVERTER"))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Convertisseur de température
                    val fahrenheitValue = remember { mutableStateOf("") }
                    val celsiusValue = remember { mutableStateOf("") }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = fahrenheitValue.value,
                            onValueChange = {
                                fahrenheitValue.value = it
                                celsiusValue.value = if (it.isNotEmpty()) {
                                    ((it.toDouble() - 32) * 5 / 9).toString()
                                } else {
                                    ""
                                }
                            },
                            label = { Text("°F") },
                            modifier = Modifier.weight(1f), // Prendre une proportion de l'espace
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Number
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text("= ")

                        Spacer(modifier = Modifier.width(8.dp))

                        TextField(
                            value = celsiusValue.value,
                            onValueChange = {
                                celsiusValue.value = it
                                fahrenheitValue.value = if (it.isNotEmpty()) {
                                    // Convertir en Fahrenheit
                                    ((it.toDouble() * 9 / 5) + 32).toString()
                                } else {
                                    ""
                                }
                            },
                            label = { Text("°C") },
                            modifier = Modifier.weight(1f), // Prendre une proportion de l'espace
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Number
                            )
                        )
                    }
                } else {
                    CircularProgressIndicator()
                }
            }
        }
    }
}


