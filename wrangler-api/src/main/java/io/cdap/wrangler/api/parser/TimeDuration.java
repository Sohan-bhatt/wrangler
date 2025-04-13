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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cdap.wrangler.api.annotations.PublicEvolving;

import java.util.concurrent.TimeUnit; // Useful for time conversions
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a time duration value parsed from a directive argument (e.g., "500ms", "1.5s").
 * Stores the value canonically in nanoseconds.
 */
@PublicEvolving
public class TimeDuration implements Token {

    // Store the value in the canonical unit (e.g., nanoseconds)
    private long nanoseconds;
    // Optional: Store the original token string if needed
    private String originalToken;

    // Define patterns for parsing (adjust regex as needed)
    // Example: handles integer/decimal numbers and common units (case-insensitive)
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(\\d*\\.?\\d+)\\s*(ns|us|ms|s|m|h|d)?", Pattern.CASE_INSENSITIVE);

    /**
     * Parses the token string (e.g., "500ms", "2.1s") and stores the canonical value in nanoseconds.
     *
     * @param tokenValue The raw string token from the recipe.
     * @throws IllegalArgumentException if the token cannot be parsed.
     */
    public TimeDuration(String tokenValue) {
         this.originalToken = tokenValue;
         Matcher matcher = TIME_PATTERN.matcher(tokenValue.trim());

         if (matcher.matches()) {
            double numericValue = Double.parseDouble(matcher.group(1));
            String unit = matcher.group(2) != null ?
                        matcher.group(2).toLowerCase() : "s"; // Default to seconds if no unit

            switch (unit) {
                case "ns":
                    this.nanoseconds = (long) numericValue;
                    break;
                case "us": // Microseconds
                    this.nanoseconds = (long) (numericValue * 1_000L);
                    break;
                case "ms": // Milliseconds
                    this.nanoseconds = (long) (numericValue * 1_000_000L);
                    break;
                case "s": // Seconds
                    this.nanoseconds = (long) (numericValue * 1_000_000_000L);
                    break;
                // case "m": // Minutes
                //     this.nanoseconds = (long) (numericValue * 60L
                  //      * 1_000_000_000L);
                //     break;
                // case "h": // Hours
                //     this.nanoseconds = (long) (numericValue * 3600L
                //              * 1_000_000_000L);
                //     break;
                // case "d": // Days
                //     this.nanoseconds = (long) (numericValue * 86400L
                //              * 1_000_000_000L);
                //     break;
                default:
                    throw new IllegalArgumentException("Unknown time unit: '" + tokenValue + "'");
            }
         } else {
             throw new IllegalArgumentException("Cannot parse time duration token: '"
                        + tokenValue + "'");
         }
    }

    /**
     * Returns the canonical value (total nanoseconds) of this TimeDuration object.
     *
     * @return the value in nanoseconds as a long primitive.
     */
    @Override
    public Long value() { // Return type consistent with canonical storage
        return nanoseconds;
    }

    /**
     * Returns the specific canonical value in nanoseconds, as required by the assignment.
     * Consider if another unit like milliseconds is more appropriate based on expected usage.
     *
     * @return the value in nanoseconds.
     */
    public long getNanoseconds() {
        return nanoseconds;
    }

     /**
     * Helper method to get value in other units if needed.
     * @param unit The TimeUnit to convert to.
     * @return The value in the specified TimeUnit.
     */
    public long getValue(TimeUnit unit) {
        return unit.convert(this.nanoseconds, TimeUnit.NANOSECONDS);
    }


    /**
     * Returns the type of this object as TokenType.TIME_DURATION.
     * Make sure TokenType.TIME_DURATION is added to the TokenType enum.
     *
     * @return TokenType.TIME_DURATION
     */
    @Override
    public TokenType type() {
         // IMPORTANT: You MUST add TIME_DURATION to the TokenType enum, e.g.:
        // public enum TokenType { ..., BYTE_SIZE, TIME_DURATION; }
        return TokenType.TIME_DURATION;
    }

    /**
     * Returns the members of this object as a JsonElement.
     *
     * @return Json representation of this object.
     */
    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", TokenType.TIME_DURATION.name());
        object.addProperty("value_nanos", nanoseconds); // Store canonical value
        object.addProperty("original_token", originalToken); // Optional: include original
        return object;
    }

     @Override
    public String toString() {
        // Optional: Provide a useful string representation
        // You could convert nanos back to a more readable format here if desired
        return originalToken + " (" + nanoseconds + " ns)";
    }
}
