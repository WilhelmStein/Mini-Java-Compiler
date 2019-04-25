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
				MainVisitor mainVis = new MainVisitor(classToMethods, scopeToVars, inheritanceChain);
				ClassDefVisitor classDefVis = new ClassDefVisitor(classToMethods, scopeToVars, inheritanceChain);
				Goal root = parser.Goal();
				root.accept(classDefVis, null);
				root.accept(mainVis, null);
				System.out.println(args[i] + " was parsed successfully.");
			}
			catch(Exception ex) {
				System.out.println(ex.getMessage());
				System.exit(-1);
			}
			finally{
				try{
					if(fis != null)
					{
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
