package com.timcastelijns.chatexchange.chat

import org.junit.Test
import kotlin.test.assertEquals

class StackExchangeEventTypeTest {

    @Test
    fun `event type codes should be correctly mapped`() {
        assertEquals(1, StackExchangeEventType.MESSAGE_POSTED.code)
        assertEquals(2, StackExchangeEventType.MESSAGE_EDITED.code)
        assertEquals(3, StackExchangeEventType.USER_ENTERED.code)
        assertEquals(4, StackExchangeEventType.USER_LEFT.code)
        assertEquals(5, StackExchangeEventType.ROOM_NAME_CHANGED.code)
        assertEquals(6, StackExchangeEventType.MESSAGE_STARRED.code)
        assertEquals(7, StackExchangeEventType.DEBUG_MESSAGE.code)
        assertEquals(8, StackExchangeEventType.USER_MENTIONED.code)
        assertEquals(9, StackExchangeEventType.MESSAGE_FLAGGED.code)
        assertEquals(10, StackExchangeEventType.MESSAGE_DELETED.code)
        assertEquals(11, StackExchangeEventType.FILE_ADDED.code)
        assertEquals(12, StackExchangeEventType.MODERATOR_FLAG.code)
        assertEquals(13, StackExchangeEventType.USER_SETTINGS_CHANGED.code)
        assertEquals(14, StackExchangeEventType.GLOBAL_NOTIFICATION.code)
        assertEquals(15, StackExchangeEventType.ACCESS_LEVEL_CHANGED.code)
        assertEquals(16, StackExchangeEventType.USER_NOTIFICATION.code)
        assertEquals(17, StackExchangeEventType.INVITATION.code)
        assertEquals(18, StackExchangeEventType.MESSAGE_REPLY.code)
        assertEquals(19, StackExchangeEventType.MESSAGE_MOVED_OUT.code)
        assertEquals(20, StackExchangeEventType.MESSAGE_MOVED_IN.code)
        assertEquals(21, StackExchangeEventType.TIME_BREAK.code)
        assertEquals(22, StackExchangeEventType.FEED_TICKER.code)
        assertEquals(29, StackExchangeEventType.USER_SUSPENDED.code)
        assertEquals(30, StackExchangeEventType.USER_MERGED.code)
        assertEquals(34, StackExchangeEventType.USER_NAME_OR_AVATAR_CHANGED.code)
    }

}
