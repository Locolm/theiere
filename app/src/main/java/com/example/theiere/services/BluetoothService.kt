import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.util.UUID

class BluetoothService : Service() {

    private val binder = BluetoothBinder()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _temperatureFlow = MutableStateFlow(-1000) // Valeur initiale
    val temperatureFlow: StateFlow<Int> get() = _temperatureFlow

    private var bluetoothSocket: BluetoothSocket? = null // Ton socket Bluetooth ici
    private val bluetoothDeviceUUID: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    inner class BluetoothBinder : Binder() {
        fun getService(): BluetoothService = this@BluetoothService
    }

    override fun onCreate() {
        super.onCreate()
        connectToBluetoothDevice() // Connexion au périphérique Bluetooth
        startReceiving() // Commencer à recevoir des messages
    }

    private fun connectToBluetoothDevice() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e("BluetoothService", "Bluetooth non disponible ou désactivé.")
            return
        }

        // Chercher le périphérique par nom
        val device = bluetoothAdapter.bondedDevices.find { it.name == "ESP32-BT-Theiere" }
        if (device == null) {
            Log.e("BluetoothService", "Périphérique ESP32-BT-Theiere introuvable.")
            return
        }

        try {
            // Vérifier les permissions Bluetooth
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e("BluetoothService", "Permission BLUETOOTH_CONNECT non accordée.")
                // déclencher demande de permissions ici (pour le moment non)
                return
            }
            // UUID pour le profil série SPP
            bluetoothSocket = device.createRfcommSocketToServiceRecord(bluetoothDeviceUUID)

            // Établir la connexion
            bluetoothSocket?.connect()
            Log.i("BluetoothService", "Connexion Bluetooth établie avec succès.")
        } catch (e: IOException) {
            Log.e("BluetoothService", "Erreur lors de la connexion Bluetooth : ${e.message}")
            try {
                bluetoothSocket?.close()
            } catch (closeException: IOException) {
                Log.e("BluetoothService", "Erreur lors de la fermeture du socket : ${closeException.message}")
            }
        }
    }


    private fun startReceiving() {
        scope.launch {
            while (true) {
                val message = receiveMessage()
                val newTemperature = parseTemperature(message)
                _temperatureFlow.value = newTemperature
            }
        }
    }

    private fun receiveMessage(): String? {
        return try {
            val inputStream: InputStream = bluetoothSocket?.inputStream ?: return null
            val buffer = ByteArray(1024)
            val receivedMessage = StringBuilder()

            var bytesRead: Int
            bytesRead = inputStream.read(buffer)
            val part = String(buffer, 0, bytesRead)
            receivedMessage.append(part)

            if (receivedMessage.contains("\n")) {
                return receivedMessage.toString().trim()
            }
            null
        } catch (e: IOException) {
            Log.e("BluetoothService", "Erreur lors de la réception des données : ${e.message}")
            null
        }
    }

    private fun parseTemperature(message: String?): Int {
        // Implemente la logique pour parser la température depuis le message reçu
        return message?.toIntOrNull() ?: -1000
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothSocket?.close() // Fermer la connexion Bluetooth
    }
}
