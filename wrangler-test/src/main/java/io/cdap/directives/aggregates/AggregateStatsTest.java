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


package io.cdap.directives.aggregates;

import io.cdap.wrangler.TestingRig;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientStore;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for the AggregateStats directive.
 */
@RunWith(JUnit4.class)
public class AggregateStatsTest {

    @Test
    public void testAggregateStatsTotal() throws Exception {
        // Create sample input rows with ByteSize and TimeDuration values
        List<Row> inputRows = new ArrayList<>();
        inputRows.add(new Row("data_size", new ByteSize("10KB"), "response_time", new TimeDuration("500ms")));
        inputRows.add(new Row("data_size", new ByteSize("2MB"), "response_time", new TimeDuration("1s")));
        inputRows.add(new Row("data_size", new ByteSize("500B"), "response_time", new TimeDuration("200ms")));

        // Define the recipe using the aggregate-stats directive
        String[] recipe = new String[] {
            "aggregate-stats :data_size :response_time total_size_b total_time_s"
        };

        // Execute the recipe using TestingRig
        List<Row> results = TestingRig.execute(recipe, inputRows);

        // Assert that the output contains a single row
        Assert.assertEquals(1, results.size());
        Row outputRow = results.get(0);

        // Assert the aggregated values (total size in bytes, total time in seconds)
        Assert.assertEquals(2048 + 2 * 1024 * 1024 + 500, outputRow.getValue("total_size_b")); // 2050348
        Assert.assertEquals(0.5 + 1.0 + 0.2, outputRow.getValue("total_time_s")); // 1.7
    }

    @Test
    public void testAggregateStatsAverage() throws Exception {
        // Create sample input rows
        List<Row> inputRows = new ArrayList<>();
        inputRows.add(new Row("data_size", new ByteSize("1KB"), "response_time", new TimeDuration("1s")));
        inputRows.add(new Row("data_size", new ByteSize("2KB"), "response_time", new TimeDuration("2s")));
        inputRows.add(new Row("data_size", new ByteSize("3KB"), "response_time", new TimeDuration("3s")));

        // Define the recipe using the aggregate-stats directive with type=average
        String[] recipe = new String[] {
            "aggregate-stats :data_size :response_time avg_size_kb avg_time_s type=average"
        };

        // Execute the recipe
        List<Row> results = TestingRig.execute(recipe, inputRows);

        // Assert the output contains a single row
        Assert.assertEquals(1, results.size());
        Row outputRow = results.get(0);

        // Assert the aggregated values (average size in KB, average time in seconds)
        Assert.assertEquals((1 + 2 + 3), outputRow.getValue("avg_size_kb")); // 2
        Assert.assertEquals((1.0 + 2.0 + 3.0) / 3.0, outputRow.getValue("avg_time_s")); // 2.0
    }

    @Test
    public void testAggregateStatsDifferentUnits() throws Exception {
        // Test with different input and output units
        List<Row> inputRows = new ArrayList<>();
        inputRows.add(new Row("data_size", new ByteSize("1024B"), "response_time", new TimeDuration("2000ms")));
        inputRows.add(new Row("data_size", new ByteSize("2048B"), "response_time", new TimeDuration("3s")));

        String[] recipe = new String[] {
            "aggregate-stats :data_size :response_time total_size_mb total_time_m size_unit=MB time_unit=m"
        };

        List<Row> results = TestingRig.execute(recipe, inputRows);
        Assert.assertEquals(1, results.size());
        Row outputRow = results.get(0);

        // Assert the aggregated values (total size in MB, total time in minutes)
        Assert.assertEquals((1024.0 + 2048.0) / (1024.0 * 1024.0), outputRow.getValue("total_size_mb")); // 0.0029296875
        Assert.assertEquals((2.0 + 3.0) / 60.0, outputRow.getValue("total_time_m")); // 0.08333333333333333
    }
}