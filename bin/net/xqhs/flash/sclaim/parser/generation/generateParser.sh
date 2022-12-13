#!/bin/bash

# the program byacc/j is used for the generation of the parser. 
# Run this script in Linux or run the following command after BYACC/J was successfully installed, in any supported OS.

yacc -J -Jclass=ParserSClaim -Jpackage=net.xqhs.flash.sclaim.parser.generation -Jsemantic=ParserSClaimVal parser.y
