package com.timcastelijns.chatexchange.chat

import org.glassfish.tyrus.client.ClientManager
import org.glassfish.tyrus.client.ClientProperties
import org.glassfish.tyrus.container.jdk.client.JdkClientContainer
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import javax.websocket.*

internal class WebSocket(
        private val hostUrlBase: String,
        private val chatEventListener: ((String) -> Unit),
        private val clientManager: ClientManager = ClientManager.createClient(JdkClientContainer::class.java.name)
) : AutoCloseable {

    private val configBuilder = ClientEndpointConfig.Builder.create()

    private lateinit var webSocketSession: Session

    init {
        configBuilder.configurator(object : ClientEndpointConfig.Configurator() {
            override fun beforeRequest(headers: MutableMap<String, MutableList<String>>?) {
                val list = mutableListOf<String>()
                list.add(hostUrlBase)
                headers?.put("Origin", list)
            }
        })

        clientManager.properties[ClientProperties.RETRY_AFTER_SERVICE_UNAVAILABLE] = true
    }

    fun open(webSocketUrl: String) {
        try {
            webSocketSession = clientManager.connectToServer(object : Endpoint() {
                override fun onOpen(session: Session?, config: EndpointConfig?) {
                    session?.addMessageHandler(String::class.java, ::handleChatEvent)
                }

                override fun onError(session: Session?, thr: Throwable?) {

                }
            }, configBuilder.build(), URI(webSocketUrl))
        } catch (e: DeploymentException) {
            throw ChatOperationException("Cannot connect to chat websocket", e)
        } catch (e: URISyntaxException) {
            throw ChatOperationException("Cannot connect to chat websocket", e)
        } catch (e: IOException) {
            throw ChatOperationException("Cannot connect to chat websocket", e)
        }
    }

    private fun handleChatEvent(json: String) {
        chatEventListener.invoke(json)
    }

    override fun close() {
        try {
            webSocketSession.close()
        } catch (e: IOException) {

        }
    }
}
