**API-HARVESTER**
 
A tool for harvesting data from an api  
Very generic implementation that could work for some cases that accept an offset and limit query parameters.

**Usage**

*Harvest mode:*
* Download the directory api-harvester-run  
* Update the harvester.properties file appropriately 
* Run the .jar file `java -jar api-harvester-1.0-SNAPSHOT-jar-with-dependencies.jar`

It will output harvesting information on the terminal but also under the directory logs, which will be generated.

*Convert mode:*

Convert from json files to xml files.  
usage: xml converter  
 `-c,--convert`                     enable only convertion of input  
 `-d,--outputDirectoryPath <arg>`   output directory path  
 `-f,--filePath <arg>`              file to read from  
 `-h,--help`                        help output  
 `-l,--listElementNames <arg>`      if json is list, set the element names  
 `-o,--outputFileName <arg>`        output filename  
 `-r,--rootElementName <arg>`       root element name  

Examples:  
Convert json file located in `/tmp/test.json` to current directory `./` with output file name `test.xml`.  
If json is a list and does not start with a bracket `{` then each element will have the tag name `record`  
`java -jar api-harvester-1.0-SNAPSHOT-jar-with-dependencies.jar -c -d ./ -o test.xml -l record -f /tmp/test.json`

Run with providing a wrapper element as well:
`java -jar api-harvester-1.0-SNAPSHOT-jar-with-dependencies.jar -c -d ./ -o test.xml -l record -r root -f /tmp/test.json`

**harvester.properties**  
The user who runs the .jar file should have write access to the directories specified for the generation of the directories and files.  
`api.endpoint` The api endpoint to harvest.  
`directory.name.prefix` The directory prefix that the harvest should have, for example if it's set to harvest then the result should be something like `harvest-2017_03_27_1746/`.  
`json.convert.to.xml` true or false. Convert json received data automatically to xml.  
`offset.parameter.name` The query parameter name used for offset.  
`limit.parameter.name`  The query parameter name used for limit.  
`offset` The value of the offset query parameter.  
`limit`  The value of the limit query parameter.  
`record.list.field` The element that hold the list/array of the records to be extracted from the response.  
`harvest.output.directory` The root directory where all the harvest directory should reside.  
