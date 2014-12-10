
.PHONY: gen
gen:
	mkdir -p build
	javac -d build `find com/gen -name \*.java`
	echo "Manifest-Version: 1.0" > jar.manifest
	echo "Name: Ian's Disassembler" >> jar.manifest
	echo "Created-By: Ian Preston" >> jar.manifest
	echo "Specification-Version: 1" >> jar.manifest
	echo "Main-Class: com.gen.TableGen" >> jar.manifest
	echo "" >> jar.manifest

	jar -cfm Gen.jar jar.manifest -C build com
	rm -f jar.manifest

.PHONY: dis
dis:
	mkdir -p build
	javac -g -d build `find com -name \*.java`
	echo "Manifest-Version: 1.0" > jar.manifest
	echo "Name: Ian's Disassembler" >> jar.manifest
	echo "Created-By: Ian Preston" >> jar.manifest
	echo "Specification-Version: 1" >> jar.manifest
	echo "Main-Class: com.Dis" >> jar.manifest
	echo "" >> jar.manifest

	jar -cfm Dis.jar jar.manifest -C build com
	rm -f jar.manifest

.PHONY: pat
pat:
	mkdir -p build
	javac -g -d build `find com -name \*.java`
	echo "Manifest-Version: 1.0" > jar.manifest
	echo "Name: Ian's Disassembler" >> jar.manifest
	echo "Created-By: Ian Preston" >> jar.manifest
	echo "Specification-Version: 1" >> jar.manifest
	echo "Main-Class: com.PatternGenerator" >> jar.manifest
	echo "" >> jar.manifest

	jar -cfm Pat.jar jar.manifest -C build com
	rm -f jar.manifest

.PHONY: compare
compare:
	mkdir -p build
	javac -g -d build `find com -name \*.java`
	echo "Manifest-Version: 1.0" > jar.manifest
	echo "Name: Ian's Disassembler" >> jar.manifest
	echo "Created-By: Ian Preston" >> jar.manifest
	echo "Specification-Version: 1" >> jar.manifest
	echo "Main-Class: com.ObjdumpCompare" >> jar.manifest
	echo "" >> jar.manifest

	jar -cfm Compare.jar jar.manifest -C build com
	rm -f jar.manifest

