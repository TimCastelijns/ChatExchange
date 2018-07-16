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

fun JsonArray.toEvents(room: Room): List<Event> {
    // kicked?
    if (size() == 2 &&
            map { it.asJsonObject }.any { it.get("event_type").asInt == 4 } &&
            map { it.asJsonObject }.any { it.get("event_type").asInt == 15 }) {
        return listOf(KickedEvent(this, room))
    }

    // TODO: handle feeds (userId = -2)
    return map { it.asJsonObject }
            .filter {
                (!it.has("user_id") || it.get("user_id").asLong > 0) &&
                        it.get("room_id").asInt == room.roomId
            }
            .mapNotNull {
                when (StackExchangeEventType.fromCode(it.get("event_type").asInt)) {
                    StackExchangeEventType.MESSAGE_POSTED -> MessagePostedEvent(it, room)
                    StackExchangeEventType.MESSAGE_EDITED -> MessageEditedEvent(it, room)
                    StackExchangeEventType.MESSAGE_STARRED -> MessageStarredEvent(it, room)
                    StackExchangeEventType.MESSAGE_DELETED -> MessageDeletedEvent(it, room)
                    StackExchangeEventType.USER_ENTERED -> UserEnteredEvent(it, room)
                    StackExchangeEventType.USER_LEFT -> UserLeftEvent(it, room)
                    StackExchangeEventType.USER_MENTIONED -> UserMentionedEvent(it, room)
                    StackExchangeEventType.USER_REQUESTED_ACCESS -> UserRequestedAccessEvent(it, room)
                    StackExchangeEventType.MESSAGE_REPLY -> MessageRepliedToEvent(it, room)
                    else -> {
                        println("Unhandled event: $it")
                        null
                    }
                }
            }
            .toList()

}

private enum class StackExchangeEventType(val code: Int) {

    MESSAGE_POSTED(1),
    MESSAGE_EDITED(2),
    USER_ENTERED(3),
    USER_LEFT(4),
    ROOM_NAME_CHANGED(5),
    MESSAGE_STARRED(6),
    DEBUG_MESSAGE(7),
    USER_MENTIONED(8),
    MESSAGE_FLAGGED(9),
    MESSAGE_DELETED(10),
    FILE_ADDED(11),
    MODERATOR_FLAG(12),
    USER_SETTINGS_CHANGED(13),
    GLOBAL_NOTIFICATION(14),
    ACCESS_LEVEL_CHANGED(15),
    USER_REQUESTED_ACCESS(16),
    INVITATION(17),
    MESSAGE_REPLY(18),
    MESSAGE_MOVED_OUT(19),
    MESSAGE_MOVED_IN(20),
    TIME_BREAK(21),
    FEED_TICKER(22),
    USER_SUSPENDED(29),
    USER_MERGED(30),
    USER_NAME_OR_AVATAR_CHANGED(34);

    companion object {

        fun fromCode(code: Int) = StackExchangeEventType.values()
                .firstOrNull { it.code == code }
    }

}

//note: below is what an access request looks like. Is it actually event 15?
// {"event_type":15,"time_stamp":1531495000,"content":"Access now request","id":87812698,"user_id":1869081,
// "target_user_id":1869081,"user_name":"Tomer Oszlak","room_id":15,"room_name":"Android"}
