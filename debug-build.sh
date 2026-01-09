#!/bin/bash

# Get detailed compilation errors
# Run this from the project root directory

echo "Running Maven with verbose compilation output..."
echo "================================================"
echo ""

mvn clean compile -X 2>&1 | grep -A 20 "COMPILATION ERROR" || mvn clean compile 2>&1 | grep -A 50 "error:"

echo ""
echo "================================================"
echo "If you don't see errors above, run:"
echo "  mvn clean compile -X"
echo "And check the output manually"
