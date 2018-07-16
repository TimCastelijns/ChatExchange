import java.io.IOException

class StackExchangeClient(
        private val email: String,
        private val password: String
) : AutoCloseable {

    private val httpClient = HttpClient()
    private val cookies = HashMap<String, String>()
    private val rooms = mutableListOf<Room>()
    private val autoCreateAccount = true

    private fun seLogin(email: String, password: String, _host: String) {
        var host = _host
        val originalHost = host

        if (host.equals(ChatHost.STACK_EXCHANGE.hostName, ignoreCase = true)) {
            host = ChatHost.META_STACK_EXCHANGE.hostName
        }

        var response = httpClient.get("https://$host/users/login", cookies)
        val fkey = response.parse()
                .select("input[name='fkey']")
                .`val`()

        response = httpClient.post("https://$host/users/login", cookies,
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

            val formResponse = httpClient.post(formUrl, cookies, *formData.toTypedArray())
            if (formResponse.parse().getElementsByClass("js-inbox-button")
                            .first() == null) {
                throw IllegalStateException("Unable to create an account on $host. " +
                        "Please create the account manually")
            }
        }

        val checkResponse = httpClient.get("https://$originalHost/users/current", cookies)
        if (checkResponse.parse().getElementsByClass("js-inbox-button")
                        .first() == null) {
            throw IllegalStateException("Unable to login to Stack Exchange. " +
                    "(Site: $originalHost via $host)")
        }
    }

    fun joinRoom(host: ChatHost, roomId: Int): Room {
        val mainSiteHost = host.hostName
        var alreadyLoggedIn = false

        rooms.forEach {
            if (it.host == host) {
                alreadyLoggedIn = true
                return@forEach
            }
        }

        if (!alreadyLoggedIn) {
            try {
                seLogin(email, password, mainSiteHost)
            } catch (e: IOException) {
                throw ChatOperationException("Login to $mainSiteHost failed")
            }
        }

        if (rooms.any { it.host == host && it.roomId == roomId }) {
            throw ChatOperationException("Cannot join a room you are already in")
        }

        val chatRoom = Room(host, roomId, httpClient, cookies)
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
