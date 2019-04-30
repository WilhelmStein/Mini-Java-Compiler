#!/bin/bash
make clean && make all && clear;

for FILE_NAME in ./tests/*.javaa; do
    TEMP=${FILE_NAME%.*}.offset;
    OFFSET_FILE=./tests/offsets/${TEMP#./tests/};
    java Main -q "$FILE_NAME" > "$OFFSET_FILE";
done

for FILE_NAME in ./tests/results/*.txt; do
    TEMP=${FILE_NAME%.*}.offset;
    OFFSET_FILE=./tests/offsets/${TEMP#./tests/results/};
    diff "$FILE_NAME" "$OFFSET_FILE";
done