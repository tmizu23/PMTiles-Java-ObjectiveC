# Variables
JAVAC = javac
JAVA = java
SOURCES = Main.java $(wildcard PMTiles/*.java)
CLASSES = $(SOURCES:.java=.class)

# Targets
all: compile

compile: $(CLASSES)

%.class: %.java
	$(JAVAC) $<

run: compile
	$(JAVA) Main

clean:
	rm -f $(CLASSES)
