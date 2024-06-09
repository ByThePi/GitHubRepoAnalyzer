# GitHubRepoAnalyzer

The program clones a repository, specified by a user-provided URL, to the device where it is executed.
Various git commands are utilized during the cloning process.

It recursively scans the cloned project and examines only files with the *.java extension,
excluding those that contain "interface" or "enum" structures.

Focusing solely on classes, the program reads the lines of each file one by one using a "while" loop.

It counts Javadocs, comment lines, empty lines, total lines, and functions in the file using various methods.
From this data, it calculates the number of code lines and the comment deviation percentage.

All this data is printed to the console in a specific format under the relevant class.
The program then terminates.
