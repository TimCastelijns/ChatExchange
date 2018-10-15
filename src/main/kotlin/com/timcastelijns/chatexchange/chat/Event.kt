package com.timcastelijns.chatexchange.chat

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
        parentMessageId = jsonObject.get("parent_id")?.asLong ?: -1
        isReply = parentMessageId != -1
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

class UserNotificationEvent(jsonElement: JsonElement, room: Room) : Event(jsonElement, room)

class AccessLevelChangedEvent(jsonElement: JsonElement, room: Room) : Event(jsonElement, room) {

    val accessLevel: AccessLevel
    val targetUserId: Long
    val targetUser: User

    init {
        val jsonObject = jsonElement.asJsonObject
        val content = jsonObject.get("content").asString
        accessLevel = AccessLevel.fromAlias(content.split(" ")[2])
        targetUserId = jsonObject.get("target_user_id").asLong
        targetUser = room.getUser(targetUserId)
    }
}

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
        val editCount: Int,
        val parentMessageId: Long,
        val isReply: Boolean
)

data class User(
        val id: Long,
        val name: String,
        val reputation: Int,
        val isModerator: Boolean,
        val isRoomOwner: Boolean,
        val dateLastSeen: Instant?,
        val dateLastMessage: Instant?,
        var isCurrentlyInRoom: Boolean = false,
        var profileLink: String = ""
)

enum class AccessLevel(val alias: String) {

    REQUEST("request"),
    READ_WRITE("read-write"),
    READ("read"),
    DEFAULT("(default)");

    companion object {

        fun fromAlias(alias: String) = AccessLevel.values()
                .first { it.alias == alias }
    }

}
