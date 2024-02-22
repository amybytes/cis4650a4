# CIS*4650 Checkpoint 1
Ethan Rowan (1086586)

## Building
Ensure that the `CLASSPATH` variable in the makefile includes paths to the CUP library and the local directory. By default, the makefile assumes the CUP jar files are located in the `./cup` directory.

The parser and scanner can be built using `make all`.

## Running
The parser and scanner can be run using `java -cp CLASSPATH Main INPUT_FILE -a` where `CLASSPATH` is the same classpath used for the makefile and `INPUT_FILE` is the path to the input file.

Passing the `-a` to the program causes the syntax tree is displayed after parsing.