package com.timcastelijns.chatexchange.chat

import com.nhaarman.mockitokotlin2.*
import org.glassfish.tyrus.client.ClientManager
import org.glassfish.tyrus.client.ClientProperties
import org.junit.Ignore
import org.junit.Test
import java.net.URI
import javax.websocket.Endpoint
import javax.websocket.Session

class WebSocketTest {

    private val clientManager = mock<ClientManager> {
        on { connectToServer(any<Endpoint>(), any(), any()) } doReturn mock<Session>()
    }

    @Ignore
    @Test
    fun `retry is enabled`() {
        WebSocket("", {}, clientManager)
        verify(clientManager).properties[ClientProperties.RETRY_AFTER_SERVICE_UNAVAILABLE] = true
    }

    @Test
    fun `open connects to server`() {
        val webSocketUrl = "ws://socket"
        val webSocket = WebSocket("", {}, clientManager)
        webSocket.open(webSocketUrl)

        verify(clientManager).connectToServer(any<Endpoint>(), any(), eq(URI(webSocketUrl)))
    }

}
