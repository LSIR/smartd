#!/bin/bash
 
# preparation
cd lib

dir=com/mysql
# if the dir does not exist, extract the jar file
if [ ! -d "$dir" ]; then
	unzip -o mysql-connector-java-5.0.4-bin.jar
fi
# copy to the bin directory
cp -r com ../bin


# go to bin directory
cd ../bin

# create jar for SurveyAnalysis.jar
echo Main-class: ch.epfl.lsir.smartd.forecast.Forecast > manifest.txt
jar cvfm Forecast.jar manifest.txt com/mysql ch
rm -f manifest.txt

# bring the new jars
mv Forecast.jar ..

# go to main project directory
cd ..


