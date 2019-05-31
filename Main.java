import syntaxtree.*;
import java.io.*;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.List;


class OffsetMapData {
	Integer offset;
	String className;

	OffsetMapData(Integer offset, String className) {
		this.offset = offset;
		this.className = className;
	}
}

class OffsetMaps { // Essentially a Pair class

	public String className;

	public Map<String,OffsetMapData> variableOffsets;
	public Map<String,OffsetMapData> methodOffsets;

	public int totalVarOffset;
	public int totalMethodOffset;

	OffsetMaps(String className)
	{
		this.variableOffsets = new LinkedHashMap<String,OffsetMapData>();
		this.methodOffsets = new LinkedHashMap<String,OffsetMapData>();
		
		this.totalMethodOffset = 0;
		this.totalVarOffset = 0;

		this.className = className;
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

			Map<String, Map<String, List<Argument>>> classToMethods = new HashMap<String, Map<String, List<Argument>>>();
			Map<String, Map<String, String>> scopeToVars = new HashMap<String, Map<String, String>>();
			Map<String, String> inheritanceChain = new HashMap<String, String>();

			Map<String, OffsetMaps> classToOffsetMap = new LinkedHashMap<String, OffsetMaps>();

			boolean found_error = false;

			try{
				//System.out.println("Checking file: " + args[i]);
				fis = new FileInputStream(args[i]);
				MiniJavaParser parser = new MiniJavaParser(fis);
				MainVisitor mainVis = new MainVisitor(classToMethods, scopeToVars, inheritanceChain, classToOffsetMap);
				ClassDefVisitor classDefVis = new ClassDefVisitor(classToMethods, scopeToVars, inheritanceChain);
				Goal root = parser.Goal();
				root.accept(classDefVis, null);
				root.accept(mainVis, null);

				IntermediateCodeVisitor intermediateCodeVis = new IntermediateCodeVisitor(args[i], quietMode, classToOffsetMap, scopeToVars, inheritanceChain, classToMethods);
				root.accept(intermediateCodeVis, null);
				
				//System.out.println("\n");

			}
			catch(Exception ex) {
				System.out.println("Exception Encountered in file: " + args[i] + "\n\t" + ex + "\n");
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
