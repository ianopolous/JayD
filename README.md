JayD
====

An x86 disassembler written in Java (port of udis86)


Usage:
To generate the disassembler from the xml:
* make gen
* java -jar Gen.jar

To disassemble some bytes:
* make dis
* java -jar Dis.jar 05 01 2c 34 1d
or
* java -jar Dis.jar -rm 05 01 2c