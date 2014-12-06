#!/bin/bash

echo $1
ANT_BUILD=/usr/local/ant/bin/ant
echo $ANT_BUILD
OPT=1
if [ "$1" = "medlda" ]
then 
 echo "build medlda";
 $ANT_BUILD -buildfile build.xml
else
 OPT=1
fi

