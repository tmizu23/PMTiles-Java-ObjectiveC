# Variables
CC = clang
FRAMEWORKS = -framework Foundation -framework AppKit -framework CoreGraphics -framework ImageIO -lz -g
SOURCES = main.m $(wildcard PMTiles/*.m)
OUTFILE = output

# Targets
all: $(OUTFILE)

$(OUTFILE): $(SOURCES)
	$(CC) $(FRAMEWORKS) $(SOURCES) -o $(OUTFILE)

clean:
	rm -f $(OUTFILE)

run: $(OUTFILE)
	./$(OUTFILE)
