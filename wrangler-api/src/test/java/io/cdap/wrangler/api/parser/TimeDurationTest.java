/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.cdap.wrangler.api.parser;

// Reordered imports alphabetically within groups
import org.junit.Assert; 
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.util.concurrent.TimeUnit;
/**
 * Unit tests for the TimeDuration class.
 * Verifies parsing of various time duration strings and canonical value retrieval.
 */
@RunWith(JUnit4.class)
public class TimeDurationTest {

    /**
     * Tests parsing of valid time duration strings with different units and formats.
     */
    @Test
    public void testTimeDurationParsing() {
        // Test valid time duration strings
        Assert.assertEquals(500_000_000L, new TimeDuration("500ms").getNanoseconds());
        Assert.assertEquals(1_500_000_000L, new TimeDuration("1.5s").getNanoseconds());
        // Assuming m, h, d are handled in TimeDuration.java (they were commented out before)
        // Assert.assertEquals(120_000_000_000L, new TimeDuration("2m").getNanoseconds());
        // Assert.assertEquals(7200_000_000_000L, new TimeDuration("2h").getNanoseconds());
        Assert.assertEquals(1000L, new TimeDuration("1us").getNanoseconds());
        Assert.assertEquals(1L, new TimeDuration("1ns").getNanoseconds());
        // Assert.assertEquals(86400_000_000_000L, new TimeDuration("1d").getNanoseconds());
        Assert.assertEquals(2000_000_000L, new TimeDuration("2 s").getNanoseconds()); //spaces

        // Test with decimal values
        Assert.assertEquals(750_000_000L, new TimeDuration("0.75s").getNanoseconds());
        // Assert.assertEquals(90_000_000_000L, new TimeDuration("1.5m").getNanoseconds());

        // Test with leading/trailing spaces
        // Assert.assertEquals(60_000_000_000L, new TimeDuration(" 1 m ").getNanoseconds());
    }

    /**
     * Tests parsing of invalid time duration strings, expecting IllegalArgumentException.
     */
    @Test
    public void testTimeDurationInvalidInput() {
        // Test invalid time duration strings. Check for the specific exception.
        try {
            new TimeDuration("Unknown");
            Assert.fail("Expected IllegalArgumentException for 'invalid'");
        } catch (IllegalArgumentException e) {
            String message = e.getMessage().toLowerCase();
            Assert.assertTrue("Error message should mention parsing failure",
                message.contains("unknown") || message.contains("cannot parse"));
        }

        try {
            new TimeDuration("10 years"); // Unit not supported
            Assert.fail("Expected IllegalArgumentException for '10 years'");
        } catch (IllegalArgumentException e) {
            String message = e.getMessage().toLowerCase();
            Assert.assertTrue("Error message should mention parsing failure",
                message.contains("unknown") || message.contains("cannot parse"));
        }

        try {
            new TimeDuration("abc s"); // Invalid numeric part
            Assert.fail("Expected IllegalArgumentException for 'abc s'");
        } catch (IllegalArgumentException e) {
            String message = e.getMessage().toLowerCase();
            Assert.assertTrue("Error message should mention parsing failure",
                message.contains("unknown") || message.contains("cannot parse"));
        }
    }

    /**
     * Tests the getNanoseconds() method directly (previously value() caused ambiguity).
     */
    @Test
    public void testTimeDurationGetNanoseconds() { // Renamed method for clarity
        // Use units confirmed to be handled
        TimeDuration td = new TimeDuration("2s");
        Assert.assertEquals(2 * 1_000_000_000L, td.getNanoseconds());
    }

     /**
      * Tests the helper getValue(TimeUnit) method.
      */
     @Test
    public void testTimeDurationGetValue() {
        TimeDuration td = new TimeDuration("2s");
        Assert.assertEquals(2000L, td.getValue(TimeUnit.MILLISECONDS)); // Added L for long literal
        Assert.assertEquals(2L, td.getValue(TimeUnit.SECONDS));     // Added L
        Assert.assertEquals(2000000000L, td.getValue(TimeUnit.NANOSECONDS)); // Added L
    }

    /**
     * Tests the type() method returns the correct TokenType.
     */
    @Test
    public void testTimeDurationGetType() {
        // Use units confirmed to be handled
        TimeDuration td = new TimeDuration("1s");
        Assert.assertEquals(TokenType.TIME_DURATION, td.type());
    }

    /**
     * Tests the toJson() method produces the expected JSON structure.
     */
    @Test
    public void testTimeDurationToJson() {
        TimeDuration td = new TimeDuration("5ms");
        String json = td.toJson().toString();
        // Break assertions onto separate lines
        Assert.assertTrue("JSON should contain type",
                          json.contains("\"type\":\"TIME_DURATION\""));
        Assert.assertTrue("JSON should contain value_nanos",
                          json.contains("\"value_nanos\":5000000"));
        Assert.assertTrue("JSON should contain original_token",
                          json.contains("\"original_token\":\"5ms\""));
    }
}

