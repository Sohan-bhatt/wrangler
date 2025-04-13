# Wrangler Enhancements: Byte Size and Time Duration Units

This document outlines enhancements made to the **CDAP Wrangler library** as part of the **Software Engineer Intern Assignment**. The primary objective was to add native support for parsing **byte size** (e.g., KB, MB) and **time duration** (e.g., ms, s) units in recipes, along with implementing a new aggregation directive that utilizes these units.

---

## ✨ Implemented Features

### 1. Grammar and Parser Updates (ANTLR & Core)

- **Lexer Rules Added (`Directives.g4`)**
  - Introduced `BYTE_SIZE` and `TIME_DURATION` tokens.
  - Used supporting fragments: `BYTE_UNIT` (e.g., B, KB, MB) and `TIME_UNIT` (e.g., ms, s, m, h).
  - Added a helper rule for matching integers (`INTEGER`).

- **Parser Rule Enhanced**
  - Updated the `value` parser rule to include `BYTE_SIZE` and `TIME_DURATION` as valid types.

- **Core Visitor Logic (`RecipeVisitor.java`)**
  - Implemented logic in the `visitValue()` method to:
    - Identify `BYTE_SIZE` or `TIME_DURATION` tokens.
    - Instantiate the appropriate `ByteSize` or `TimeDuration` object.
    - Add the object to the corresponding `TokenGroup`.

### 2. API Updates (`wrangler-api` Module)

- **New Classes in `io.cdap.wrangler.api.parser`:**
  - `ByteSize.java`
    - Parses values like "10MB", stores value in bytes.
    - Method: `getBytes()`.
  - `TimeDuration.java`
    - Parses values like "2.5s", stores value in nanoseconds.
    - Methods: `getNanoseconds()`, `getValue(TimeUnit)`.

- **Enum Update**
  - `TokenType.java`: Added new constants `BYTE_SIZE` and `TIME_DURATION`.

### 3. New Directive: `AggregateStats`

- **Path**: `wrangler-core/src/main/java/io/cdap/directives/aggregates/AggregateStats.java`
- **Purpose**: Aggregates columns containing byte size and time duration values.

#### Arguments
- **Required:**
  - `:byte_col`, `:time_col`, `:target_size_col`, `:target_time_col`
- **Optional:**
  - `type` ("total" or "average", default: "total")
  - `size_unit` (default: B)
  - `time_unit` (default: s)

#### Implementation
- `initialize()` handles argument parsing and validation.
- `execute()` uses `TransientStore` and `ExecutorContext` to:
  - Aggregate size and time values.
  - Count rows.
- The `finish()` method was excluded due to interface constraints.
- Helpers `convertBytes()` and `convertNanos()` assist with unit conversions.

### 4. Testing Suite

- **Unit Tests (API Module):**
  - `ByteSizeTest.java`, `TimeDurationTest.java`: Validate parsing and value conversion.

- **Unit Tests (Core Module):**
  - `AggregateStatsTest.java`: Uses `TestingRig` for recipe simulation.

- **Note:** Existing test failures in unrelated areas (e.g., `ConvertStringTest`, `ValidateStandardTest`) are under review and not caused by the new functionality.

---

## 📄 Usage Examples

```wrangler
# Example 1: Total size (MB) and time (s)
aggregate-stats :data_transfer_size :response_time :total_size :total_time type=total size_unit=MB time_unit=s

# Example 2: Average size (KB) and time (ms)
aggregate-stats :size_col :duration_col :avg_size_kb :avg_time_ms type=average size_unit=KB time_unit=ms
```

```wrangler
# Example: Direct set-column usage
set-column :myBytes '150MB'
set-column :myDuration '2.5s'
```

---

## 📊 Build & Environment Notes

- **Build Command**: From the root:
```bash
mvn clean install
```

### Troubleshooting Summary
- **Common Issues Resolved:**
  - Missing `pom.xml` errors
  - ANTLR rule mismatches (e.g., undefined `INTEGER`, typos)
  - Java compilation issues (missing dependencies, incorrect method signatures)
  - Apache RAT and Checkstyle violations
  - PowerMock `InaccessibleObjectException` with Java 24

- **Tips:**
  - For module-specific builds:
```bash
cd wrangler-api && mvn compile -X
```

---

## ⚠️ Current Status & Known Issues

- ✅ Core implementation is complete and functional.
- ✅ Passes Checkstyle and compiles cleanly.
- ✅ Unit tests for added functionality are passing.
- ❌ Known Issue: Some **existing tests fail** due to likely environment or mocking issues (e.g., `ObjenesisException` with PowerMock under Java 24). Further investigation needed.

---

For questions or to contribute, please open an issue or submit a pull request.



