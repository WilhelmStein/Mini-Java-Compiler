import syntaxtree.*;
import visitor.*;
import java.io.*;
import java.util.HashMap;
import java.util.List;

class Main {

	HashMap<String, String[]> ClassMethods;
    public static void main (String [] args){
		if(args.length == 0){
			System.err.println("Usage: java Driver <inputFile1> <inputFile2> ... <inputFileN>");
			System.exit(1);
		}

		FileInputStream fis = null;
		HashMap<String, HashMap<String, List<String>>> classToMethods = new HashMap<String, HashMap<String, List<String>>>();
		HashMap<String, HashMap<String, String>> scopeToVars = new HashMap<String, HashMap<String, String>>();
		HashMap<String, String> inheritanceChain = new HashMap<String, String>();
		
		for(int i = 0; i < args.length; i++ ) {
			try{
				fis = new FileInputStream(args[i]);
				MiniJavaParser parser = new MiniJavaParser(fis);
				System.err.println("Program parsed successfully.");
				MainVisitor mainVis = new MainVisitor(classToMethods, scopeToVars, inheritanceChain);
				ClassDefVisitor classDefVis = new ClassDefVisitor(classToMethods, scopeToVars, inheritanceChain);
				Goal root = parser.Goal();
				root.accept(classDefVis, null);
				root.accept(mainVis, null);
			}
			catch(Exception ex) {
				System.out.println(ex.getMessage());
			}
			finally{
				try{
					if(fis != null)
					{
						System.out.println(args[i] + " was parsed successfully.");
						fis.close();
					}
				}
				catch(IOException ex){
				System.err.println(ex.getMessage());
				}
			}
		}
    }
}
