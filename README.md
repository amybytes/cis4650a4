# CIS*4650 Checkpoint 3
Ethan Rowan (1086586)

## Building
Ensure that the `CLASSPATH` variable in the makefile includes paths to the CUP library and the local directory. By default, the makefile assumes the CUP jar file is located in the `/usr/share/java/cup.jar` directory.

The program can be built using `make all`.

## Running
The program can be run using the command `java -cp CLASSPATH CM INPUT_FILE -s` where `CLASSPATH` is the same classpath used for the makefile and `INPUT_FILE` is the path to the input file.

Passing the `-a` option to the program causes the syntax tree to be written to "INPUT_FILE.abs".

Passing the `-s` option to the program causes the symbol table to be written to "INPUT_FILE.sym".

For example: `java -cp /usr/share/java/cup.jar:. CM program.cm -s` scans, parses, and runs the semantic analyzer on the input file "program.cm" and writes the syntax tree to "program.sym".

## Additional Notes
- Array types in function parameters are arbitrarily assigned a size of -1, meaning the size is not yet known.
