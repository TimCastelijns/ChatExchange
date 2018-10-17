package com.timcastelijns.chatexchange.chat

import org.junit.Test

class EventParserTest {

    @Test
    fun `message posted event should be parsed correctly`() {
        val raw = """
            {
                "r15": {
                    "e": [{
                        "event_type": 1,
                        "time_stamp": 1539700465,
                        "content": "test",
                        "id": 89977419,
                        "user_id": 1843331,
                        "user_name": "Tim Castelijns",
                        "room_id": 15,
                        "room_name": "Android",
                        "message_id": 44284587
                    }],
                    "t": 89977421,
                    "d": 3
                }
            }
        """.trimIndent()
    }

    @Test
    fun `message edited event should be parsed correctly`() {
        val raw = """
            {
                "r15": {
                    "e": [{
                        "event_type": 2,
                        "time_stamp": 1539700744,
                        "content": "Are you saying CF doesn\u0026#39;t make good comments, Felix?",
                        "id": 89977584,
                        "user_id": 4070469,
                        "user_name": "Mauker",
                        "room_id": 15,
                        "room_name": "Android",
                        "message_id": 44284680,
                        "message_edits": 1
                    }],
                    "t": 89977584,
                    "d": 1
                }
            }
        """.trimIndent()
    }

    @Test
    fun `user joined event should be parsed correctly`() {
        val raw = """
            {
                "r15": {
                    "e": [{
                        "event_type": 3,
                        "time_stamp": 1539701390,
                        "id": 89977925,
                        "user_id": 7495506,
                        "target_user_id": 7495506,
                        "user_name": "SkynetTester",
                        "room_id": 15,
                        "room_name": "Android"
                    }],
                    "t": 89977925,
                    "d": 1
                }
            }
        """.trimIndent()
    }

    @Test
    fun `user left event should be parsed correctly`() {
        val raw = """
            {
                "r15": {
                    "e": [{
                        "event_type": 4,
                        "time_stamp": 1539700514,
                        "id": 89977444,
                        "user_id": 4467208,
                        "target_user_id": 4467208,
                        "user_name": "Murat Karagöz",
                        "room_id": 15,
                        "room_name": "Android"
                    }],
                    "t": 89977459,
                    "d": 16
                }
            }
        """.trimIndent()
    }

    @Test
    fun `message starred event should be parsed correctly`() {
        // Note: this event (6) is raised for both starred and pinned.
        // Note when 'you' star a message, the payload includes user_name and message_starred
        val rawOtherUserStarred = """
            {
                "r15": {
                    "e": [{
                        "event_type": 6,
                        "time_stamp": 1539690843,
                        "content": "\o",
                        "id": 89977708,
                        "room_id": 15,
                        "room_name": "Android",
                        "message_id": 44282230,
                        "message_stars": 4
                    }],
                    "t": 89977708,
                    "d": 1
                }
            }
        """.trimIndent()
        val loggedInUserStarred = """
            {
                "r15": {
                    "e": [{
                        "event_type": 6,
                        "time_stamp": 1539764506,
                        "content": "o/",
                        "id": 90000092,
                        "user_id": 1843331,
                        "user_name": "Tim Castelijns",
                        "room_id": 15,
                        "room_name": "Android",
                        "message_id": 44292692,
                        "message_stars": 1,
                        "message_starred": true
                    }],
                    "t": 90000092,
                    "d": 1
                }
            }
        """.trimIndent()
    }

    @Test
    fun `message pinned event should be parsed correctly`() {
        // Note: this event (6) is raised for both starred and pinned.
        // Note when 'you' pin a message, the payload includes user_name and message_starred
        // Note: message_owner_stars and message_owner_starred are excluded for unpin events.
        val raw = """
            {
                "r15": {
                    "e": [{
                        "event_type": 6,
                        "time_stamp": 1539764322,
                        "content": "can we remove the \u0026quot;cats\u0026quot; tag on the chat room",
                        "id": 90000039,
                        "user_id": 1843331,
                        "user_name": "Tim Castelijns",
                        "room_id": 15,
                        "room_name": "Android",
                        "message_id": 44292653,
                        "message_stars": 1,
                        "message_owner_stars": 1,
                        "message_starred": true,
                        "message_owner_starred": true
                    }],
                    "t": 90000039,
                    "d": 1
                }
            }
        """.trimIndent()
    }

    @Test
    fun `user mentioned event should be parsed correctly`() {
        val raw = """
            {
                "r15": {
                    "e": [{
                        "event_type": 8,
                        "time_stamp": 1539701511,
                        "content": "@TimCastelijns",
                        "id": 89978081,
                        "user_id": 7495506,
                        "target_user_id": 1843331,
                        "user_name": "SkynetTester",
                        "room_id": 15,
                        "room_name": "Android",
                        "message_id": 44284861,
                        "parent_id": 44284855
                    }],
                    "t": 89978082,
                    "d": 4
                }
            }
        """.trimIndent()
    }

    @Test
    fun `message flagged event should be parsed correctly`() {
        val raw = """
            {
                "r15": {
                    "e": [{
                        "event_type": 9,
                        "time_stamp": 1539784900,
                        "content": "if you find that offensive womp, feel free to flag it (I need event data for flagged messages)",
                        "id": 90009500,
                        "room_id": 15,
                        "room_name": "Android",
                        "message_id": 44296899,
                        "message_flags": 1
                    }],
                    "t": 90009500,
                    "d": 1
                }
            }
        """.trimIndent()
    }

    @Test
    fun `message deleted event should be parsed correctly`() {
        val raw = """
            {
                "r15": {
                    "e": [{
                        "event_type": 10,
                        "time_stamp": 1539702258,
                        "id": 89978496,
                        "user_id": 1843331,
                        "user_name": "Tim Castelijns",
                        "room_id": 15,
                        "room_name": "Android",
                        "message_id": 44285062
                    }],
                    "t": 89978496,
                    "d": 1
                }
            }
        """.trimIndent()
    }

    @Test
    fun `access level changed events should be parsed correctly`() {
        val raw = """
            {
                "r15": {
                    "e": [{
                        "event_type": 15,
                        "time_stamp": 1539701973,
                        "content": "Access now read-only",
                        "id": 89978287,
                        "user_id": 1843331,
                        "target_user_id": 7495506,
                        "user_name": "Tim Castelijns",
                        "room_id": 15,
                        "room_name": "Android"
                    }, {
                        "event_type": 15,
                        "time_stamp": 1539702031,
                        "content": "Access now (default)",
                        "id": 89978322,
                        "user_id": 1843331,
                        "target_user_id": 7495506,
                        "user_name": "Tim Castelijns",
                        "room_id": 15,
                        "room_name": "Android"
                    }, {
                        "event_type": 15,
                        "time_stamp": 1539702052,
                        "content": "Access now request",
                        "id": 89978330,
                        "user_id": 7495506,
                        "target_user_id": 7495506,
                        "user_name": "SkynetTester",
                        "room_id": 15,
                        "room_name": "Android"
                    }],
                    "t": 89978288,
                    "d": 2
                }
            }
        """.trimIndent()
    }

    @Test
    fun `user notification event should be parsed correctly`() {
        val raw = """
            {
            "r15": {
                "e": [{
                    "event_type": 15,
                    "time_stamp": 1539702052,
                    "content": "Access now request",
                    "id": 89978330,
                    "user_id": 7495506,
                    "target_user_id": 7495506,
                    "user_name": "SkynetTester",
                    "room_id": 15,
                    "room_name": "Android"
                }, {
                    "event_type": 16,
                    "time_stamp": 1539702052,
                    "content": "\u003ca href=\"/users/7495506/skynettester\"\u003eSkynetTester\u003c/a\u003e has requested access to \u003ca href=\"/rooms/info/15?tab=access\"\u003eAndroid\u003c/a\u003e.",
                    "id": 89978336,
                    "user_id": 7495506,
                    "target_user_id": 1843331,
                    "user_name": "SkynetTester"
                }],
                "t": 89978343,
                "d": 15
            }
        }
        """.trimIndent()
    }

    @Test
    fun `invitation event should be parsed correctly`() {
        val raw = """
            {
                "r15": {
                    "e": [{
                        "event_type": 17,
                        "time_stamp": 1539701801,
                        "content": "\u003ca href=\"/users/7495506/skynettester\"\u003eSkynetTester\u003c/a\u003e has invited you to join \u003ca href=\"/rooms/1/sandbox\" target=\"_self\"\u003eSandbox\u003c/a\u003e. See your \u003ca href=\"/rooms?tab=invited\"\u003einvitations\u003c/a\u003e.",
                        "id": 89978214,
                        "user_id": 7495506,
                        "target_user_id": 1843331,
                        "user_name": "SkynetTester",
                        "room_id": 1,
                        "room_name": "Sandbox"
                    }],
                    "t": 89978214,
                    "d": 1
                }
            }
        """.trimIndent()
    }

    @Test
    fun `message reply event should be parsed correctly`() {
        // A reply consists of both a message and a reply event.
        val raw = """
            {
                "r15": {
                    "e": [{
                        "event_type": 18,
                        "time_stamp": 1539701610,
                        "content": "@TimCastelijns LMAO @Jordy summon",
                        "id": 89978123,
                        "user_id": 5148907,
                        "target_user_id": 1843331,
                        "user_name": "Cold Fire",
                        "room_id": 15,
                        "room_name": "Android",
                        "message_id": 44284890,
                        "parent_id": 44284882,
                        "show_parent": true
                    }, {
                        "event_type": 1,
                        "time_stamp": 1539701610,
                        "content": "@TimCastelijns LMAO @Jordy summon",
                        "id": 89978125,
                        "user_id": 5148907,
                        "user_name": "Cold Fire",
                        "room_id": 15,
                        "room_name": "Android",
                        "message_id": 44284890,
                        "parent_id": 44284882,
                        "show_parent": true
                    }],
                    "t": 89978125,
                    "d": 4
                }
            }
        """.trimIndent()
    }

    @Test
    fun `message moved out event should be parsed correctly`() {
        // A reply consists of both a message and a reply event.
        val raw = """
            {
                "r15": {
                    "e": [{
                        "event_type": 19,
                        "time_stamp": 1539701486,
                        "content": "@timcastelijns",
                        "id": 89978251,
                        "user_id": 1843331,
                        "user_name": "Tim Castelijns",
                        "room_id": 15,
                        "room_name": "Android",
                        "message_id": 44284848,
                        "moved": true
                    }, {
                        "event_type": 19,
                        "time_stamp": 1539701496,
                        "content": "@ColdFire",
                        "id": 89978253,
                        "user_id": 1843331,
                        "user_name": "Tim Castelijns",
                        "room_id": 15,
                        "room_name": "Android",
                        "message_id": 44284855,
                        "parent_id": 44284812,
                        "moved": true
                    }, {
                        "event_type": 19,
                        "time_stamp": 1539701511,
                        "content": "@TimCastelijns",
                        "id": 89978255,
                        "user_id": 1843331,
                        "user_name": "Tim Castelijns",
                        "room_id": 15,
                        "room_name": "Android",
                        "message_id": 44284861,
                        "parent_id": 44284855,
                        "moved": true
                    }, {
                        "event_type": 1,
                        "time_stamp": 1539701881,
                        "content": "\u0026rarr; \u003ci\u003e\u003ca href=\"https://chat.stackoverflow.com/transcript/message/44284848#44284848\"\u003e3 messages\u003c/a\u003e moved to \u003ca href=\"https://chat.stackoverflow.com/rooms/23262/trash-can\"\u003eTrash can\u003c/a\u003e\u003c/i\u003e",
                        "id": 89978257,
                        "user_id": 1843331,
                        "user_name": "Tim Castelijns",
                        "room_id": 15,
                        "room_name": "Android",
                        "message_id": 44284970
                    }],
                    "t": 89978259,
                    "d": 9
                }
            }
        """.trimIndent()
    }


    @Test
    fun `user profile changed event should be parsed correctly`() {
        val raw = """
            {
                "r15": {
                    "e": [{
                        "event_type": 34,
                        "time_stamp": 1539763429,
                        "id": 89999571,
                        "user_id": -2,
                        "target_user_id": 9676629,
                        "user_name": "Feeds",
                        "room_id": 15,
                        "room_name": "Android"
                    }],
                    "t": 89999571,
                    "d": 1
                }
            }
        """.trimIndent()
    }

}
