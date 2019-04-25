import java.util.HashMap;
import java.util.List;

import syntaxtree.*;
import visitor.GJDepthFirst;

class ClassDefVisitor extends GJDepthFirst<String, String> {

    HashMap<String, HashMap<String, List<String>>> classToMethods;
    HashMap<String, HashMap<String, String>> scopeToVars;
	HashMap<String, String> inheritanceChain;

    ClassDefVisitor(HashMap<String, HashMap<String, List<String>>> classToMethods,
    HashMap<String, HashMap<String, String>> scopeToVars,
    HashMap<String, String> inheritanceChain ) throws Exception 
    {
        super();
        this.classToMethods = classToMethods;
        this.scopeToVars = scopeToVars;
        this.inheritanceChain = inheritanceChain;
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
            throw new Exception("Redefinition Error: Class " + className + " already exists.\n");
        else 
        {
            classToMethods.put(className, new HashMap<String, List<String>>());
            scopeToVars.put(className, new HashMap<String, String>());
        }

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
            throw new Exception("Redefinition Error: Class " + className + " already exists.\n");

        classToMethods.put(className, new HashMap<String, List<String>>());
        scopeToVars.put(className, new HashMap<String, String>());

        String parentClass = n.f3.accept(this, argu);

        if( !classToMethods.containsKey(parentClass) )
            throw new Exception("Inheritance Error: Class " + parentClass + " must be defined before class " + className + ".\n");
        
        inheritanceChain.put(className, parentClass);

        return _ret;
    }
}