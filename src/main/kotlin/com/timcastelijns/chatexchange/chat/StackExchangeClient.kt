package com.timcastelijns.chatexchange.chat

import java.io.IOException

class StackExchangeClient(
        private val email: String,
        private val password: String
) : AutoCloseable {

    private val rooms = mutableListOf<Room>()

    private fun seLogin(email: String, password: String, _host: String, autoCreateAccount: Boolean = true) {
        var host = _host
        val originalHost = host

        if (host.equals(ChatHost.STACK_EXCHANGE.hostName, ignoreCase = true)) {
            host = ChatHost.META_STACK_EXCHANGE.hostName
        }

        var response = HttpClient.get("https://$host/users/login")
        val fkey = response.parse()
                .select("input[name='fkey']")
                .`val`()

        response = HttpClient.post("https://$host/users/login",
                "email", email, "password", password, "fkey", fkey)

        // Create an account on that site if necessary
        val formElement = response.parse().getElementById("logout-user")
        if (formElement != null) {
            if (!autoCreateAccount) {
                throw IllegalStateException("Unable to login to Stack Exchange. " +
                        "The user does not have an account on $originalHost")
            }

            val formInputs = formElement.getElementsByTag("input")
            val formData = mutableListOf<String>()

            formInputs.forEach {
                val key = it.attr("name")
                val value = it.`val`()

                if (key == null || key.isEmpty()) {
                    return@forEach
                }

                formData.add(key)
                formData.add(value)
            }

            val formUrl = "https://$host${formElement.attr("action")}"

            val formResponse = HttpClient.post(formUrl, *formData.toTypedArray())
            if (formResponse.parse().getElementsByClass("js-inbox-button")
                            .first() == null) {
                throw IllegalStateException("Unable to create an account on $host. " +
                        "Please create the account manually")
            }
        }

        val checkResponse = HttpClient.get("https://$originalHost/users/current")
        if (checkResponse.parse().getElementsByClass("js-inbox-button")
                        .first() == null) {
            throw IllegalStateException("Unable to login to Stack Exchange. " +
                    "(Site: $originalHost via $host)")
        }
    }

    fun joinRoom(host: ChatHost, roomId: Int): Room {
        val mainSiteHost = host.hostName
        val alreadyLoggedIn = rooms.any { it.host == host }

        if (!alreadyLoggedIn) {
            try {
                seLogin(email, password, mainSiteHost)
            } catch (e: IOException) {
                throw ChatOperationException("Login to $mainSiteHost failed")
            }
        }

        val alreadyLoggedInToThisRoom = alreadyLoggedIn && rooms.any { it.roomId == roomId }
        if (alreadyLoggedInToThisRoom) {
            throw ChatOperationException("Cannot join a room you are already in")
        }

        val chatRoom = Room(host, roomId)
        rooms.add(chatRoom)
        println("Joined room $roomId")
        return chatRoom
    }

    override fun close() {
        rooms.forEach {
            it.leave()
        }
    }

}
