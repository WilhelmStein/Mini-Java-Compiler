#!/bin/bash
make clean && make all && clear;

for FILE_NAME in ./tests/*.java; do
    TEMP=${FILE_NAME%.*}.offset;
    OFFSET_FILE=./tests/offsets/${TEMP#./tests/};
    java Main -q "$FILE_NAME" > "$OFFSET_FILE";
done

for FILE_NAME in ./tests/offset_results/*.txt; do
    TEMP=${FILE_NAME%.*}.offset;
    OFFSET_FILE=./tests/offsets/${TEMP#./tests/offset_results/};
    diff "$FILE_NAME" "$OFFSET_FILE";
done