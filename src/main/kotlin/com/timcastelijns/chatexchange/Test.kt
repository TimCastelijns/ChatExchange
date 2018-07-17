package com.timcastelijns.chatexchange

import com.timcastelijns.chatexchange.chat.ChatHost
import com.timcastelijns.chatexchange.chat.StackExchangeClient
import java.io.FileInputStream
import java.util.*
import java.util.concurrent.CountDownLatch

fun main(args: Array<String>) {
    val properties = Properties()
    FileInputStream("credentials.properties").use {
        properties.load(it)
    }

    val client = StackExchangeClient(properties.getProperty("email"), properties.getProperty("password"))
    val roomIdSandbox = 1
    val countDownLatch = CountDownLatch(1)
    val room = client.joinRoom(ChatHost.STACK_OVERFLOW, roomIdSandbox)

    room.messagePostedEventListener = {
        println("Message posted by ${it.userName}: ${it.message.content}")
    }

    room.messageEditedEventListener = {
        println("Message edited by ${it.userName}: ${it.message.content}")
    }

    room.messageDeletedEventListener = {
        println("Message deleted by ${it.userName}: ${it.message.content}")
    }

    room.messageStarredEventListener = {
        println("Message starred. '${it.message.content}' now has ${it.message.starCount} stars")
    }

    room.messageRepliedToEventListener = {
        println("Message replied to by ${it.userName}: ${it.message.content}")
    }

    room.userEnteredEventListener = {
        println("User entered: ${it.userName}")
    }

    room.userLeftEventListener = {
        println("User left: ${it.userName}")
    }

    room.userMentionedEventListener = {
        println("User mentioned: ${it.userName}")
    }

    room.userNotificationEventListener = {
        println("User notification: ${it.content}")
    }

    client.use { _ ->
        countDownLatch.await()
    }
}