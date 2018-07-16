# ChatExchange

Kotlin API for StackExchange chat

This is a kotlin port of [ChatExchange](https://github.com/SOBotics/chatexchange).

Structurally it is almost completely the same as the original, hence all credit for that goes to original creator(s) and maintainer(s)
of that project, most notably [Tunaki](https://github.com/Tunaki/).

## Status

All functionality has been ported, but not everything has been fully tested.

A test file exists for this purpose (see below). Currently there are no automated tests.

## Things left to do

- __Prettify the code__

  It is currently basically the kotlin version of the java code. It would be great to convert whatever possible to idiomatic kotlin.


- __Handle unhandled events__

  Currently only a specific subset of events are handled. It would be great to support all of them, and separate them by purpose. E.g. MessageStarred and MessagePinned could be 2 separate events.

## Testing

There is a very basic set up in Test.kt that joins the sandbox room and attached event listeners.

This can be used for testing purposes, but requires a file `credentials.properties` to be present in the root with format:

    email=<email>
    password=<password>
    
Enter the credentials for the account you are using to connect to chat.