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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.theiere.viewModel.FirestoreUtils
import com.example.theiere.ui.theme.MainColor
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

private const val TAG = "AddTeaScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTeaScreen(navController: NavController) {
    val db = Firebase.firestore
    var teaId by remember { mutableStateOf(FirestoreUtils.generateUID()) }
    var theName by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }
    var isFavorite by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val userId = FirestoreUtils.userId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(FirestoreUtils.translate("ADD_TEA"), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = FirestoreUtils.translate("BACK"), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MainColor)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    value = theName,
                    onValueChange = { theName = it },
                    label = { Text(FirestoreUtils.translate("TEA_NAME")) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = temperature,
                    onValueChange = { temperature = it },
                    label = { Text(FirestoreUtils.translate("TEMPERATURE")) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text(FirestoreUtils.translate("TIME")) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Checkbox(
                        checked = isFavorite,
                        onCheckedChange = { isFavorite = it },
                        colors = CheckboxDefaults.colors(checkedColor = MainColor)
                    )
                    Text("Ajouter aux favoris")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    if (theName.isBlank() || temperature.isBlank() || time.isBlank()) {//
                        errorMessage = FirestoreUtils.translate("MUST_FILL")
                    } else {
                        val teaConfig = hashMapOf(
                            "TeaId" to teaId,
                            "Name" to theName,
                            "Temperature" to temperature.toIntOrNull(),
                            "Temps" to time.toIntOrNull(),
                            "Favoris" to isFavorite,
                            "UserId" to FirestoreUtils.userId,

                        )

                        db.collection("thes")
                            .add(teaConfig)
                            .addOnSuccessListener {
                                navController.popBackStack()
                            }
                            .addOnFailureListener { e ->
                                errorMessage = FirestoreUtils.translate("ERROR_ADDING_TEA")
                                Log.w(TAG, "Error adding document", e)
                            }
                    }
                }) {
                    Text(FirestoreUtils.translate("ADD_TEA"))
                }
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}
