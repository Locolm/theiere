package com.example.theiere.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.theiere.ui.theme.MainBGColor
import com.example.theiere.ui.theme.MainColor
import com.example.theiere.ui.theme.MainColorButton
import com.example.theiere.viewModel.FirestoreUtils
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.example.theiere.model.BluetoothManager
import com.example.theiere.ui.theme.MainColorButton2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendTeaScreen(navController: NavController, teaName: String, temperature: String, time: String) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope() // Obtient le CoroutineScope
    val bluetoothManager = remember { BluetoothManager(context) } // Assurez-vous de le faire
    var temperatureInstruction by remember { mutableIntStateOf(temperature.toDouble().toInt()) }
    var motorInstruction by remember { mutableStateOf("UP") }
    var temperatureActuel by remember { mutableIntStateOf(-100) }
    var timetemp= time.toDouble().toInt()+1 //Temps de descente du moteur
    var timeRemaining by remember { mutableIntStateOf(timetemp) }
    
    var teaReady by remember { mutableStateOf(false) }

    var bluetoothDevice = remember { mutableStateOf<BluetoothDevice?>(null) }
    var infusion = false

    suspend fun sendAndReceiveData() {
        withContext(Dispatchers.IO) {
            try {
                // Envoyer le message
                bluetoothManager.sendMessage(
                    "temp>${temperatureInstruction}|motor>${motorInstruction}"
                )

                // Attendre une réponse
                val receivedMessage = bluetoothManager.receiveMessage()
                if (receivedMessage != null) {
                    temperatureActuel = FirestoreUtils.getValueFromMessage(receivedMessage, "temp")
                        ?.trim()?.toIntOrNull() ?: temperatureActuel
                } else {
                    println("Aucun message reçu.")
                }
            } catch (e: Exception) {
                println("Erreur dans sendAndReceiveData : ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$teaName ${FirestoreUtils.translate("PREPARATION")}", color = Color.White) },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MainColor)
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        // Box pour contenir le contenu principal
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MainBGColor),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (bluetoothDevice.value!= null)
                {
                    LaunchedEffect(Unit){
                        while (true) {
                            // Attendre 1 seconde avant de répéter
                            delay(1000L)

                            // Exécuter l'envoi/recevoir des données
                            coroutineScope.launch {
                                sendAndReceiveData()
                            }
                        }
                    }
                    // température Goal
                    Text(
                        text = "${FirestoreUtils.translate("TEMP_TO_REACH")} : $temperatureInstruction°C ${FirestoreUtils.convertToFahrenheit(temperature)}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    // température Actu
                    Text(
                        text = "${FirestoreUtils.translate("TEMP_ACTU")} : $temperatureActuel°C ${FirestoreUtils.convertToFahrenheit(temperatureActuel)}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if(temperatureActuel>=temperatureInstruction && !infusion)
                    {
                        infusion = true
                        motorInstruction = "DOWN"
                    }
                    if (infusion){
                        //temps
                        val txt = countdownTimer(timeRemaining)
                        if (txt=="00:00"){
                            teaReady = true;
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "${FirestoreUtils.translate("WAITING_TIME")} : $txt",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Bouton d'annulation
                    Button(
                        onClick = {
                            temperatureInstruction = -1000
                            motorInstruction = "UP"
                            navController.navigate("home")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MainColorButton), // Couleur rouge
                        modifier = Modifier.padding(64.dp)
                    ) {
                        Text(text = FirestoreUtils.translate("CANCEL"), color = Color.White)
                    }
                }
                else{

                    // Attente de réponse
                    Text(
                        text = buildAnnotatedString {
                            append(FirestoreUtils.translate("MESSAGE_TIMER1"))
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MainColor)) { // Orange et gras
                                append(FirestoreUtils.translate("MESSAGE_TIMER2"))
                            }
                            append(FirestoreUtils.translate("MESSAGE_TIMER3"))
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = buildAnnotatedString {
                            append(FirestoreUtils.translate("TO_CONNECT1"))
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MainColor)) {
                                append(FirestoreUtils.translate("TO_CONNECT2"))
                            }
                            append(FirestoreUtils.translate("TO_CONNECT3"))
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MainColor)) {
                                append("blutooth")
                            }
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Button(
                        onClick = {
                            temperatureActuel = 0
                            coroutineScope.launch {
                                // Vérifier les permissions Bluetooth
                                if (ActivityCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.BLUETOOTH_CONNECT
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    val connection = bluetoothManager.connectToDevice()
                                    bluetoothDevice.value = connection
                                } else {
                                    println("Permission Bluetooth non accordée")
                                    // Demander la permission Bluetooth si elle n'est pas accordée
                                    ActivityCompat.requestPermissions(
                                        context as Activity,
                                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                                        100
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MainColorButton2),
                        modifier = Modifier.padding(64.dp)
                    ) {
                        Text(text = FirestoreUtils.translate("CONNEXION"), color = Color.White)
                    }
                }
            }
        }
    }
    if (teaReady) {
        temperatureInstruction = -1000
        motorInstruction = "UP"
        AlertDialog(
            onDismissRequest = {
                teaReady = false
                navController.navigate("home")
            },
            title = {
                Text(text = "Info")
            },
            text = {
                Text(FirestoreUtils.translate("TEA_READY"))
            },
            confirmButton = {
                Button(
                    onClick = {
                        teaReady = false
                        navController.navigate("home")
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun countdownTimer(timeInMinutes: Int): String {
    var timeRemaining by remember { mutableIntStateOf(timeInMinutes * 60) } // Convertir en secondes

    LaunchedEffect(key1 = timeRemaining) {
        while (timeRemaining > 0) {
            // Décrémenter le temps toutes les secondes
            delay(1000L)
            timeRemaining -= 1
        }
    }

    // Afficher le temps restant en minutes et secondes
    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60

    return String.format("%02d:%02d", minutes, seconds) // Affichage formaté en mm:ss
}

fun convertMinutesToTimeString(minutes: Int): String {
    val totalSeconds = minutes * 60
    val minutesPart = totalSeconds / 60
    val secondsPart = totalSeconds % 60
    return String.format("%02d:%02d", minutesPart, secondsPart)
}

fun convertTimeStringToMinutes(timeString: String): Int {
    val timeParts = timeString.split(":")
    if (timeParts.size == 2) {
        val minutes = timeParts[0].toIntOrNull() ?: 0
        val seconds = timeParts[1].toIntOrNull() ?: 0
        return minutes + (seconds / 60)
    } else {
        return 0
    }
}

