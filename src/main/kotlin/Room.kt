import com.google.gson.JsonElement
import com.google.gson.JsonParser
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import java.util.regex.Pattern

class Room(
        val host: ChatHost,
        val roomId: Int,
        private val httpClient: HttpClient,
        private val cookies: MutableMap<String, String>
) {

    companion object {
        private const val SUCCESS = "ok"
        private val TRY_AGAIN_PATTERN = Pattern.compile("You can perform this action again in (\\d+) seconds")
        private val CURRENT_USERS_PATTERN = Pattern.compile("\\{id:\\s?(\\d+),")
        private val MARKDOWN_LINK_PATTERN = Pattern.compile("\\[(\\\\]|[^\\]])+\\]\\((https?:)?//(\\\\\\)|\\\\\\(|[^\\s)(])+\\)")
        private val FAILED_UPLOAD_PATTERN = Pattern.compile("var error = '(.+)';")
        private val SUCCESS_UPLOAD_PATTERN = Pattern.compile("var result = '(.+)';")
        private const val NUMBER_OF_RETRIES_ON_THROTTLE = 5
        private val MESSAGE_TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneOffset.UTC)
        private const val EDIT_WINDOW_SECONDS = 115
        private const val WEB_SOCKET_RESTART_SECONDS = 30L
        private const val MAX_CHAT_MESSAGE_LENGTH = 500
    }

    var messagePostedEventListener: ((MessagePostedEvent) -> Unit)? = null
    var messageEditedEventListener: ((MessageEditedEvent) -> Unit)? = null
    var messageDeletedEventListener: ((MessageDeletedEvent) -> Unit)? = null
    var messageStarredEventListener: ((MessageStarredEvent) -> Unit)? = null
    var userEnteredEventListener: ((UserEnteredEvent) -> Unit)? = null
    var userLeftEventListener: ((UserLeftEvent) -> Unit)? = null
    var userRequestedAccessEventListener: ((UserRequestedAccessEvent) -> Unit)? = null
    var userMentionedEventListener: ((UserMentionedEvent) -> Unit)? = null
    var messageRepliedToEventListener: ((MessageRepliedToEvent) -> Unit)? = null

    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val eventExecutor = Executors.newCachedThreadPool()

    private lateinit var webSocket: WebSocket
    private var lastWebsocketMessageDate = LocalDateTime.now()

    private lateinit var fkey: String
    private var hostUrlBase: String = host.baseUrl

    private var hasLeft = false

    private var pingableUserIds = listOf<Long>()
    private val currentUserIds = mutableSetOf<Long>()

    init {
        executeAndSchedule(Runnable { fkey = retrieveFkey(roomId) }, 1)
        executeAndSchedule(Runnable { syncPingableUsers() }, 24)

        syncCurrentUsers()
        initWebsocket()

        executor.scheduleAtFixedRate({
            if (ChronoUnit.SECONDS.between(lastWebsocketMessageDate, LocalDateTime.now()) > WEB_SOCKET_RESTART_SECONDS) {
                closeWebSocket()
                try {
                    Thread.sleep(3000)
                } catch (e: InterruptedException) {

                }
                initWebsocket()
            }
        }, WEB_SOCKET_RESTART_SECONDS, WEB_SOCKET_RESTART_SECONDS,
                TimeUnit.SECONDS)
    }

    private fun initWebsocket() {
        var webSocketUrl = post("$hostUrlBase/ws-auth", "roomid",
                roomId.toString())
                .asJsonObject
                .get("url").asString

        val time = post("$hostUrlBase/chats/$roomId/events")
                .asJsonObject
                .get("time").asString

        webSocketUrl = "$webSocketUrl?l=$time"

        webSocket = WebSocket(hostUrlBase)
        webSocket.chatEventListener = { handleChatEvent(it) }
        webSocket.open(webSocketUrl)
    }

    private fun handleChatEvent(json: String) {
        lastWebsocketMessageDate = LocalDateTime.now()
        val jsonObject = JsonParser().parse(json).asJsonObject
        val jsonArray = jsonObject.entrySet().filter { it.key.equals("r$roomId") }
                .map { it.value }
                .map { it.asJsonObject }
                .mapNotNull { it.get("e") }
                .map { it.asJsonArray }
                .firstOrNull()

        jsonArray?.let {
            it.toEvents(this).forEach { event ->
                when (event) {
                    is MessagePostedEvent -> messagePostedEventListener?.invoke(event)
                    is MessageEditedEvent -> messageEditedEventListener?.invoke(event)
                    is MessageDeletedEvent -> messageDeletedEventListener?.invoke(event)
                    is MessageStarredEvent -> messageStarredEventListener?.invoke(event)
                    is UserEnteredEvent -> {
                        currentUserIds += event.userId
                        userEnteredEventListener?.invoke(event)
                    }
                    is UserLeftEvent -> {
                        currentUserIds -= event.userId
                        userLeftEventListener?.invoke(event)
                    }
                    is UserRequestedAccessEvent -> userRequestedAccessEventListener?.invoke(event)
                    is UserMentionedEvent -> userMentionedEventListener?.invoke(event)
                    is MessageRepliedToEvent -> messageRepliedToEventListener?.invoke(event)
                }
            }
        }
    }

    private fun executeAndSchedule(action: Runnable, rate: Long) {
        action.run()
        executor.scheduleAtFixedRate(action, rate, rate, TimeUnit.HOURS)
    }

    private fun retrieveFkey(roomId: Int): String {
        try {
            val response = httpClient.get("$hostUrlBase/rooms/$roomId", cookies)
            return response.parse().getElementById("fkey").`val`()
        } catch (e: IOException) {
            throw ChatOperationException(e)
        }
    }

    private fun syncPingableUsers() {
        val json = try {
            httpClient.get("$hostUrlBase/rooms/pingable/$roomId", cookies)
                    .body()
        } catch (e: IOException) {
            throw ChatOperationException(e)
        }

        val jsonArray = JsonParser().parse(json).asJsonArray
        pingableUserIds = jsonArray.map { it.asJsonArray.get(0).asLong }
    }

    private fun syncCurrentUsers() {
        val document = try {
            httpClient.get("$hostUrlBase/rooms/$roomId", cookies)
                    .parse()
        } catch (e: IOException) {
            throw ChatOperationException(e)
        }

        val html = document.getElementsByTag("script")[3].html()

        val matcher = CURRENT_USERS_PATTERN.matcher(html)
        currentUserIds.clear()
        while (matcher.find()) {
            currentUserIds.add(matcher.group(1).toLong())
        }
    }

    private fun post(url: String, vararg data: String) =
            post(NUMBER_OF_RETRIES_ON_THROTTLE, url, *data)

    private fun post(retryCount: Int, url: String, vararg data: String): JsonElement {
        val response = try {
            httpClient.postIgnoringErrors(url, cookies, *withFkey(arrayOf(*data)))
        } catch (e: IOException) {
            throw ChatOperationException(e)
        }

        val body = response.body()
        if (response.statusCode() == 200) {
            return JsonParser().parse(body)
        }

        val matcher = TRY_AGAIN_PATTERN.matcher(body)
        if (retryCount > 0 && matcher.find()) {
            val throttle = matcher.group(1).toLong()
            try {
                Thread.sleep(1000 * throttle)
            } catch (e: InterruptedException) {

            }

            return post(retryCount - 1, url, *data)
        } else {
            throw ChatOperationException("The chat operation failed with the message: $body")
        }
    }

    private fun withFkey(data: Array<String>): Array<String> {
        val dataWithFkey = Array(data.size + 2) { "" }
        dataWithFkey[0] = ("fkey")
        dataWithFkey[1] = (fkey)
        System.arraycopy(data, 0, dataWithFkey, 2, data.size)
        return dataWithFkey
    }

    private fun <T> supplyAsync(supplier: Supplier<T>) =
            CompletableFuture.supplyAsync(supplier, executor)
                    .whenComplete { res, t ->
                        if (res != null) {

                        }

                        if (t != null) {

                        }
                    }

    fun getUser(userId: Long) = getUsers(listOf(userId))[0]

    private fun getUsers(userIds: Iterable<Long>): List<User> {
        val ids = userIds.joinToString(separator = ",") { it.toString() }

        return post("$hostUrlBase/user/info", "ids", ids, "roomId", roomId.toString())
                .asJsonObject.get("users")
                .asJsonArray
                .map { it.asJsonObject }
                .map {
                    val user = with(it) {
                        val id = get("id").asLong
                        val userName = get("name").asString
                        val reputation = get("reputation").asInt
                        val isModerator = if (get("is_moderator").isJsonNull) false else get("is_moderator").asBoolean
                        val isRoomOwner = if (get("is_owner").isJsonNull) false else get("is_owner").asBoolean
                        val lastSeen = if (get("last_seen").isJsonNull) null else Instant.ofEpochSecond(get("last_seen").asLong)
                        val lastPost = if (get("last_post").isJsonNull) null else Instant.ofEpochSecond(get("last_post").asLong)
                        val currentlyInRoom = currentUserIds.contains(id)
                        val profileLink = "$hostUrlBase/users/$id"

                        User(id, userName, reputation, isModerator, isRoomOwner, lastSeen, lastPost, currentlyInRoom, profileLink)
                    }
                    user
                }
    }

    fun getMessage(messageId: Long): Message {
        val documentHistory: Document
        val content: String
        try {
            documentHistory = httpClient.get("$hostUrlBase/messages/$messageId/history", cookies, "fkey", fkey).parse()
            content = Parser.unescapeEntities(httpClient.get("$hostUrlBase/message/$messageId", cookies, "fkey", fkey).body(), false)
        } catch (e: HttpStatusException) {
            if (e.statusCode == 404) {
                // non-RO cannot see deleted message of another user: so if 404, it means message is deleted
                return Message(messageId, null, null, null, true, 0, false, 0)
            }
            throw ChatOperationException(e)
        } catch (e: IOException) {
            throw ChatOperationException(e)
        }

        val contents = documentHistory.select(".messages .content")
        val plainContent = contents.get(1).select(".message-source").first().text()
        val starVoteContainer = documentHistory.select(".messages .flash .stars.vote-count-container").first()

        val starCount = if (starVoteContainer == null) {
            0
        } else {
            val times = starVoteContainer.select(".times").first()
            if (times == null || !times.hasText()) 1 else times.text().toInt()
        }

        val pinned = !documentHistory.select(".vote-count-container.stars.owner-star").isEmpty()
        val editCount = contents.size - 2 // -2 to remove the current version and the first version
        val user = getUser(documentHistory.select(".username > a").first().attr("href").split("/")[2].toLong())
        val deleted = contents.any { it.getElementsByTag("b").html().equals("deleted") }
        return Message(messageId, user, plainContent, content, deleted, starCount, pinned, editCount)
    }

    fun send(message: String): CompletionStage<Long> {
        val parts = toParts(message, MAX_CHAT_MESSAGE_LENGTH)
        for (i in 0 until parts.size) {
            val part = parts[i]
            supplyAsync(Supplier {
                val element = post("$hostUrlBase/chats/$roomId/messages/new", "text", part)
                return@Supplier element.asJsonObject.get("id").asLong
            })
        }

        val part = parts.last()
        return supplyAsync(Supplier {
            val element = post("$hostUrlBase/chats/$roomId/messages/new", "text", part)
            return@Supplier element.asJsonObject.get("id").asLong
        })
    }

    fun uploadImage(path: Path): CompletionStage<String> {
        val inputStream = try {
            Files.newInputStream(path)
        } catch (e: IOException) {
            throw ChatOperationException("Can't open path $path for reading", e)
        }

        return uploadImage(path.fileName.toString(), inputStream).whenComplete { _, _ ->
            try {
                inputStream.close()
            } catch (e: IOException) {

            }
        }
    }

    private fun uploadImage(fileName: String, inputStream: InputStream): CompletionStage<String> =
            supplyAsync(Supplier {
                val response = try {
                    httpClient.postWithFile("$hostUrlBase/upload/image", cookies, "filename", fileName, inputStream)
                } catch (e: IOException) {
                    throw ChatOperationException("Failed to upload image", e)
                }

                val html = Jsoup.parse(response.body()).getElementsByTag("script").first().html()
                val failedUploadMatched = FAILED_UPLOAD_PATTERN.matcher(html)

                if (failedUploadMatched.find()) {
                    throw ChatOperationException(failedUploadMatched.group(1))
                }

                val successUploadMatcher = SUCCESS_UPLOAD_PATTERN.matcher(html)
                if (successUploadMatcher.find()) {
                    return@Supplier successUploadMatcher.group(1)
                }

                throw ChatOperationException("Failed to upload image")
            })

    private fun toParts(_message: String, maxPartLength: Int): List<String> {
        var message = _message
        if (message.length <= maxPartLength || (message.trim().contains("\n") && !message.trim().endsWith("\n"))) {
            return listOf(message)
        }

        val messages = mutableListOf<String>()
        while (message.length > maxPartLength) {
            val nonBreakingIndices = message.identifyNonBreakingIndexes()
            var breakIndex = message.lastIndexOf(' ', maxPartLength)
            if (breakIndex < 0) {
                breakIndex = maxPartLength
            }

            nonBreakingIndices.forEach {
                if (it[0] < breakIndex && breakIndex < it[1]) {
                    breakIndex = it[0] - 1
                    return@forEach
                }
            }
            if (breakIndex < 0) {
                // we did our best, but this part starts with a non breaking index, and ends further than what is allowed...
                throw ChatOperationException("Cannot send message: it is longer than $maxPartLength characters and cannot be broken into adequate parts")
            }

            messages.add(message.substring(0, breakIndex))
            message = message.substring(breakIndex + 1)
        }
        if (!message.isEmpty()) {
            messages.add(message)
        }
        return messages
    }

    private fun String.identifyNonBreakingIndexes(): List<IntArray> {
        val nonBreakingParts = mutableListOf<IntArray>()
        val matcher = MARKDOWN_LINK_PATTERN.matcher(this)

        while (matcher.find()) {
            nonBreakingParts.add(intArrayOf(matcher.start(), matcher.end()))
        }

        return nonBreakingParts
    }

    fun replyTo(messageId: Long, message: String) = send(":$messageId $message")

    fun isEditable(messageId: Long): Boolean {
        try {
            val documentHistory = httpClient.get("$hostUrlBase/messages/$messageId/history", cookies, "fkey", fkey).parse()
            val time = LocalTime.parse(documentHistory.getElementsByClass("timestamp").last().html(), MESSAGE_TIME_FORMATTER)
            return ChronoUnit.SECONDS.between(time, LocalTime.now(ZoneOffset.UTC)) < EDIT_WINDOW_SECONDS
        } catch (e: IOException) {
            throw ChatOperationException(e)
        }
    }

    fun delete(messageId: Long) =
            supplyAsync(Supplier {
                val result = post("$hostUrlBase/messages/$messageId/delete").asString
                if (SUCCESS != result) {
                    throw ChatOperationException("Cannot delete message $messageId for reason: $result")
                }
                return@Supplier null
            })

    fun toggleStar(messageId: Long) =
            supplyAsync(Supplier {
                val result = post("$hostUrlBase/messages/$messageId/star").asString
                if (SUCCESS != result) {
                    throw ChatOperationException("Cannot star/unstar message $messageId for reason: $result")
                }
                return@Supplier null
            })

    fun togglePin(messageId: Long) =
            supplyAsync(Supplier {
                val result = post("$hostUrlBase/messages/$messageId/owner-str").asString
                if (SUCCESS != result) {
                    throw ChatOperationException("Cannot pin/unpin message $messageId for reason: $result")
                }
                return@Supplier null
            })

    fun leave(quiet: Boolean = true) {
        if (hasLeft) {
            return
        }

        post("$hostUrlBase/chats/leave/$roomId", "quiet", quiet.toString())
        hasLeft = true
        close()
    }

    fun getPingableUsers() = getUsers(pingableUserIds)

    private fun getThumbs(): RoomThumbs {
        val json = try {
            httpClient.get("$hostUrlBase/rooms/thumbs/$roomId", cookies).body()
        } catch (e: IOException) {
            throw ChatOperationException(e)
        }

        val jsonObject = JsonParser().parse(json).asJsonObject
        val tags = Jsoup.parse(jsonObject.get("tags").asString).getElementsByTag("a").map {
            it.html()
        }

        with(jsonObject) {
            return RoomThumbs(get("id").asInt, get("name").asString, get("description").asString, get("isFavorite").asBoolean, tags)
        }
    }

    private fun close() {
        executor.shutdown()
        eventExecutor.shutdown()
        closeWebSocket()
    }

    private fun closeWebSocket() {
        webSocket.close()
    }

}

private data class RoomThumbs(
        val id: Int,
        val name: String,
        val description: String,
        val isFavorite: Boolean,
        val tags: List<String>
)
