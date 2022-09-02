#!/bin/bash
# ------------------------------------------------------------------
#     Title: Automating Flash-MAS libs export
#            for building a Flash-MAS Android .apk

#     Description:
#            Steps taken by this script:
#             1. generates a Flash-MAS build
#             2. external libs used initially are decompressed
#             3. everything (1+2) is packaged together into one single jar
#             4. resulting Flash-MAS.jar is used as lib in the Android project
#             5. an Android build is generated with the resulted apk

# Dependency:
#     arch used here: https://github.com/andreiolaru-ro/FLASH-MAS
# ------------------------------------------------------------------
VERSION=0.1.0
USAGE="Usage: just run executable from immediate parent directory"

# -- Body ---------------------------------------------------------
# TODO generate latest flash-mas build
mkdir tmp
mkdir tmp/external_jars

# now ALL modules are integrated into final .jar,
# TODO should choose only needed modules
cp -R ../../bin/* tmp/
cp -R ../../lib/* tmp/external_jars/

for f in tmp/external_jars/*.jar
do
  unzip -o $f -d tmp
done

mv tmp/external_jars tmp/.ext_jars
cd tmp
jar cf Flash-MAS.jar *
cp -R Flash-MAS.jar ../../flash/libs/Flash-MAS.jar
cd ..

rm -rf tmp
# -----------------------------------------------------------------

# TODO generate apk build
# TODO org this script in functions, hide commands outputs & add executing steps as echo