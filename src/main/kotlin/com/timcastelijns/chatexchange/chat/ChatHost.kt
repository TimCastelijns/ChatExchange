package com.timcastelijns.chatexchange.chat

enum class ChatHost(val hostName: String) {

    STACK_OVERFLOW("stackoverflow.com"),
    STACK_EXCHANGE("stackexchange.com"),
    META_STACK_EXCHANGE("meta.stackexchange.com");

    var baseUrl = "https://chat.$hostName"
}
