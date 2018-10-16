package com.timcastelijns.chatexchange

import com.timcastelijns.chatexchange.chat.ChatHost
import com.timcastelijns.chatexchange.chat.StackExchangeClient
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import java.io.FileInputStream
import java.util.*
import kotlin.coroutines.experimental.CoroutineContext

fun main(args: Array<String>) {
    val job = Job()
    val coroutineContext: CoroutineContext = Dispatchers.IO + job
    val scope = CoroutineScope(coroutineContext)

    val properties = Properties()
    FileInputStream("credentials.properties").use {
        properties.load(it)
    }

    val client = StackExchangeClient(properties.getProperty("email"), properties.getProperty("password"))
    val roomIdSandbox = 15
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
        println("User notification: $it")
    }

    room.accessLevelChangedEventListener = {
        println("Access level changed: ${it.targetUser.name} level is now: ${it.accessLevel}. Action was initiated by ${it.userName}")
    }

    print("Send message by typing in the console directly. Hit enter to send.")
    while (true) {
        val message = readLine()
        if (message == "q") {
            break
        }
        scope.launch {
            room.send(message!!)
        }
    }

    job.cancel()
    client.close()
}
