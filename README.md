# WorldReader
*Current version is 1.0 for WorldEngine 0.19.0*

Java utility for reading [WorldEngine](https://github.com/Mindwerks/worldengine) files (in [ProtoBuf](https://developers.google.com/protocol-buffers/) format) and operating on their data.

The application will take as an argument either a world file or a directory. If no argument is included, it will default to the current active directory. If a directory, it will look for files named *.world in the directory, and operate on each one. For each world file processed, the application will generate a corresponding height map, rainfall map, river map, and temperature map.

The current version has been tested against WorldEngine 0.19.0, though should be compatible with future versions, if the ProtoBuf format used in those versions remains backwards compatible.

### Usage Examples
##### Simple usage
This command will search the current directory for files named *.world and generate images for each one.
```
java -jar world-reader-1.0.jar
```
##### Specific file
This command will generate images for the file named myworld.world in the current directory.
```
java -jar world-reader-1.0.jar myworld.world
```
##### Directory
This command will search the directory name myworldfolder for files named *.world and generate images for each one.
```
java -jar world-reader-1.0.jar myworldfolder
```
