package com.articulate.sigma;

import com.articulate.sigma.parsing.*;
import org.junit.Test;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public class CLIMapParserTest {

    // -------------------------------------------------------------------------
    // Long-option (--) tests
    // -------------------------------------------------------------------------

    @Test
    public void testLongFlagOnly() {
        Map<String, List<String>> result = CLIMapParser.parse(new String[]{"--verbose"});
        assertTrue(result.containsKey("verbose"));
        assertTrue(result.get("verbose").isEmpty());
    }

    @Test
    public void testLongOptionWithSingleValue() {
        Map<String, List<String>> result = CLIMapParser.parse(new String[]{"--output", "results.txt"});
        assertEquals(List.of("results.txt"), result.get("output"));
    }

    @Test
    public void testLongOptionWithMultipleValues() {
        Map<String, List<String>> result = CLIMapParser.parse(
                new String[]{"--many", "foo", "bar", "baz"});
        assertEquals(List.of("foo", "bar", "baz"), result.get("many"));
    }

    @Test
    public void testMultipleLongOptions() {
        Map<String, List<String>> result = CLIMapParser.parse(
                new String[]{"--output", "out.txt", "--verbose", "--level", "3"});
        assertEquals(List.of("out.txt"), result.get("output"));
        assertTrue(result.containsKey("verbose"));
        assertEquals(List.of("3"), result.get("level"));
    }

    // -------------------------------------------------------------------------
    // Short-option (-) tests
    // -------------------------------------------------------------------------

    @Test
    public void testSingleShortFlag() {
        Map<String, List<String>> result = CLIMapParser.parse(new String[]{"-h"});
        assertTrue(result.containsKey("h"));
        assertTrue(result.get("h").isEmpty());
    }

    @Test
    public void testBundledShortFlags() {
        Map<String, List<String>> result = CLIMapParser.parse(new String[]{"-avr"});
        assertTrue(result.containsKey("a"));
        assertTrue(result.containsKey("v"));
        assertTrue(result.containsKey("r"));
    }

    @Test
    public void testBundledShortFlagsLastReceivesValue() {
        // Only the last letter in a bundle receives trailing values
        Map<String, List<String>> result = CLIMapParser.parse(new String[]{"-xvf", "archive.tar"});
        assertTrue(result.containsKey("x"));
        assertTrue(result.containsKey("v"));
        assertEquals(List.of("archive.tar"), result.get("f"));
    }

    @Test
    public void testShortFlagWithValue() {
        Map<String, List<String>> result = CLIMapParser.parse(new String[]{"-g", "Animal", "subclass"});
        assertEquals(List.of("Animal", "subclass"), result.get("g"));
    }

    // -------------------------------------------------------------------------
    // Positional argument tests
    // -------------------------------------------------------------------------

    @Test
    public void testPositionalArguments() {
        Map<String, List<String>> result = CLIMapParser.parse(new String[]{"file1.txt", "file2.txt"});
        assertEquals(List.of("file1.txt", "file2.txt"), result.get("_positional"));
    }

    @Test
    public void testMixedFlagsAndPositional() {
        Map<String, List<String>> result = CLIMapParser.parse(
                new String[]{"positionalArg", "--verbose"});
        assertEquals(List.of("positionalArg"), result.get("_positional"));
        assertTrue(result.containsKey("verbose"));
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    public void testEmptyArgs() {
        Map<String, List<String>> result = CLIMapParser.parse(new String[]{});
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDuplicateLongOptionAccumulatesValues() {
        // putIfAbsent means a second --output is treated as a new key only on first occurrence;
        // values after the second flag go to the second key's list
        Map<String, List<String>> result = CLIMapParser.parse(
                new String[]{"--output", "first.txt", "--output", "second.txt"});
        // Second --output resets lastKey but putIfAbsent keeps the original list,
        // so "second.txt" is appended to the existing list
        assertTrue(result.get("output").contains("first.txt"));
        assertTrue(result.get("output").contains("second.txt"));
    }

    @Test
    public void testOrderIndependence() {
        String[] order1 = {"--verbose", "--output", "out.txt", "--level", "3"};
        String[] order2 = {"--output", "out.txt", "--level", "3", "--verbose"};
        Map<String, List<String>> r1 = CLIMapParser.parse(order1);
        Map<String, List<String>> r2 = CLIMapParser.parse(order2);
        assertEquals(r1.get("output"), r2.get("output"));
        assertEquals(r1.get("level"), r2.get("level"));
        assertEquals(r1.containsKey("verbose"), r2.containsKey("verbose"));
    }
}
