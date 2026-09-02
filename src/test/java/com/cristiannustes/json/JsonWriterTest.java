package com.cristiannustes.json;

import static com.cristiannustes.testing.Assertions.assertEquals;
import static com.cristiannustes.testing.Assertions.assertThrows;

import com.cristiannustes.testing.Test;

public class JsonWriterTest {

    @Test
    public void writesObjectsInInsertionOrder() {
        JsonObject document = JsonObject.builder()
            .put("id", "42")
            .put("count", 3)
            .put("enabled", false)
            .build();

        assertEquals("{\"id\":\"42\",\"count\":3,\"enabled\":false}", document.toJson());
    }

    @Test
    public void escapesQuotesBackslashesAndControlCharacters() {
        JsonObject document = JsonObject.builder()
            .put("text", "say \"hi\"\\ now\u0001")
            .build();

        assertEquals("{\"text\":\"say \\\"hi\\\"\\\\ now\\u0001\"}", document.toJson());
    }

    @Test
    public void writesWholeNumbersWithoutADecimalPart() {
        assertEquals("{\"total\":10}", JsonObject.builder().put("total", 10).build().toJson());
    }

    @Test
    public void survivesARoundTrip() {
        String original = "{\"a\":[1,{\"b\":\"c\"}],\"d\":true,\"e\":null}";

        assertEquals(original, JsonParser.parse(original).toJson());
    }

    @Test
    public void refusesValuesJsonCannotRepresent() {
        assertThrows(JsonException.class, () -> new JsonNumber(Double.NaN));
        assertThrows(JsonException.class, () -> new JsonNumber(Double.POSITIVE_INFINITY));
    }
}
