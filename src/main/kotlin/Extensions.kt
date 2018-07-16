import com.google.gson.JsonArray
import com.google.gson.JsonObject

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

private fun JsonArray.hasEventType(eventType: Int) =
        map { it.asJsonObject }
                .any { it.get("event_type").asInt == eventType }
