#!/bin/bash
make clean && make all && clear;

for DIR_NAME in ./tests/*; do

    FILE_NAME=$DIR_NAME/${DIR_NAME#./tests/}.javaa;
    java Main "$FILE_NAME";
    
    EXPECTED_OFFSET_FILE=$DIR_NAME/${DIR_NAME#./tests/}.txt;

    if [ -f "$EXPECTED_OFFSET_FILE" ]; then
        clang-4.0 -o $DIR_NAME/out.exe $DIR_NAME/${DIR_NAME#./tests/}.ll;
        if [ "$1" == "-e" ]; then
            $DIR_NAME/out.exe;
        fi
    fi
done