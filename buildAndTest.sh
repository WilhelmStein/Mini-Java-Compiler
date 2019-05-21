#!/bin/bash
make clean && make all && clear;

for FILE_NAME in ./tests/*.javaa; do
    TEMP=${FILE_NAME%.*}.offset;
    OFFSET_FILE=./tests/offsets/${TEMP#./tests/};
    java Main $1 "$FILE_NAME" > "$OFFSET_FILE";
done

if [ "$1" != "-q" ]; then
    for FILE_NAME in ./tests/offset_results/*.txt; do
        TEMP=${FILE_NAME%.*}.offset;
        OFFSET_FILE=./tests/offsets/${TEMP#./tests/offset_results/};
        diff "$FILE_NAME" "$OFFSET_FILE";
    done
fi

for FILE_NAME in ./tests/*.ll; do
	NEW_FILENAME=./tests/llvm/${FILE_NAME#./tests/};
	mv $FILE_NAME $NEW_FILENAME;
done