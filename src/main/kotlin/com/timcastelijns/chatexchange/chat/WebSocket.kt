package com.timcastelijns.chatexchange.chat

import okhttp3.*
import okhttp3.WebSocket
import java.io.IOException

class WebSocket(
        private val hostUrlBase: String
) : AutoCloseable {

    private val client: OkHttpClient = OkHttpClient.Builder().build()

    private var webSocket: WebSocket? = null

    var chatEventListener: ((String) -> Unit)? = null

    fun open(webSocketUrl: String) {
        val request = Request.Builder()
            .url(webSocketUrl)
            .header("Origin", hostUrlBase)
            .build()

        webSocket = client.newWebSocket(request, object: WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                handleChatEvent(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                throw ChatOperationException("Cannot connect to chat websocket", t)
            }
        })
    }

    fun handleChatEvent(json: String) {
        chatEventListener?.invoke(json)
    }

    override fun close() {
        try {
            webSocket?.close(1000, "Client closing")
            client.dispatcher.executorService.shutdown()
        } catch (e: IOException) {

        }
    }
}
