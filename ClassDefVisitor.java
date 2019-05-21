import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import syntaxtree.*;
import visitor.GJDepthFirst;

class Argument {
    public String argumentName;
    public String argumentType;

    Argument(String argumentName, String argumentType) {
        this.argumentName = argumentName;
        this.argumentType = argumentType;
    }

    static List<String> typeList(List<Argument> argList) {
        List<String> tl = new ArrayList<String>();
        for (Argument arg : argList) {
            tl.add(arg.argumentType);
        }
        
        return tl;
    }   
}

class ClassDefVisitor extends GJDepthFirst<String, String> {

    Map<String, Map<String, List<String>>> classToMethods;
    Map<String, Map<String, String>> scopeToVars;
    Map<String, String> inheritanceChain;

    private List<Argument> argList;

    ClassDefVisitor(Map<String, Map<String, List<String>>> classToMethods,
                    Map<String, Map<String, String>> scopeToVars,
                    Map<String, String> inheritanceChain ) throws Exception 
    {
        super();
        this.classToMethods = classToMethods;
        this.scopeToVars = scopeToVars;
        this.inheritanceChain = inheritanceChain;

        this.argList = new ArrayList<Argument>();
    }

    // Utility functions

    private List<String> findMethodData(String methodName, String startScope) {

        if(startScope == null)
            return null;

        Map<String, List<String>> methods = classToMethods.get(startScope);
        List<String> args = methods.get(methodName);
        if( args == null )
        {
            String parentClass = inheritanceChain.get(startScope);
            return findMethodData(methodName, parentClass);
        }
        
        return args;
    }

    // Visit functions

    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    @Override
    public String visit(Goal n, String argu) throws Exception {
        n.f0.accept(this, argu);
        if( n.f1.present() )
            n.f1.accept(this, argu);

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")" 
     * f13 -> "{" 
     * f14 -> ( VarDeclaration() )* 
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    @Override
    public String visit(MainClass n, String argu) throws Exception {
        String className = n.f1.accept(this, argu);
        
        if (scopeToVars.containsKey(className)) // Check if class has already been defined.
            throw new Exception("Redefinition Error: Class " + className + " already exists.");

        classToMethods.put(className, new HashMap<String, List<String>>());
        scopeToVars.put(className, new HashMap<String, String>());

        classToMethods.put(className + "::main", new HashMap<String, List<String>>());
        scopeToVars.put(className + "::main", new HashMap<String, String>());
        inheritanceChain.put(className + "::main", className);

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public String visit(ClassDeclaration n, String argu) throws Exception {
        String className = n.f1.accept(this, argu);

        if (scopeToVars.containsKey(className))
            throw new Exception("Redefinition Error: Class " + className + " already exists.");
        
        classToMethods.put(className, new HashMap<String, List<String>>());
        scopeToVars.put(className, new HashMap<String, String>());

        if( n.f4.present() )
            n.f4.accept(this, className);

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public String visit(ClassExtendsDeclaration n, String argu) throws Exception {
        String className = n.f1.accept(this, argu);

        if (scopeToVars.containsKey(className))
            throw new Exception("Redefinition Error: Class " + className + " already exists.");

        classToMethods.put(className, new HashMap<String, List<String>>());
        scopeToVars.put(className, new HashMap<String, String>());

        String parentClass = n.f3.accept(this, argu);

        if( !classToMethods.containsKey(parentClass) )
            throw new Exception("Inheritance Error: Class " + parentClass + " must be defined before class " + className + ".");
        
        inheritanceChain.put(className, parentClass);

        if( n.f6.present() )
            n.f6.accept(this, className);

        return null;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    @Override
    public String visit(MethodDeclaration n, String argu) throws Exception {
        String methodType = n.f1.accept(this, argu);
        String methodName = n.f2.accept(this, argu);

        Map<String, List<String> > classMethods = classToMethods.get(argu);

        if( classMethods.containsKey(methodName) )
            throw new Exception("Redefinition Error: Method " + argu + "::" + methodName + " already defined.");

        List<String> methodData = findMethodData(methodName, argu);

        HashMap<String, String> argumentsToTypes = new HashMap<String,String>();

        if( n.f4.present() )
        {
            n.f4.accept(this, argu);
            List<String> typeList = Argument.typeList(argList);
            if(methodData == null)
            {
                methodData = typeList;
                methodData.add(0, methodType);
                classMethods.put(methodName, methodData);
            }
            else if(methodType != methodData.get(0) || !methodData.subList(1, methodData.size()).equals(typeList))
                throw new Exception("Overload Error: Cannot overload function " + argu + "::" + methodName + "");

            for (Argument arg : argList)
                argumentsToTypes.put(arg.argumentName, arg.argumentType);
            
            argList.clear();
        }
        else
        {
            if(methodData == null)
            {
                methodData = new ArrayList<String>();
                methodData.add(0, methodType);
                classMethods.put(methodName, methodData);
            }
            else if(methodType != methodData.get(0) )
                throw new Exception("Overload Error: Cannot overload function " + argu + "::" + methodName);
            
        }

        String currScope = argu + "::" + methodName;
        scopeToVars.put(currScope, argumentsToTypes);
        inheritanceChain.put(currScope, argu);
        
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, String argu) throws Exception {
        argList.add(new Argument(n.f1.accept(this, argu), n.f0.accept(this, argu)));
        return null;
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    @Override
    public String visit(Identifier n, String argu) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    @Override
    public String visit(ArrayType n, String argu) throws Exception {
        return "array";
    }

    /**
     * f0 -> "boolean"
     */
    @Override
    public String visit(BooleanType n, String argu) throws Exception {
        return "boolean";
    }

    /**
     * f0 -> "int"
     */
    @Override
    public String visit(IntegerType n, String argu) throws Exception {
        return "int";
    }
}