# Simple XMI annotation viewer

This package provides a very simple viewer for XMI files. The app assumes that the typesystem is available as an XML file called typesystem.xml in the same directory (this is what dkpro XmiWriter prints anyway).

## Compile and package
```bash
mvn package
```
The `target` directory then contains an executable jar-file `SimpleXmiViewer-VERSION.jar` and an OSX-ready app `SimpleXmiViewer-VERSION.app`.