import syntaxtree.*;
import visitor.*;
import java.io.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


class OffsetMaps {

	public HashMap<String,Integer> variableOffsets;
	public HashMap<String,Integer> methodOffsets;

	OffsetMaps()
	{
		this.variableOffsets = new HashMap<String,Integer>();
		this.methodOffsets = new HashMap<String,Integer>();
	}
}

class Main {

	private static List<Entry<String, Integer>> sortByValues(HashMap<String, Integer> map) {
		List<Entry<String, Integer>> list = new LinkedList<Entry<String, Integer>>(map.entrySet());
		// Defined Custom Comparator here
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
			}
		});

		return list;
   }
    public static void main (String [] args){
			if(args.length == 0){
				System.err.println("Usage: java Driver <inputFile1> <inputFile2> ... <inputFileN>");
				System.exit(1);
			}

			FileInputStream fis = null;
			
			for(int i = 0; i < args.length; i++ ) {

				HashMap<String, HashMap<String, List<String>>> classToMethods = new HashMap<String, HashMap<String, List<String>>>();
				HashMap<String, HashMap<String, String>> scopeToVars = new HashMap<String, HashMap<String, String>>();
				HashMap<String, String> inheritanceChain = new HashMap<String, String>();

				HashMap<String, OffsetMaps> classToOffsetMap = new HashMap<String, OffsetMaps>();

				boolean found_error = false;

				try{
					
					System.out.println("Checking File: " + args[i]);
					fis = new FileInputStream(args[i]);
					MiniJavaParser parser = new MiniJavaParser(fis);
					MainVisitor mainVis = new MainVisitor(classToMethods, scopeToVars, inheritanceChain, classToOffsetMap);
					ClassDefVisitor classDefVis = new ClassDefVisitor(classToMethods, scopeToVars, inheritanceChain);
					Goal root = parser.Goal();
					root.accept(classDefVis, null);
					root.accept(mainVis, null);

					for (Entry<String, OffsetMaps> offsetMap : classToOffsetMap.entrySet()) {

						System.out.println("-----------Class " + offsetMap.getKey() + "-----------");

						System.out.println("--Variables--");
						if(!offsetMap.getValue().variableOffsets.isEmpty())
						{
							List<Entry<String, Integer>> sorted = sortByValues(offsetMap.getValue().variableOffsets);
							for(Entry<String, Integer> entry : sorted) {
								System.out.println(offsetMap.getKey() + "." + entry.getKey() + " : " + entry.getValue());
							}
							System.out.println();
						}

						System.out.println("---Methods---");
						if(!offsetMap.getValue().methodOffsets.isEmpty())
						{
							
							List<Entry<String, Integer>> sorted = sortByValues(offsetMap.getValue().methodOffsets);
							for(Entry<String, Integer> entry : sorted) {
								System.out.println(offsetMap.getKey() + "." + entry.getKey() + " : " + entry.getValue());
							}
							System.out.println();
						}

					}
					
					System.out.println("\n");

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
