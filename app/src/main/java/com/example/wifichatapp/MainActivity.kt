package com.example.wifichatapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.*
import java.net.*

class MainActivity : AppCompatActivity() {

    private lateinit var txtStatus: TextView
    private lateinit var btnHost: Button
    private lateinit var btnConnect: Button
    private lateinit var edtIP: EditText
    private lateinit var txtIPDisplay: TextView

    private var serverSocket: ServerSocket? = null
    private var socket: Socket? = null
    private val PORT = 8888

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtStatus = findViewById(R.id.txtStatus)
        btnHost = findViewById(R.id.btnHost)
        btnConnect = findViewById(R.id.btnConnect)
        edtIP = findViewById(R.id.edtIP)
        txtIPDisplay = findViewById(R.id.txtIPDisplay)

        txtIPDisplay.text = "Your IP: ${getLocalIpAddress()}"

        btnHost.setOnClickListener {
            startServer()
        }

        btnConnect.setOnClickListener {
            val ip = edtIP.text.toString().trim()
            if (ip.isEmpty()) {
                Toast.makeText(this, "Enter Server IP", Toast.LENGTH_SHORT).show()
            } else {
                connectToServer(ip)
            }
        }
    }

    private fun startServer() {
        txtStatus.text = "Starting server..."
        CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(PORT)
                runOnUiThread { txtStatus.text = "Waiting for client..." }

                socket = serverSocket!!.accept()
                runOnUiThread {
                    txtStatus.text = "Client connected: ${socket!!.inetAddress.hostAddress}"
                }

                val intent = Intent(this@MainActivity, ChatActivity::class.java)
                ChatActivity.setSocket(socket!!)
                intent.putExtra("deviceName", socket!!.inetAddress.hostAddress)
                runOnUiThread {
                    startActivity(intent)
                }

            } catch (e: IOException) {
                runOnUiThread {
                    txtStatus.text = "Server error: ${e.message}"
                }
            }
        }
    }

    private fun connectToServer(ip: String) {
        txtStatus.text = "Connecting to server..."
        CoroutineScope(Dispatchers.IO).launch {
            try {
                socket = Socket(ip, PORT)
                runOnUiThread {
                    txtStatus.text = "Connected to $ip"
                }
                val intent = Intent(this@MainActivity, ChatActivity::class.java)
                ChatActivity.setSocket(socket!!)
                intent.putExtra("deviceName", ip)
                runOnUiThread {
                    startActivity(intent)
                }
            } catch (e: IOException) {
                runOnUiThread {
                    txtStatus.text = "Connection failed: ${e.message}"
                }
            }
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.hostAddress ?: "Unavailable"
                    }
                }
            }
        } catch (ex: Exception) {
        }
        return "Unavailable"
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            socket?.close()
            serverSocket?.close()
        } catch (e: Exception) {
        }
    }
}
