package com.example.theiere.view

import BluetoothService
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.*
import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi

class MainActivity : ComponentActivity() {
    private val PERMISSION_REQUEST_CODE = 100
    private var bluetoothService: BluetoothService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as BluetoothService.BluetoothBinder
            bluetoothService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            bluetoothService = null
            isBound = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TheiereApp()
        }

        // Vérifiez si les permissions Bluetooth sont déjà accordées
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions()
        } else {
            startBluetoothService()
        }
    }

    private fun startBluetoothService() {
        Intent(this, BluetoothService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestBluetoothPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSION_REQUEST_CODE)
    }

    override fun onStart() {
        super.onStart()
        if (!isBound) {
            startBluetoothService()
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}

@Composable
fun TheiereApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "connexion") {
        composable("connexion"){ConnexionScreen(navController)}
        composable("home") { HomeScreen(navController) }
        composable("addUser") { AddUserScreen(navController) }
        composable("userInfo") { UserInfoScreen(navController) }
        composable("addTea") { AddTeaScreen(navController) }
        composable("editTea/{teaId}") { backStackEntry ->
            val teaId = backStackEntry.arguments?.getString("teaId")
            EditTeaScreen(navController, teaId)
        }
        // Définir la route dans votre NavHost
        composable("SendTea/{name}/{temperature}/{time}") { backStackEntry ->
            val temperature = backStackEntry.arguments?.getString("temperature")
            val time = backStackEntry.arguments?.getString("time")
            val name = backStackEntry.arguments?.getString("name")
            // Passez ces paramètres à votre écran SendThe
            if (name != null && temperature != null && time != null) {
                SendTeaScreen(navController,name, temperature, time)
            }
        }
    }
}

@Composable
fun PermissionDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permissions Required") },
        text = { Text("This app requires Bluetooth and Location permissions to function properly.") },
        confirmButton = {
            Button(
                onClick = {
                    // Redemander les permissions
                    // Vous devrez également gérer les résultats dans onRequestPermissionsResult
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        }
    )
}