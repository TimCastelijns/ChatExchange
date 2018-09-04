package com.timcastelijns.chatexchange.chat

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.time.Instant
import java.util.regex.Pattern

internal fun JsonObject.extractUsers(): List<User> =
        get("users")
                .asJsonArray
                .map { it.asJsonObject }
                .map {
                    with(it) {
                        val id = get("id").asLong
                        val userName = get("name").asString
                        val reputation = get("reputation").asInt
                        val isModerator = if (get("is_moderator").isJsonNull) false else get("is_moderator").asBoolean
                        val isRoomOwner = if (get("is_owner").isJsonNull) false else get("is_owner").asBoolean
                        val lastSeen = if (get("last_seen").isJsonNull) null else Instant.ofEpochSecond(get("last_seen").asLong)
                        val lastPost = if (get("last_post").isJsonNull) null else Instant.ofEpochSecond(get("last_post").asLong)

                        User(id, userName, reputation, isModerator, isRoomOwner, lastSeen, lastPost)
                    }
                }

internal fun JsonObject.extractEventsForRoom(room: Room): List<Event> =
        entrySet()
                .filter { it.key == "r${room.roomId}" }
                .map { it.value }
                .map { it.asJsonObject }
                .mapNotNull { it.get("e") }
                .map { it.asJsonArray }
                .firstOrNull()
                ?.toEvents(room) ?: emptyList()

private fun JsonArray.toEvents(room: Room): List<Event> {
    // Kicked?
    if (size() == 2 && hasEventType(4) && hasEventType(15)) {
        return listOf(KickedEvent(this, room))
    }

    // TODO: handle feeds (userId = -2)
    // TODO: handle unhandled events
    return map { it.asJsonObject }
            .filter {
                (!it.has("user_id") || it.get("user_id").asLong > 0) &&
                        it.get("room_id")?.asInt == room.roomId
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
                    StackExchangeEventType.ACCESS_LEVEL_CHANGED -> AccessLevelChangedEvent(it, room)
                    StackExchangeEventType.USER_NOTIFICATION -> UserNotificationEvent(it, room)
                    StackExchangeEventType.MESSAGE_REPLY -> MessageRepliedToEvent(it, room)
                    else -> {
                        println("Unhandled event: $it")
                        null
                    }
                }
            }
            .toList()
}

private fun JsonArray.hasEventType(eventType: Int) =
        map { it.asJsonObject }
                .any { it.get("event_type").asInt == eventType }



private const val MAX_CHAT_MESSAGE_LENGTH = 500
private val MARKDOWN_LINK_PATTERN = Pattern.compile("\\[(\\\\]|[^\\]])+\\]\\((https?:)?//(\\\\\\)|\\\\\\(|[^\\s)(])+\\)")

internal fun String.toParts(maxPartLength: Int = MAX_CHAT_MESSAGE_LENGTH): List<String> {
        var message = this
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
