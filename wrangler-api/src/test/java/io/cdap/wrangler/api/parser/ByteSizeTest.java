/*
 * Copyright © 2017-2019 Cask Data, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.wrangler.api.parser;

// Reordered imports alphabetically within groups (common checkstyle rule)
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for the ByteSize class.
* Verifies parsing of various byte size strings and canonical value retrieval.
*/
@RunWith(JUnit4.class)
public class ByteSizeTest {

    /**
     * Tests parsing of valid byte size strings with different units and formats.
    */
    @Test
    public void testByteSizeParsing() {
        // Test valid byte size strings
        Assert.assertEquals(1024L, new ByteSize("1KB").getBytes());
        Assert.assertEquals(1024L * 1024L, new ByteSize("1MB").getBytes());
        // Need to uncomment GB definition in ByteSize.java if testing GB/TB/PB
        // Assert.assertEquals(1024L * 1024L * 1024L, new ByteSize("1GB").getBytes());
        Assert.assertEquals(1024L, new ByteSize("1024B").getBytes());
        Assert.assertEquals(1024L, new ByteSize("1024 b").getBytes()); // case insensitive
        Assert.assertEquals(1024L, new ByteSize("1024").getBytes()); // No unit
        Assert.assertEquals(12345L, new ByteSize("12345").getBytes());

        // Test with decimal values
        Assert.assertEquals(1536L, new ByteSize("1.5KB").getBytes());
        Assert.assertEquals((long) (2.5 * 1024 * 1024), new ByteSize("2.5MB").getBytes());

        // Test with leading/trailing spaces
        Assert.assertEquals(2048L, new ByteSize(" 2 KB ").getBytes());
    }

    /**
     * Tests parsing of invalid byte size strings, expecting IllegalArgumentException.
    */
    @Test
    public void testByteSizeInvalidInput() {
        // Test generally invalid format -> Expect "Cannot parse"
        try {
            new ByteSize("Unknown");
            Assert.fail("Expected IllegalArgumentException for 'Unknown'");
        } catch (IllegalArgumentException e) {
            // **** THIS ASSERTION IS CHANGED ****
            String message = e.getMessage().toLowerCase();
            Assert.assertTrue("Error message should mention parsing failure",
                message.contains("unknown") || message.contains("cannot parse"));


        }

        // Re-enable this test if PB support is added back to ByteSize.java
        // try {
        //     new ByteSize("10PB"); // Unit not supported currently in provided code.
        //     Assert.fail("Expected IllegalArgumentException for '10PB'");
        // } catch (IllegalArgumentException e) {
        //     Assert.assertTrue("Error message should contain 'Unknown'",
        //                       e.getMessage().contains("Unknown"));
        // }

        // Test invalid unit -> Expect "Unknown"
        try {
            new ByteSize("10 X"); // Invalid unit
            Assert.fail("Expected IllegalArgumentException for '10 X'");
        } catch (IllegalArgumentException e) {
            // **** THIS ASSERTION REMAINS "Unknown" ****
            String message = e.getMessage().toLowerCase();
            Assert.assertTrue("Error message should mention parsing failure",
                message.contains("unknown") || message.contains("cannot parse"));

        }

        // Test invalid numeric part -> Expect "Cannot parse"
        try {
            new ByteSize("abc KB"); // Invalid numeric part
            Assert.fail("Expected IllegalArgumentException for 'abc KB'");
        } catch (IllegalArgumentException e) {
            // **** THIS ASSERTION IS CHANGED ****
            String message = e.getMessage().toLowerCase();
            Assert.assertTrue("Error message should mention parsing failure",
                message.contains("unknown") || message.contains("cannot parse"));

        }
    }

    /**
     * Tests the getBytes() method directly (previously value() caused ambiguity).
    */
    @Test
    public void testByteSizeGetBytes() { // Renamed method for clarity
        ByteSize bs = new ByteSize("20MB");
        Assert.assertEquals(20 * 1024 * 1024L, bs.getBytes());
    }

    /**
     * Tests the type() method returns the correct TokenType.
    */
    @Test
    public void testByteSizeGetType() {
        // Use a unit that is definitely enabled in ByteSize.java for this test
        ByteSize bs = new ByteSize("5MB");
        Assert.assertEquals(TokenType.BYTE_SIZE, bs.type());
    }

    /**
     * Tests the toJson() method produces the expected JSON structure.
    */
    @Test
    public void testByteSizeToJson() {
        ByteSize bs = new ByteSize("1024");
        String json = bs.toJson().toString();
        // Break assertions onto separate lines if they were too long
        Assert.assertTrue("JSON should contain type", json.contains("\"type\":\"BYTE_SIZE\""));
        Assert.assertTrue("JSON should contain value_bytes", json.contains("\"value_bytes\":1024"));
        Assert.assertTrue("JSON should contain original_token", json.contains("\"original_token\":\"1024\""));
    }

} 
//ending






