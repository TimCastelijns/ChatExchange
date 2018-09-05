import com.google.gson.JsonParser
import com.timcastelijns.chatexchange.chat.AccessLevelChangedEvent
import com.timcastelijns.chatexchange.chat.StackExchangeEventType
import org.junit.Test

class Atest {

    @Test
    fun test() {
        val event = "{\"r15\":{\"e\":[{\"event_type\":15,\"time_stamp\":1536054643,\"content\":\"Access now request\",\"id\":88959114,\"user_id\":7495506,\"target_user_id\":7495506,\"user_name\":\"SkynetTester\",\"room_id\":15,\"room_name\":\"Android\"},{\"event_type\":16,\"time_stamp\":1536054643,\"content\":\"\\u003ca href=\\\"/users/7495506/skynettester\\\"\\u003eSkynetTester\\u003c/a\\u003e has requested access to \\u003ca href=\\\"/rooms/info/15?tab=access\\\"\\u003eAndroid\\u003c/a\\u003e.\",\"id\":88959121,\"user_id\":7495506,\"target_user_id\":9676629,\"user_name\":\"SkynetTester\"}],\"t\":88959127,\"d\":14}}\n"
        val json = JsonParser().parse(event)

        val o = json.asJsonObject.entrySet().filter { it.key == "r15" }
                .map { it.value }
                .map { it.asJsonObject }
                .mapNotNull { it.get("e") }
                .map { it.asJsonArray }
                .firstOrNull()!!
                .map { it.asJsonObject }
                .filter {
                    (!it.has("user_id") || it.get("user_id").asLong > 0) &&
                            it.get("room_id")?.asInt == 15
                }
                .mapNotNull {
                    when (StackExchangeEventType.fromCode(it.get("event_type").asInt)) {
                        StackExchangeEventType.ACCESS_LEVEL_CHANGED -> "success"
                        else -> null
                    }
                }
                .toList()


        println(o)
    }

}