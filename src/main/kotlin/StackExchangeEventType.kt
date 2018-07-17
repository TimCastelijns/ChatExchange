internal enum class StackExchangeEventType(val code: Int) {

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
    USER_NOTIFICATION(16),
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
