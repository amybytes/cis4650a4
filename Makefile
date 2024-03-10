JAVA=java
JAVAC=javac
JFLEX=jflex
CLASSPATH=-cp /usr/share/java/cup.jar:.
CUP=$(JAVA) $(CLASSPATH) java_cup.Main

all: CM.class

CM.class: absyn/*.java parser.java sym.java Lexer.java ShowTreeVisitor.java SemanticAnalyzer.java Scanner.java CM.java

%.class: %.java
	$(JAVAC) $(CLASSPATH) $^

Lexer.java: cminus.flex
	$(JFLEX) cminus.flex

parser.java: cminus.cup
	$(CUP) -dump -expect 3 cminus.cup
	# $(CUP) -expect 3 cminus.cup

clean:
	rm -f parser.java Lexer.java sym.java *.class absyn/*.class *~
