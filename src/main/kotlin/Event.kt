import com.google.gson.JsonArray
import com.google.gson.JsonElement
import java.time.Instant

sealed class Event(jsonElement: JsonElement, room: Room) {

    var instant: Instant
    var userId: Long
    var userName: String?
    var user: User? = null

    init {
        val jsonObject = jsonElement.asJsonObject
        instant = Instant.ofEpochSecond(jsonObject.get("time_stamp").asLong)
        userId = jsonObject.get("user_id")?.asLong ?: 0
        userName = jsonObject.get("user_name")?.asString
        user = if (userId > 0) room.getUser(userId) else null
    }
}

open class MessageEvent(jsonElement: JsonElement, room: Room) : Event(jsonElement, room) {

    val message: Message

    init {
        val jsonObject = jsonElement.asJsonObject
        message = room.getMessage(jsonObject.get("message_id").asLong)
    }
}

open class PingMessageEvent(jsonElement: JsonElement, room: Room) : MessageEvent(jsonElement, room) {

    val parentMessageId: Long
    val targetUserId: Long

    init {
        val jsonObject = jsonElement.asJsonObject
        targetUserId = jsonObject.get("target_user_id").asLong
        parentMessageId = jsonObject.get("parent_id")?.asLong ?: -1
    }
}

class MessagePostedEvent(jsonElement: JsonElement, room: Room) : MessageEvent(jsonElement, room)

class MessageEditedEvent(jsonElement: JsonElement, room: Room) : MessageEvent(jsonElement, room)

class MessageDeletedEvent(jsonElement: JsonElement, room: Room) : MessageEvent(jsonElement, room)

class MessageStarredEvent(jsonElement: JsonElement, room: Room) : MessageEvent(jsonElement, room) {

    val starred: Boolean
    val pinned: Boolean

    init {
        val jsonObject = jsonElement.asJsonObject
        starred = jsonObject.get("message_starred")?.asBoolean ?: false
        pinned = jsonObject.get("message_owner_starred")?.asBoolean ?: false
    }
}

class UserEnteredEvent(jsonElement: JsonElement, room: Room) : Event(jsonElement, room)

class UserLeftEvent(jsonElement: JsonElement, room: Room) : Event(jsonElement, room)

class UserRequestedAccessEvent(jsonElement: JsonElement, room: Room) : Event(jsonElement, room)

class UserMentionedEvent(jsonElement: JsonElement, room: Room) : PingMessageEvent(jsonElement, room)

class MessageRepliedToEvent(jsonElement: JsonElement, room: Room) : PingMessageEvent(jsonElement, room)

class KickedEvent(jsonElement: JsonElement, room: Room) : Event(jsonElement, room) {
    // TODO implement this class
}

data class Message(
        val id: Long,
        val user: User?,
        val plainContent: String?,
        val content: String?,
        val isDeleted: Boolean,
        val starCount: Int,
        val isPinned: Boolean,
        val editCount: Int
)

data class User(
        val id: Long,
        val name: String,
        val reputation: Int,
        val isModerator: Boolean,
        val isRoomOwner: Boolean,
        val dateLastSeen: Instant?,
        val dateLastMessage: Instant?,
        val isCurrentlyInRoom: Boolean,
        val profileLink: String
)
