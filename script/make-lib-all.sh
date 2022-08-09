#!/bin/bash

rm -r lib-all
mkdir lib-all

for file in $(find lib/ -type f  -name "*.jar"); do
	name=$(basename $file)
	cp "$file" "lib-all/$name"
done