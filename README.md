# GitHubRepoAnalyzer
Clones the given repo and analyzes only files with *.java extension


Program clones that repo to the device on which the program is run,
via the URL received from the user and representing a Github repo. 
Various git commands are used for the cloning process.

It recursively scans the cloned project file and examines only files with *.java extension,
excluding structures containing "interface" and "enum" from these files.

The program, which evaluates only the classes,
starts reading the lines of the file under examination one by one with the help of a "while" loop.

It counts "javadocs", comment lines, empty lines, total number of lines and functions in the file with the help of various methods.
Based on the data it obtains, it calculates the number of code lines and the comment deviation percentage.

All this data is printed to the console in a specific pattern under the relevant class. And the program ends.
