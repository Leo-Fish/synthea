#!/bin/bash

# Handle Ctrl+C gracefully
trap 'echo ""; echo "Script interrupted by user. Exiting..."; exit 130' INT TERM

# Check if test file is provided
if [ -z "$1" ]; then
    echo "Usage: $0 <test_file>"
    echo "Example: $0 tests.txt"
    echo ""
    echo "The test file should contain one test name per line."
    exit 1
fi

TEST_FILE="$1"
TIMEOUT_DURATION="30m"
OUTPUT_DIR="flakiness_results"
NUM_RUNS=3

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Check if test file exists
if [ ! -f "$TEST_FILE" ]; then
    echo "Error: Test file '$TEST_FILE' not found"
    exit 1
fi

echo "========================================"
echo "Starting flakiness detection"
echo "Test file: $TEST_FILE"
echo "Timeout per test: $TIMEOUT_DURATION"
echo "Number of runs per test: $NUM_RUNS"
echo "Output directory: $OUTPUT_DIR"
echo "========================================"
echo ""

# Read tests from file
TEST_COUNT=0
while IFS= read -r TEST_NAME || [ -n "$TEST_NAME" ]; do
    # *** FIX: Trim leading/trailing whitespace from the test name ***
    TEST_NAME=$(echo "$TEST_NAME" | tr -d '\r' | xargs)
    
    # Skip empty lines
    [ -z "$TEST_NAME" ] && continue
    
    TEST_COUNT=$((TEST_COUNT + 1))
    echo ""
    echo "========================================"
    echo "Test $TEST_COUNT: $TEST_NAME"
    echo "========================================"
    
    FLAKY=false
    
    # Run the test 3 times
    for RUN in $(seq 1 $NUM_RUNS); do
        echo ""
        echo "--- Run $RUN/$NUM_RUNS ---"
        
        OUTPUT=$(timeout $TIMEOUT_DURATION ./gradlew nondexTest -rerun-tasks --tests "$TEST_NAME" 2>&1 | tee /dev/tty)
        EXIT_CODE=$?
        
        # Check if timeout occurred
        if [ $EXIT_CODE -eq 124 ]; then
            echo "ERROR: nondexTest timed out after $TIMEOUT_DURATION"
            FLAKY=true
            break
        fi
        
        # Check for build failure (case-insensitive)
        OUTPUT_LOWER=$(echo "$OUTPUT" | tr '[:upper:]' '[:lower:]')
        
        if echo "$OUTPUT_LOWER" | grep -q "build failed"; then
            echo "Build failed detected on run $RUN"
            FLAKY=true
            break
        fi
    done
    
    # Extract class and method name for filename (last two parts after dots)
    # e.g., OAuth2TokenIntrospectionTests.requestWhenObtainReferenceAccessTokenAndIntrospectThenActive
    CLASS_AND_METHOD=$(echo "$TEST_NAME" | rev | cut -d'.' -f1-2 | rev)
    # Replace any characters that might cause issues in filenames
    SAFE_NAME=$(echo "$CLASS_AND_METHOD" | tr '/' '_' | tr ' ' '_')
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    
    if [ "$FLAKY" = true ]; then
        echo ""
        echo "========================================="
        echo "FLAKINESS DETECTED - Running nondexDebug"
        echo "========================================="
        
        DEBUG_OUTPUT=$(timeout $TIMEOUT_DURATION ./gradlew nondexDebug -rerun-tasks --tests "$TEST_NAME" 2>&1 | tee /dev/tty)
        EXIT_CODE=$?
        
        if [ $EXIT_CODE -eq 124 ]; then
            echo "ERROR: nondexDebug timed out after $TIMEOUT_DURATION"
            OUTPUT_FILE="$OUTPUT_DIR/${SAFE_NAME}_FAILED_TIMEOUT_${TIMESTAMP}.txt"
        else
            OUTPUT_FILE="$OUTPUT_DIR/${SAFE_NAME}_FAILED_${TIMESTAMP}.txt"
        fi
        
        echo ""
        echo "========================================="
        echo "Extracting debug results..."
        echo "========================================="
        
        # Extract debug path
        DEBUG_LINE=$(echo "$DEBUG_OUTPUT" | grep "DEBUG RESULTS FOR.*AND SEED:.*AT:")
        
        {
            echo "Test: $TEST_NAME"
            echo "Status: FAILED (Flaky)"
            echo "Timestamp: $(date)"
            echo "========================================="
            echo ""
            
            if [ -n "$DEBUG_LINE" ]; then
                DEBUG_PATH=$(echo "$DEBUG_LINE" | sed 's/.*AT: \(.*\)/\1/')
                echo "Debug path: $DEBUG_PATH"
                echo ""
                echo "========================================="
                echo "Debug file contents:"
                echo "========================================="
                echo ""
                
                if [ -f "$DEBUG_PATH" ]; then
                    cat "$DEBUG_PATH"
                else
                    echo "Warning: Debug file not found at $DEBUG_PATH"
                fi
            else
                echo "Warning: Could not find debug results path in output"
                echo ""
                echo "========================================="
                echo "nondexDebug output:"
                echo "========================================="
                echo ""
                echo "$DEBUG_OUTPUT"
            fi
        } > "$OUTPUT_FILE"
        
        echo "Results saved to: $OUTPUT_FILE"
        
        # Also display the debug file contents
        echo ""
        echo "========================================="
        echo "Debug file contents:"
        echo "========================================="
        if [ -n "$DEBUG_LINE" ]; then
            DEBUG_PATH=$(echo "$DEBUG_LINE" | sed 's/.*AT: \(.*\)/\1/')
            if [ -f "$DEBUG_PATH" ]; then
                cat "$DEBUG_PATH"
            fi
        fi
        
    else
        echo ""
        echo "========================================="
        echo "NO FLAKINESS FOUND"
        echo "========================================="
        
        OUTPUT_FILE="$OUTPUT_DIR/${SAFE_NAME}_PASSED_${TIMESTAMP}.txt"
        
        {
            echo "Test: $TEST_NAME"
            echo "Status: PASSED (No flakiness detected)"
            echo "Timestamp: $(date)"
            echo "Number of successful runs: $NUM_RUNS"
        } > "$OUTPUT_FILE"
        
        echo "Results saved to: $OUTPUT_FILE"
    fi
    
done < "$TEST_FILE"

echo ""
echo "========================================"
echo "All tests completed!"
echo "Results saved in: $OUTPUT_DIR"
echo "========================================"