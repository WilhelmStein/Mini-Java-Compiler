all: compile

compile:
	java -jar ./jtb132di.jar -te miniJava.jj
	java -jar ./javacc5.jar miniJava-jtb.jj
	javac Main.java MainVisitor.java ClassDefVisitor.java

clean:
	rm -f *.class *~
