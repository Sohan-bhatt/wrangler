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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cdap.wrangler.api.annotations.PublicEvolving;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
// Consider adding necessary imports if using specific libraries for parsing

/**
 * Represents a byte size value parsed from a directive argument (e.g., "10KB", "1.5MB").
 * Stores the value canonically in bytes.
 */
@PublicEvolving
public class ByteSize implements Token {

    // Store the value in the canonical unit (bytes)
    private long bytes;
    // Optional: Store the original token string if needed
    private String originalToken;

    // Define patterns for parsing (adjust regex as needed for flexibility)
    // Example: handles integer/decimal numbers and common units (case-insensitive)
    private static final Pattern BYTE_PATTERN = Pattern.compile(
                "(\\d*\\.?\\d+)\\s*([KMGTP]?B?)", Pattern.CASE_INSENSITIVE);
    private static final long KB = 1024;
    private static final long MB = KB * 1024;
    // private static final long GB = MB * 1024;
    // private static final long TB = GB * 1024;
    // private static final long PB = TB * 1024;


    /**
     * Parses the token string (e.g., "10KB", "1.5MB") and stores the canonical value in bytes.
     *
     * @param tokenValue The raw string token from the recipe.
     * @throws IllegalArgumentException if the token cannot be parsed.
     */
    public ByteSize(String tokenValue) {
        this.originalToken = tokenValue;
        Matcher matcher = BYTE_PATTERN.matcher(tokenValue.trim());

        if (matcher.matches()) {
            double numericValue = Double.parseDouble(matcher.group(1));
            String unit = matcher.group(2).toUpperCase();

            switch (unit) {
                case "B":
                case "": // Assume bytes if no unit
                    this.bytes = (long) numericValue;
                    break;
                case "KB":
                    this.bytes = (long) (numericValue * KB);
                    break;
                case "MB":
                    this.bytes = (long) (numericValue * MB);
                    break;
                // case "GB":
                //     this.bytes = (long) (numericValue * GB);
                //     break;
                // case "TB":
                //     this.bytes = (long) (numericValue * TB);
                //     break;
                //  case "PB":
                //     this.bytes = (long) (numericValue * PB);
                //     break;
                // Add other units like KiB, MiB if needed
                default:
                    throw new IllegalArgumentException(
                    "Unknown or unsupported byte unit in token: '" + tokenValue + "'");
            }
        } else {
            throw new IllegalArgumentException("Cannot parse byte size token: '" + tokenValue + "'");
        }
    }

    /**
     * Returns the canonical value (total bytes) of this ByteSize object.
     *
     * @return the value in bytes as a long primitive.
     */
    @Override
    public Long value() { // Return type consistent with canonical storage
        return bytes;
    }

    /**
     * Returns the specific canonical value in bytes, as required by the assignment.
     *
     * @return the value in bytes.
     */
    public long getBytes() {
        return bytes;
    }

    /**
     * Returns the type of this object as TokenType.BYTE_SIZE.
     * Make sure TokenType.BYTE_SIZE is added to the TokenType enum.
     *
     * @return TokenType.BYTE_SIZE
     */
    @Override
    public TokenType type() {
        // IMPORTANT: You MUST add BYTE_SIZE to the TokenType enum, e.g.:
        // public enum TokenType { ..., BYTE_SIZE, TIME_DURATION; }
        return TokenType.BYTE_SIZE;
    }

    /**
     * Returns the members of this object as a JsonElement.
     *
     * @return Json representation of this object.
     */
    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", TokenType.BYTE_SIZE.name());
        object.addProperty("value_bytes", bytes); // Store canonical value
        object.addProperty("original_token", originalToken); // Optional: include original
        return object;
    }

     @Override
    public String toString() {
        // Optional: Provide a useful string representation
        return originalToken + " (" + bytes + " bytes)";
    }
}
