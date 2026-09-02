package com.cristiannustes.json;

import static com.cristiannustes.testing.Assertions.assertEquals;
import static com.cristiannustes.testing.Assertions.assertThrows;
import static com.cristiannustes.testing.Assertions.assertTrue;

import com.cristiannustes.testing.Test;

public class JsonParserTest {

    @Test
    public void readsAnObjectWithEveryKindOfValue() {
        JsonObject parsed = JsonParser.parseObject("""
            {"login":"marcin","age":37,"active":true,"tags":["a","b"],"address":null}
            """);

        assertEquals("marcin", parsed.requireString("login"));
        assertEquals(37L, parsed.optionalLong("age").orElseThrow());
        assertEquals(JsonBoolean.TRUE, parsed.find("active").orElseThrow());
        assertEquals(2, parsed.requireArray("tags").size());
        assertTrue(parsed.find("address").isEmpty(), "an explicit null should read as absent");
    }

    @Test
    public void decodesEscapeSequences() {
        JsonObject parsed = JsonParser.parseObject("{\"text\":\"line\\nquote\\\"tab\\tstar\\u002A\"}");

        assertEquals("line\nquote\"tab\tstar*", parsed.requireString("text"));
    }

    @Test
    public void readsNumbersInScientificNotation() {
        JsonValue parsed = JsonParser.parse("[1e3,-2.5,0]");

        assertEquals("[1000,-2.5,0]", parsed.toJson());
    }

    @Test
    public void rejectsTrailingContent() {
        JsonException failure = assertThrows(JsonException.class, () -> JsonParser.parse("{} garbage"));

        assertTrue(failure.getMessage().contains("trailing content"), failure.getMessage());
    }

    @Test
    public void rejectsDuplicateFields() {
        assertThrows(JsonException.class, () -> JsonParser.parse("{\"a\":1,\"a\":2}"));
    }

    @Test
    public void rejectsUnescapedControlCharacters() {
        assertThrows(JsonException.class, () -> JsonParser.parse("{\"a\":\"one\ntwo\"}"));
    }

    @Test
    public void rejectsTrailingCommas() {
        assertThrows(JsonException.class, () -> JsonParser.parse("[1,2,]"));
    }

    @Test
    public void refusesToRecurseWithoutBound() {
        String bomb = "[".repeat(500) + "]".repeat(500);

        JsonException failure = assertThrows(JsonException.class, () -> JsonParser.parse(bomb));

        assertTrue(failure.getMessage().contains("nested deeper"), failure.getMessage());
    }

    @Test
    public void rejectsARootThatIsNotAnObject() {
        assertThrows(JsonException.class, () -> JsonParser.parseObject("[1,2]"));
    }
}
