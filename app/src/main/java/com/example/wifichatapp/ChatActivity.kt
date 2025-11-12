package com.example.wifichatapp

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.*
import java.net.Socket

class ChatActivity : AppCompatActivity() {

    private lateinit var chatRecycler: RecyclerView
    private lateinit var txtTitle: TextView
    private lateinit var editMsg: EditText
    private lateinit var btnSend: Button

    private lateinit var chatAdapter: ChatAdapter
    private val chatList = mutableListOf<ChatMessage>()

    companion object {
        private var socket: Socket? = null
        fun setSocket(sock: Socket) {
            socket = sock
        }
    }

    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private var listenJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatRecycler = findViewById(R.id.chatRecycler)
        txtTitle = findViewById(R.id.txtTitle)
        editMsg = findViewById(R.id.editMsg)
        btnSend = findViewById(R.id.btnSend)

        chatAdapter = ChatAdapter(chatList)
        chatRecycler.layoutManager = LinearLayoutManager(this)
        chatRecycler.adapter = chatAdapter

        val deviceName = intent.getStringExtra("deviceName") ?: "Unknown"
        txtTitle.text = "Chat with $deviceName"

        try {
            reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
            writer = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream()))
        } catch (e: IOException) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        btnSend.setOnClickListener {
            val msg = editMsg.text.toString().trim()
            if (msg.isNotEmpty()) {
                sendMessage(msg)
            }
        }

        listenForMessages()
    }

    private fun sendMessage(msg: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                writer?.write(msg)
                writer?.newLine()
                writer?.flush()
                withContext(Dispatchers.Main) {
                    addMessage(msg, true)
                    editMsg.text.clear()
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "Send failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun listenForMessages() {
        listenJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                var line: String?
                while (true) {
                    line = reader?.readLine() ?: break
                    withContext(Dispatchers.Main) {
                        addMessage(line, false)
                    }
                }
            } catch (_: IOException) {
            }
        }
    }

    private fun addMessage(message: String, isSent: Boolean) {
        chatList.add(ChatMessage(message, isSent))
        chatAdapter.notifyItemInserted(chatList.size - 1)
        chatRecycler.scrollToPosition(chatList.size - 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        listenJob?.cancel()
        try {
            reader?.close()
            writer?.close()
            socket?.close()
        } catch (e: Exception) {
        }
    }
}
