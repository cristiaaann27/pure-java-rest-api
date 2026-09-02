package com.cristiannustes.http;

import static com.cristiannustes.testing.Assertions.assertEquals;
import static com.cristiannustes.testing.Assertions.assertThrows;
import static com.cristiannustes.testing.Assertions.assertTrue;

import java.util.List;

import com.cristiannustes.http.error.BadRequestException;
import com.cristiannustes.testing.Test;

public class QueryParamsTest {

    @Test
    public void acceptsAParameterWithoutAValue() {
        QueryParams params = QueryParams.parse("verbose");

        assertEquals("", params.first("verbose").orElseThrow());
    }

    @Test
    public void keepsEveryValueOfARepeatedParameter() {
        QueryParams params = QueryParams.parse("tag=a&tag=b");

        assertEquals(List.of("a", "b"), params.values().get("tag"));
        assertEquals("a", params.first("tag").orElseThrow());
    }

    @Test
    public void decodesPercentEscapes() {
        QueryParams params = QueryParams.parse("name=Marcin%20Pi%C4%85tkowski");

        assertEquals("Marcin Piątkowski", params.first("name").orElseThrow());
    }

    @Test
    public void returnsAnEmptyMapForAnAbsentQuery() {
        assertTrue(QueryParams.parse(null).values().isEmpty(), "no query means no parameters");
        assertTrue(QueryParams.parse("").values().isEmpty(), "an empty query means no parameters");
    }

    @Test
    public void fallsBackWhenAParameterIsMissingOrEmpty() {
        assertEquals(20, QueryParams.parse("").intOrDefault("size", 20, 1, 100));
        assertEquals(20, QueryParams.parse("size=").intOrDefault("size", 20, 1, 100));
        assertEquals(5, QueryParams.parse("size=5").intOrDefault("size", 20, 1, 100));
    }

    @Test
    public void rejectsGarbageInsteadOfCrashing() {
        assertThrows(BadRequestException.class,
            () -> QueryParams.parse("size=abc").intOrDefault("size", 20, 1, 100));
        assertThrows(BadRequestException.class,
            () -> QueryParams.parse("size=1000").intOrDefault("size", 20, 1, 100));
    }
}
