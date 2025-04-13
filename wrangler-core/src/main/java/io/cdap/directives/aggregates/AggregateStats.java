/*
 *  Copyright © 2017-2019 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */
 package io.cdap.directives.aggregates;

 // CDAP annotations
 import io.cdap.cdap.api.annotation.Description;
 import io.cdap.cdap.api.annotation.Name;
 import io.cdap.cdap.api.annotation.Plugin;
 // Wrangler API imports
 import io.cdap.wrangler.api.Arguments;
 import io.cdap.wrangler.api.Directive;
 import io.cdap.wrangler.api.DirectiveExecutionException;
 import io.cdap.wrangler.api.DirectiveParseException; // Import this
 import io.cdap.wrangler.api.ExecutorContext;
 import io.cdap.wrangler.api.Optional; // Import this for optional args
 import io.cdap.wrangler.api.Row;
 import io.cdap.wrangler.api.TransientStore;
 import io.cdap.wrangler.api.TransientVariableScope;
 import io.cdap.wrangler.api.annotations.Categories;
 // Removed conflicting Wrangler Description import
 // import io.cdap.wrangler.api.annotations.Description;
 // Removed Wrangler Usage import
 // import io.cdap.wrangler.api.annotations.Usage;
 import io.cdap.wrangler.api.parser.ByteSize;   //MY API CLASS
 import io.cdap.wrangler.api.parser.ColumnName;
 import io.cdap.wrangler.api.parser.Text;
 import io.cdap.wrangler.api.parser.TimeDuration; //MY API CLASS
 import io.cdap.wrangler.api.parser.TokenType;
 import io.cdap.wrangler.api.parser.UsageDefinition;

 
 // Standard Java imports
 import java.math.BigDecimal;
 import java.math.RoundingMode;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;
 
 /**
  * An Aggregate Directive to calculate statistics (like total/average) for
  * byte size and time duration columns.
  */
 @Plugin(type = Directive.TYPE)
 @Name(AggregateStats.NAME)
 @Description(
 "Calculates aggregate statistics (total or average) for byte size and time duration columns.")
 @Categories(categories = {"aggregation"})
 // Removed @Usage annotation - definition is below
 public class AggregateStats implements Directive {
     public static final String NAME = "aggregate-stats";
 
     // Instance variables
     private String byteCol;
     private String timeCol;
     private String targetSizeCol;
     private String targetTimeCol;
     private String aggregationType = "total"; // Default
     private String sizeUnitOut = "B";         // Default
     private String timeUnitOut = "s";         // Default
 
     // Constants (BigDecimal for precision)
     private static final BigDecimal BYTES_IN_KB = BigDecimal.valueOf(1024.0);
     private static final BigDecimal BYTES_IN_MB =
                BYTES_IN_KB.multiply(BigDecimal.valueOf(1024.0));
     private static final BigDecimal BYTES_IN_GB =
            BYTES_IN_MB.multiply(BigDecimal.valueOf(1024.0));
     private static final BigDecimal BYTES_IN_TB =
                BYTES_IN_GB.multiply(BigDecimal.valueOf(1024.0));
     private static final BigDecimal BYTES_IN_PB =
                BYTES_IN_TB.multiply(BigDecimal.valueOf(1024.0));
 
     private static final BigDecimal NANOS_IN_US =
                    BigDecimal.valueOf(1_000.0);
     private static final BigDecimal NANOS_IN_MS =
                    BigDecimal.valueOf(1_000_000.0);
     private static final BigDecimal NANOS_IN_SECOND =
                    BigDecimal.valueOf(1_000_000_000.0);
     private static final BigDecimal NANOS_IN_MINUTE =
                    NANOS_IN_SECOND.multiply(BigDecimal.valueOf(60.0));
     private static final BigDecimal NANOS_IN_HOUR =
                    NANOS_IN_MINUTE.multiply(BigDecimal.valueOf(60.0));
     private static final BigDecimal NANOS_IN_DAY =
                    NANOS_IN_HOUR.multiply(BigDecimal.valueOf(24.0));
 
     // Unique keys for TransientStore
     private static final String TOTAL_BYTES_KEY = NAME + "_totalBytes";
     private static final String TOTAL_NANOS_KEY = NAME + "_totalNanos";
     private static final String COUNT_KEY = NAME + "_count";
 
     // Precision for division
     private static final int AVG_SCALE = 10;
     private static final RoundingMode AVG_ROUNDING = RoundingMode.HALF_UP;
 
     @Override
     public UsageDefinition define() {
         UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
         // Add usage string definition here - based on @Usage content previously
         //builder.usage("aggregate-stats :byte_col
                    //:time_col :target_size_col :target_time_col [type=total|average]
                    //  [size_unit=B|KB|MB|GB|TB|PB] [time_unit=ns|us|ms|s|m|h|d]");
         // Define mandatory arguments
         builder.define("byte_col", TokenType.COLUMN_NAME);
         builder.define("time_col", TokenType.COLUMN_NAME);
         builder.define("target_size_col", TokenType.COLUMN_NAME);
         builder.define("target_time_col", TokenType.COLUMN_NAME);
         // Define optional arguments using Optional.TRUE
         builder.define("type", TokenType.TEXT, Optional.TRUE); // Use Optional.TRUE
         builder.define("size_unit", TokenType.TEXT, Optional.TRUE); // Use Optional.TRUE
         builder.define("time_unit", TokenType.TEXT, Optional.TRUE); // Use Optional.TRUE
         return builder.build();
     }
 
     @Override
     // Added 'throws DirectiveParseException' based on ParseDateTime example
     public void initialize(Arguments args) throws DirectiveParseException {
         this.byteCol = ((ColumnName) args.value(
                "byte_col")).value();
         this.timeCol = ((ColumnName) args.value(
                    "time_col")).value();
         this.targetSizeCol = ((ColumnName) args.value(
                    "target_size_col")).value();
         this.targetTimeCol = ((ColumnName) args.value(
                    "target_time_col")).value();
 
         // Check for optional arguments using args.contains()
         if (args.contains("type")) {
             this.aggregationType = ((Text) args.value("type")).value().toLowerCase();
             if (!aggregationType.equals("total") && !aggregationType.equals("average")) {
                 // Throw DirectiveParseException as allowed by signature now
                 throw new DirectiveParseException(NAME,
                                    "Invalid aggregation type '" + this.aggregationType
                                                   + "'. Must be 'total' or 'average'.");
             }
         } // Defaults used if not present
 
         if (args.contains("size_unit")) {
             this.sizeUnitOut = ((Text) args.value(
                        "size_unit")).value().toUpperCase();
              if (!List.of("B", "KB", "MB", "GB", "TB", "PB").contains(this.sizeUnitOut)) {
                  throw new DirectiveParseException(NAME,
                                    "Unsupported output size unit: " + this.sizeUnitOut);
              }
         } // Defaults used if not present
 
         if (args.contains("time_unit")) {
             this.timeUnitOut = ((Text) args.value("time_unit")).value().toLowerCase();
             if (!List.of("ns", "us", "ms", "s", "m", "h", "d").contains(this.timeUnitOut)) {
                 throw new DirectiveParseException(NAME,
                                "Unsupported output time unit: " + this.timeUnitOut);
             }
         } // Defaults used if not present
     }
 
     @Override
     // Keep throws DirectiveExecutionException as execute can fail during processing
     public List<Row> execute(List<Row> rows,
                        ExecutorContext context) throws DirectiveExecutionException {
         TransientStore store = context.getTransientStore();
 
         // Retrieve current values using get() and handling null
         Object currentTotalBytesVal = store.get(TOTAL_BYTES_KEY);
         BigDecimal totalBytes = (currentTotalBytesVal == null)
                    ? BigDecimal.ZERO : (BigDecimal) currentTotalBytesVal;
 
         Object currentTotalNanosVal = store.get(TOTAL_NANOS_KEY);
         BigDecimal totalNanos = (currentTotalNanosVal == null)
                        ? BigDecimal.ZERO : (BigDecimal) currentTotalNanosVal;
 
         Object currentCountVal = store.get(COUNT_KEY);
         long count = (currentCountVal == null) ? 0L : (Long) currentCountVal;
 
         for (Row row : rows) {
             int byteColIdx = row.find(byteCol);
             int timeColIdx = row.find(timeCol);
 
             if (byteColIdx != -1 && timeColIdx != -1) {
                 Object byteVal = row.getValue(byteColIdx);
                 Object timeVal = row.getValue(timeColIdx);
                 boolean processedRow = false;
 
                 try { // Add try-catch for potential ClassCastException or other runtime issues
                     if (byteVal instanceof ByteSize) {
                         totalBytes = totalBytes.add(
                                    BigDecimal.valueOf(((ByteSize) byteVal).getBytes()));
                         processedRow = true;
                     }
                     if (timeVal instanceof TimeDuration) {
                          totalNanos = totalNanos.add(
                                        BigDecimal.valueOf(((TimeDuration) timeVal).getNanoseconds()));
                          processedRow = true;
                     }
                      if (processedRow) {
                          count++;
                      }
                 } catch (Exception e) {
                     // Handle unexpected errors during processing if needed
                     throw new DirectiveExecutionException(NAME,
                                        "Error processing row: " + e.getMessage(), e);
                 }
             }
         }
 
         // Store updated aggregates using correct set() signature (Scope, Key, Value)
         store.set(TransientVariableScope.GLOBAL, TOTAL_BYTES_KEY, totalBytes);
         store.set(TransientVariableScope.GLOBAL, TOTAL_NANOS_KEY, totalNanos);
         store.set(TransientVariableScope.GLOBAL, COUNT_KEY, count);
 
         return new ArrayList<>(); // Return empty list from execute for aggregates
     }
 
     // **** finish() method is intentionally REMOVED ****
     // The framework likely handles retrieving the final values from the
     // TransientStore and creating the output row for aggregate directives.
     // If testing reveals this is wrong, we'll need to find the correct finalization hook.
 
 
     // Helper methods remain the same
     private double convertBytes(BigDecimal bytes, String unit) throws DirectiveExecutionException {
          switch (unit) {
              case "B":  return bytes.doubleValue();
              case "KB": return bytes.divide(
                            BYTES_IN_KB, AVG_SCALE, AVG_ROUNDING).doubleValue();
              case "MB": return bytes.divide(
                            BYTES_IN_MB, AVG_SCALE, AVG_ROUNDING).doubleValue();
              case "GB": return bytes.divide(
                            BYTES_IN_GB, AVG_SCALE, AVG_ROUNDING).doubleValue();
              case "TB": return bytes.divide(
                            BYTES_IN_TB, AVG_SCALE, AVG_ROUNDING).doubleValue();
              case "PB": return bytes.divide(
                            BYTES_IN_PB, AVG_SCALE, AVG_ROUNDING).doubleValue();
              default:
                  throw new DirectiveExecutionException(NAME, "Unsupported output size unit: " + unit);
          }
     }
 
     private double convertNanos(BigDecimal nanos, String unit) throws DirectiveExecutionException {
          switch (unit) {
             case "ns": return nanos.doubleValue();
             case "us": return nanos.divide(
                            NANOS_IN_US, AVG_SCALE, AVG_ROUNDING).doubleValue();
             case "ms": return nanos.divide(
                            NANOS_IN_MS, AVG_SCALE, AVG_ROUNDING).doubleValue();
             case "s":  return nanos.divide(
                            NANOS_IN_SECOND, AVG_SCALE, AVG_ROUNDING).doubleValue();
             case "m":  return nanos.divide(
                            NANOS_IN_MINUTE, AVG_SCALE, AVG_ROUNDING).doubleValue();
             case "h":  return nanos.divide(
                            NANOS_IN_HOUR, AVG_SCALE, AVG_ROUNDING).doubleValue();
             case "d":  return nanos.divide(
                            NANOS_IN_DAY, AVG_SCALE, AVG_ROUNDING).doubleValue();
              default:
                  throw new DirectiveExecutionException(NAME,
                            "Unsupported output time unit: " + unit);
          }
     }
 
     @Override
     public void destroy() {
         // No resources to clean up
     }
 }
