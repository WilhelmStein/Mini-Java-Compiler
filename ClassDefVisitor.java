import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    HashMap<String, HashMap<String, List<String>>> classToMethods;
    HashMap<String, HashMap<String, String>> scopeToVars;
    HashMap<String, String> inheritanceChain;
    private List<Argument> argList;

    ClassDefVisitor(HashMap<String, HashMap<String, List<String>>> classToMethods,
    HashMap<String, HashMap<String, String>> scopeToVars,
    HashMap<String, String> inheritanceChain ) throws Exception 
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

        HashMap<String, List<String>> methods = classToMethods.get(startScope);
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
        String _ret = null;
        // n.f0.accept(this, argu);
        if( n.f1.present() )
            n.f1.accept(this, argu);
        // n.f2.accept(this, argu);
        return _ret;
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
        String _ret = null;
        //n.f0.accept(this, argu);
        String className = n.f1.accept(this, argu);

        if (scopeToVars.containsKey(className))
            throw new Exception("Redefinition Error: Class " + className + " already exists.");
        else 
        {
            classToMethods.put(className, new HashMap<String, List<String>>());
            scopeToVars.put(className, new HashMap<String, String>());
        }

        if( n.f4.present() )
            n.f4.accept(this, className);

        return _ret;
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
        String _ret = null;
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

        return _ret;
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
        String _ret = null;
        //n.f0.accept(this, argu);
        String methodType = n.f1.accept(this, argu);
        String methodName = n.f2.accept(this, argu);

        HashMap<String, List<String> > classMethods = classToMethods.get(argu);

        if( classMethods.containsKey(methodName) )
            throw new Exception("Redefinition Error: Method " + argu + "::" + methodName + " already defined.");

        //n.f3.accept(this, argu);

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
        
        return _ret;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n, String argu) throws Exception {
        String _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, String argu) throws Exception {
        String _ret = null;
        argList.add(new Argument(n.f1.accept(this, argu), n.f0.accept(this, argu)));
        return _ret;
    }

    /**
     * f0 -> ( FormalParameterTerm() )*
     */
    @Override
    public String visit(FormalParameterTail n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    @Override
    public String visit(FormalParameterTerm n, String argu) throws Exception {
        String _ret = null;
        n.f1.accept(this, argu);
        return _ret;
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
        // n.f0.accept(this, argu);
        // n.f1.accept(this, argu);
        // n.f2.accept(this, argu);
        return "array";
    }

    /**
     * f0 -> "boolean"
     */
    @Override
    public String visit(BooleanType n, String argu) throws Exception {
        //return n.f0.accept(this, argu);
        return "boolean";
    }

    /**
     * f0 -> "int"
     */
    @Override
    public String visit(IntegerType n, String argu) throws Exception {
        //return n.f0.accept(this, argu);
        return "int";
    }
}