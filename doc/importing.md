

## Eclipse

Importing the project in Eclipse is trivial because the root directory contains a `.project` and a `.classpath` file.

## Intellij

### Importing the project as an Eclipse Project

* Create the project with **New Project**, using `FLASH-MAS` as project name and using the existing location of the `FLASH-MAS` directory as Location.
* A warning is expected, that the directory is not empty.
* Go to Project Structure (click on the wheel in the title bar, select project Structure)
  * select the SDK (whatever you have installed)
  * select language level 8
  * go to Modules in the structure to the left of the window
  * click on `+`, selet Import Module, be sure `FLASH-MAS` (the project root directory) is selected, click Ok
    * select Import module from external model, select Eclipse, Next
    * Next (for Eclipse projects directory)
    * Select all, Next
    * Create
* Done

### Setting up the project manually

In order to import the project, extract the `.idea` directory in the `intellijsetup.idea.zip` archive to the root directory, such that there exist the file `<project root>/.idea/workspace.xml`

if this does not work, see the next section.

## Other IDEs

In order to be able to compile and execute the project it is necessary that in the project setup:
* all directories beginning with `src` are set as source folders
* all the `.jar` files in `lib/` are set as libraries
* Java compliance level is set to Java 8 (1.8)
