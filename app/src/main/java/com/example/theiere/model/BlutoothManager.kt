package com.example.theiere.model

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.example.theiere.viewModel.FirestoreUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothManager(private val context: Context) {

    private var ESP32_DEVICE_NAME = "ESP32-BT-Theiere"  // Nom de l'appareil ESP32
    private val ESP32_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")  // UUID standard pour SPP

    private var bluetoothSocket: BluetoothSocket? = null

    // Fonction pour mettre à jour le device
    fun updateDeviceName(newName: String) {
        if (newName.isNotBlank()) {
            ESP32_DEVICE_NAME = newName
            println("Le nom de l'appareil a été mis à jour : $ESP32_DEVICE_NAME")
        } else {
            println("Le nouveau nom de teapot ne peut pas être vide.")
        }
    }

    // Fonction pour se connecter à l'appareil Bluetooth
    fun connectToDevice(): BluetoothDevice? {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            println("Bluetooth n'est pas activé ou non disponible sur cet appareil")
            return null
        }

        // Vérifier les permissions pour le Bluetooth
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            println("Permission BLUETOOTH_CONNECT manquante")
            return null
        }

        // Obtenir la liste des appareils appairés
        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        var theiereDevice: BluetoothDevice? = null

        // Chercher l'appareil ESP32 "ESP32-BT-Theiere"
        for (device in pairedDevices) {
            if (device.name == ESP32_DEVICE_NAME) {
                theiereDevice = device
                println("Appareil trouvé : ${device.name} - ${device.address}")
                break
            }
        }

        if (theiereDevice == null) {
            println("Aucun appareil ESP32-BT-Theiere trouvé parmi les appareils appairés.")
            return null
        }

        // Se connecter à la théière
        try {
            bluetoothSocket = theiereDevice.createRfcommSocketToServiceRecord(ESP32_UUID)
            bluetoothSocket?.connect()
            println("Connexion établie avec succès à ${theiereDevice.name}.")
            return theiereDevice
        } catch (e: IOException) {
            e.printStackTrace()
            println("Erreur lors de la connexion à l'appareil: ${e.message}")
            return null
        }
    }

    // Fonction pour envoyer un message à l'appareil Bluetooth
    fun sendMessage(message: String) {
        try {
            val outputStream: OutputStream = bluetoothSocket?.outputStream ?: return
            outputStream.write(message.toByteArray())
            outputStream.flush()
            println("Message envoyé : $message")
        } catch (e: IOException) {
            e.printStackTrace()
            println("Erreur lors de l'envoi du message : ${e.message}")
        }
    }

    suspend fun receiveMessage(): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = bluetoothSocket?.inputStream ?: return@withContext null
            val buffer = ByteArray(1024)
            val receivedMessage = StringBuilder()

            while (true) {
                if (bluetoothSocket?.isConnected != true) break // Vérifiez si le socket est encore valide

                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break // Fin de la connexion ou erreur

                val part = String(buffer, 0, bytesRead)
                receivedMessage.append(part)

                if (receivedMessage.contains("\n")) break // Message complet
            }
            println("Message reçu : " + receivedMessage.toString().trim())
            receivedMessage.toString().trim()
        } catch (e: IOException) {
            println("Erreur lors de la réception des données : ${e.message}")
            null
        }
    }

    // Fonction pour fermer la connexion Bluetooth
    fun closeConnection() {
        try {
            bluetoothSocket?.close()
            println("Connexion Bluetooth fermée.")
        } catch (e: IOException) {
            e.printStackTrace()
            println("Erreur lors de la fermeture de la connexion : ${e.message}")
        }
    }
}
