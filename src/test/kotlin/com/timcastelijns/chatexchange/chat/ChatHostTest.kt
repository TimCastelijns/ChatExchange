package com.timcastelijns.chatexchange.chat

import org.junit.Test
import kotlin.test.assertEquals

class ChatHostTest {

    @Test
    fun `stack overflow baseurl should be correct`() {
        val expected = "https://chat.stackoverflow.com"
        assertEquals(expected, ChatHost.STACK_OVERFLOW.baseUrl)
    }

    @Test
    fun `stack exchange baseurl should be correct`() {
        val expected = "https://chat.stackexchange.com"
        assertEquals(expected, ChatHost.STACK_EXCHANGE.baseUrl)
    }

    @Test
    fun `meta stack exchange baseurl should be correct`() {
        val expected = "https://chat.meta.stackexchange.com"
        assertEquals(expected, ChatHost.META_STACK_EXCHANGE.baseUrl)
    }

}

