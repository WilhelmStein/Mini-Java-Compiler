import syntaxtree.*;
import java.io.*;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.List;


class OffsetMaps { // Essentially a Pair class

	public Map<String,Integer> variableOffsets;
	public Map<String,Integer> methodOffsets;

	OffsetMaps()
	{
		this.variableOffsets = new LinkedHashMap<String,Integer>();
		this.methodOffsets = new LinkedHashMap<String,Integer>();
	}
}

class Main {

    public static void main (String [] args){

		if(args.length == 0){
			System.err.println("Usage: java Main [options] <inputFile1> <inputFile2> ... <inputFileN>\nUse --help for more info.");
			System.exit(1);
		}

		boolean quietMode = false;

		int j = 0;
		for(; j < args.length; j++)
			if(args[j].equals("-q"))
			{
				quietMode = true;
				j++;
				break;
			}
			
		if (j == args.length)
			j = 0;
	
		FileInputStream fis = null;
		
		for(int i = j; i < args.length; i++ ) {

			Map<String, Map<String, List<String>>> classToMethods = new HashMap<String, Map<String, List<String>>>();
			Map<String, Map<String, String>> scopeToVars = new HashMap<String, Map<String, String>>();
			Map<String, String> inheritanceChain = new HashMap<String, String>();

			Map<String, OffsetMaps> classToOffsetMap = new LinkedHashMap<String, OffsetMaps>();

			boolean found_error = false;

			try{
				if(!quietMode)
					System.out.println("Checking File: " + args[i]);

				fis = new FileInputStream(args[i]);
				MiniJavaParser parser = new MiniJavaParser(fis);
				MainVisitor mainVis = new MainVisitor(classToMethods, scopeToVars, inheritanceChain, classToOffsetMap);
				ClassDefVisitor classDefVis = new ClassDefVisitor(classToMethods, scopeToVars, inheritanceChain);
				Goal root = parser.Goal();
				root.accept(classDefVis, null);
				root.accept(mainVis, null);

				IntermediateCodeVisitor intermediateCodeVis = new IntermediateCodeVisitor(args[i] + ".ll", classToOffsetMap, scopeToVars, inheritanceChain, classToMethods);
				root.accept(intermediateCodeVis, null);
				
				//System.out.println("\n");

			}
			catch(Exception ex) {
				System.out.println("\t" + ex.getMessage() + "\n");
				found_error = true;
			}
			finally{
				if(found_error)
					System.out.println();

				try{
					if(fis != null)
					{
						fis.close();
					}
				}
				catch(IOException ex){
				System.err.println("\t" + ex.getMessage());
				}
			}
		}
    }
}
