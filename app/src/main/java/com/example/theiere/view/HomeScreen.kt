package com.example.theiere.view

import android.util.Log
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.theiere.model.BluetoothManager
import com.example.theiere.viewModel.FirestoreUtils
import com.example.theiere.ui.theme.MainColor
import com.example.theiere.ui.theme.MainColorAdmin
import com.example.theiere.viewModel.Tea
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.compose.ui.platform.LocalContext

private const val TAG = "HomeScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val db = Firebase.firestore
    val (userData, setUserData) = remember { mutableStateOf<List<Map<String, Any>>?>(null) }
    val errorMessage = remember { mutableStateOf<String?>(null) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    val userId = FirestoreUtils.userId
    var searchQuery by remember { mutableStateOf("") }
    val bluetoothManager = BluetoothManager(LocalContext.current)

    // Charger les données utilisateur
    if (userId != null) {
        FirestoreUtils.getUserById(
            userId = userId,
            onSuccess = { user ->
                if (user != null) {
                    firstName = user.firstName
                    lastName = user.lastName.toString()
                    bluetoothManager.updateDeviceName(user.teaPot)
                } else {
                    errorMessage.value = FirestoreUtils.translate("USER_NOT_FOUND")
                    firstName = FirestoreUtils.translate("UNKNOWN")
                }
            },
            onFailure = { exception ->
                Log.w("Firestore", "Erreur lors de la récupération de l'utilisateur", exception)
                firstName = FirestoreUtils.translate("UNKNOWN")
            }
        )
    } else {
        errorMessage.value = FirestoreUtils.translate("USER_NOT_FOUND")
        firstName = FirestoreUtils.translate("UNKNOWN")
    }

    // Charger les thés depuis la base de données
    LaunchedEffect(Unit) {
        db.collection("thes")
            .get()
            .addOnSuccessListener { result ->
                val data = result.map { document -> document.data }
                setUserData(data)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Erreur récupération liste thés.", exception)
                errorMessage.value = FirestoreUtils.translate("ERROR_LOADING_TEA")
            }
    }

    // Logique de filtrage des données
    val filteredData = userData?.filter { tea ->

        val teaName = if (tea["UserId"] == "ADMIN") {
            if (FirestoreUtils.languageEN) {
                tea["Name_EN"]?.toString() ?: ""
            } else {
                tea["Name_FR"]?.toString() ?: ""
            }
        } else {
            tea["Name"]?.toString() ?: ""
        }

        val matchesSearchQuery = teaName.contains(searchQuery, ignoreCase = true)

        when (selectedFilter) {
            "Favoris" -> tea["Favoris"] == true && tea["UserId"] == userId && matchesSearchQuery
            "Default" -> tea["UserId"] == "ADMIN" && matchesSearchQuery
            else -> (tea["UserId"] == "ADMIN" || tea["UserId"] == userId) && matchesSearchQuery
        }

    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("$firstName $lastName", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigate("userInfo") }) {
                            Icon(Icons.Filled.Person, contentDescription = "Utilisateur", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            FirestoreUtils.switchLanguage()
                            navController.navigate("home") // rafraichir la page
                        }) {
                            AsyncImage(
                                model = if (!FirestoreUtils.languageEN) {
                                    "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3f/Ensign_of_France.svg/langfr-225px-Ensign_of_France.svg.png"
                                } else {
                                    "https://upload.wikimedia.org/wikipedia/commons/thumb/8/83/Flag_of_the_United_Kingdom_%283-5%29.svg/langfr-225px-Flag_of_the_United_Kingdom_%283-5%29.svg.png"
                                },
                                contentDescription = "Changer la langue",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MainColor)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { selectedFilter = "All" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedFilter == "All") MainColor else Color.Gray
                        )
                    ) {
                        Text(text = FirestoreUtils.translate("ALL"))
                    }
                    Button(
                        onClick = { selectedFilter = "Favoris" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedFilter == "Favoris") MainColor else Color.Gray
                        )
                    ) {
                        Text(text = FirestoreUtils.translate("FAVORIS"))
                    }
                    Button(
                        onClick = { selectedFilter = "Default" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedFilter == "Default") MainColor else Color.Gray
                        )
                    ) {
                        Text(text = FirestoreUtils.translate("DEFAULT"))
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("addTea") }) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter un thé")
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(FirestoreUtils.translate("FILTER_BY_NAME")) }, // Placeholder pour la barre de recherche
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
                Text(
                    text = FirestoreUtils.translate("TEA_DATA"),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                when {
                    userData == null && errorMessage.value == null -> {
                        CircularProgressIndicator()
                    }

                    errorMessage.value != null -> {
                        Text("${FirestoreUtils.translate("ERROR")}: ${errorMessage.value}")
                    }

                    filteredData != null -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(filteredData) { tea ->
                                val isAdmin = tea["UserId"] == "ADMIN"
                                val isFavorite = tea["Favoris"] == true
                                val cardColor = if (isAdmin) MainColorAdmin else MainColor

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                        .clickable(onClick = {
                                            val temperatureValue = tea["Temperature"]?.toString() ?: "0"
                                            val tempsValue = tea["Temps"]?.toString() ?: "0"
                                            val nameValue =
                                                    if (tea.containsKey("Name_EN")){
                                                        if (FirestoreUtils.languageEN) {
                                                            tea["Name_EN"]?.toString() ?: FirestoreUtils.translate("TEA")
                                                        } else {
                                                            tea["Name_FR"]?.toString() ?: FirestoreUtils.translate("TEA")
                                                        }
                                                    }
                                                    else{
                                                        tea["Name"]?.toString() ?: FirestoreUtils.translate("TEA")
                                                    }

                                            navController.navigate("SendTea/$nameValue/$temperatureValue/$tempsValue")
                                        }),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(6.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardColor)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                    ){
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.Start
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (isFavorite) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = "Favori",
                                                        tint = Color.Yellow,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                }

                                                Text("${
                                                    if (FirestoreUtils.languageEN) {
                                                        if (tea.containsKey("Name_EN")) {
                                                            tea["Name_EN"]
                                                        } else {
                                                            tea["Name"]
                                                        }
                                                    } else {
                                                        if (tea.containsKey("Name_FR")) {
                                                            tea["Name_FR"]
                                                        } else {
                                                            tea["Name"]
                                                        }
                                                    }
                                                }", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("${FirestoreUtils.translate("TEMPERATURE")} : ${tea["Temperature"]}°C${FirestoreUtils.convertToFahrenheit(tea["Temperature"])}", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("${FirestoreUtils.translate("TEMPS")} : ${tea["Temps"]} minutes", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                        }
                                        if(tea.containsKey("UserId") && tea["UserId"] != "ADMIN") {
                                            IconButton(
                                                modifier = Modifier.padding(start = 32.dp),
                                                onClick = {
                                                    val teaId = tea["TeaId"] as? String ?: ""
                                                    navController.navigate("editTea/$teaId")
                                                          },
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Settings,
                                                    contentDescription = "Edit",
                                                    tint = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(64.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}


