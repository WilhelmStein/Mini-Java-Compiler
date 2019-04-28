import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import syntaxtree.*;
import visitor.GJDepthFirst;


public class MainVisitor extends GJDepthFirst<String, String> {

    HashMap<String, HashMap<String, List<String>>> classToMethods;
    HashMap<String, HashMap<String, String>> scopeToVars;
    HashMap<String, String> inheritanceChain;

    private HashMap<String, Integer> classToVarOffset;
    private HashMap<String, Integer> classToMethodOffset; 
    HashMap<String, OffsetMaps> classToOffsetMap;

    private List<String> argList;

    public MainVisitor( HashMap<String, HashMap<String, List<String>>> classToMethods,
                        HashMap<String, HashMap<String, String>> scopeToVars,
                        HashMap<String, String> inheritanceChain,
                        HashMap<String, OffsetMaps> classToOffsetMap ) throws Exception 
    {
        super();
        this.classToMethods = classToMethods;
        this.scopeToVars = scopeToVars;
        this.inheritanceChain = inheritanceChain;

        this.classToVarOffset = new HashMap<String, Integer>();
        this.classToMethodOffset = new HashMap<String, Integer>();
        this.classToOffsetMap = classToOffsetMap;

        this.argList = new ArrayList<String>();
    }

    // Utility Functions

    private String findVarType(String varName, String startScope) {

        if(startScope == null)
            return null;

        HashMap<String, String> vars = scopeToVars.get(startScope);
        String varType = vars.get(varName);
        if ( varType == null)
        {
            String parentScope = inheritanceChain.get(startScope);
            return findVarType(varName, parentScope);
        }

        return varType;
    }

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

    private boolean isAncestorOf(String offspringClass, String ancestorClass) {

        if(offspringClass.equals(ancestorClass))
            return true;

        String parent = inheritanceChain.get(offspringClass);
        if( parent == null )
            return false;
        
        if( parent.equals(ancestorClass) )
            return true;
        else
            return isAncestorOf(parent, ancestorClass);
    }

    private boolean overrides(String methodName, String className) {

        String parentClass = inheritanceChain.get(className);

        if(parentClass == null)
            return false;

        if(classToMethods.get(parentClass).containsKey(methodName))
            return true;
        else
            return overrides(methodName, parentClass);
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
        n.f0.accept(this, argu);
        if( n.f1.present() )
            n.f1.accept(this, argu);
        // n.f2.accept(this, argu);
        return _ret;
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
        String _ret = null;
        // n.f0.accept(this, argu);
        String className = n.f1.accept(this, argu);
        
        if (scopeToVars.containsKey(className))
            throw new Exception("Redefinition Error: Class " + className + " already exists.");

        classToMethods.put(className, new HashMap<String, List<String>>());
        scopeToVars.put(className, new HashMap<String, String>());

        classToMethods.put(className + "::main", new HashMap<String, List<String>>());
        scopeToVars.put(className + "::main", new HashMap<String, String>());
        inheritanceChain.put(className + "::main", className);

        // n.f2.accept(this, argu);
        // n.f3.accept(this, argu);
        // n.f4.accept(this, argu);
        // n.f5.accept(this, argu);
        // n.f6.accept(this, argu);
        // n.f7.accept(this, argu);
        // n.f8.accept(this, argu);
        // n.f9.accept(this, argu);
        // n.f10.accept(this, argu);
        // n.f11.accept(this, argu);
        // n.f12.accept(this, argu);
        // n.f13.accept(this, argu);
        if(n.f14.present())
            n.f14.accept(this, className + "::main");
            
        if(n.f15.present())
            n.f15.accept(this, className + "::main");
        // n.f16.accept(this, argu);
        // n.f17.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> ClassDeclaration() | ClassExtendsDeclaration()
     * 
     *
     */
    @Override
    public String visit(TypeDeclaration n, String argu) throws Exception {
        return n.f0.accept(this, argu);
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

        classToVarOffset.put(className, 0);
        classToMethodOffset.put(className, 0);
        classToOffsetMap.put(className, new OffsetMaps());

        //n.f2.accept(this, argu);
        if( n.f3.present() )
            n.f3.accept(this, className);

        if( n.f4.present() )
            n.f4.accept(this, className);
        //n.f5.accept(this, argu);
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
        //n.f0.accept(this, argu);
        String className = n.f1.accept(this, argu);
        String parentClass = n.f3.accept(this, argu);

        int parentClassVarOffset = classToVarOffset.get(parentClass);
        int parentClassMethodOffset = classToMethodOffset.get(parentClass);

        //n.f2.accept(this, argu);

        classToVarOffset.put(className, parentClassVarOffset);
        classToMethodOffset.put(className, parentClassMethodOffset);
        classToOffsetMap.put(className, new OffsetMaps());

        //n.f4.accept(this, argu);
        if( n.f5.present() )
            n.f5.accept(this, className);

        if( n.f6.present() )    
            n.f6.accept(this, className);
        //n.f7.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    @Override
    public String visit(VarDeclaration n, String argu) throws Exception {
        String _ret = null;
        String type = n.f0.accept(this, argu);
        String varName = n.f1.accept(this, argu);
        HashMap<String, String> currScope = scopeToVars.get(argu);
        String seekType = currScope.get(varName);

        if (seekType != null) {
            throw new Exception("Scope: " + argu + "\n\tRedefinition Error: variable \"" + varName + "\" has already been defined as type "
                    + seekType + ".");
        } else {
            currScope.put(varName, type);

            if(!argu.contains("::"))
            {
                String currClass = argu;

                
                    int currVarOffset = classToVarOffset.get(currClass);

                    classToOffsetMap.get(currClass).variableOffsets.put(varName, currVarOffset);
                    
                    switch(type)
                    {

                        case "int": 
                            classToVarOffset.replace(currClass, currVarOffset + 4);
                            break;

                        case "boolean": 
                            classToVarOffset.replace(currClass, currVarOffset + 1);
                            break;

                        default: 
                            classToVarOffset.replace(currClass, currVarOffset + 8);
                            break;
                    }
            }
        }
        // n.f2.accept(this, argu);
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

        String currScope = argu + "::" + methodName;
        
        if( n.f7.present() )
            n.f7.accept(this, currScope);

        if( n.f8.present() )
            n.f8.accept(this, currScope);

        //n.f9.accept(this, argu);
        String returnExprType = n.f10.accept(this, currScope);
        if( !isAncestorOf(returnExprType, methodType) )
            throw new Exception("Scope: " + currScope + "\n\tError: Cannot return value of type " + returnExprType + " when expecting type " +  methodType + ".");
        //n.f11.accept(this, argu);
        //n.f12.accept(this, argu);


        if( !overrides(methodName, argu) )
        {
            int currMethodOffset = classToMethodOffset.get(argu);
            classToMethodOffset.replace(argu, currMethodOffset + 8);
            classToOffsetMap.get(argu).methodOffsets.put(methodName, currMethodOffset);
        }

        return _ret;
    }

    /**
     * f0 -> ArrayType() | BooleanType() | IntegerType() | Identifier()
     */
    @Override
    public String visit(Type n, String argu) throws Exception {
        return n.f0.accept(this, argu);
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

    /**
     * f0 -> Block() | AssignmentStatement() | ArrayAssignmentStatement() |
     * IfStatement() | WhileStatement() | PrintStatement()
     */
    @Override
    public String visit(Statement n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "{"
     * f1 -> ( Statement() )*
     * f2 -> "}"
     */
    @Override
    public String visit(Block n, String argu) throws Exception {
        String _ret = null;
        // n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        // n.f2.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    @Override
    public String visit(AssignmentStatement n, String argu) throws Exception {
        String _ret = null;
        String varName = n.f0.accept(this, argu);
        String soughtVarType = findVarType(varName, argu);

        if (soughtVarType == null)
            throw new Exception("Scope: " + argu + "\n\tError: Variable " + varName + " has not been declared.");

        // n.f1.accept(this, argu);
        String exprType = n.f2.accept(this, argu);
        if (!exprType.equals(soughtVarType))
            throw new Exception("Scope: " + argu + "\n\tError: Cannot assign value of type " + exprType + " to variable  " + varName
                    + " of type " + soughtVarType + ".");
        // n.f3.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    @Override
    public String visit(ArrayAssignmentStatement n, String argu) throws Exception {
        String _ret = null;
        String varName = n.f0.accept(this, argu);
        String soughtVarType = findVarType(varName, argu);

        if (soughtVarType == null)
            throw new Exception("Scope: " + argu + "\n\tError: Array variable " + varName + " has not been declared.");

        // n.f1.accept(this, argu);
        String indexExprType = n.f2.accept(this, argu);
        if (indexExprType != "int")
            throw new Exception("Scope: " + argu + "\n\tError: Array index must be of integer type.");

        // n.f3.accept(this, argu);
        // n.f4.accept(this, argu);
        String assignmentExprType = n.f5.accept(this, argu);
        if (assignmentExprType != "int" || soughtVarType != "array")
            throw new Exception("Scope: " + argu + "\n\tError: Cannot assign value of type " + assignmentExprType + " to array variable  "
                    + varName + " of type " + soughtVarType + ".");
        // n.f6.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    @Override
    public String visit(IfStatement n, String argu) throws Exception {
        String _ret = null;
        // n.f0.accept(this, argu);
        // n.f1.accept(this, argu);
        String condExprType = n.f2.accept(this, argu);
        if (condExprType != "boolean")
            throw new Exception("Scope: " + argu + "\n\tError: Condition value must be of boolean type.");
        // n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        // n.f5.accept(this, argu);
        n.f6.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    @Override
    public String visit(WhileStatement n, String argu) throws Exception {
        String _ret = null;
        // n.f0.accept(this, argu);
        // n.f1.accept(this, argu);
        String condExprType = n.f2.accept(this, argu);
        if (condExprType != "boolean")
            throw new Exception("Scope: " + argu + "\n\tError: Condition value must be of boolean type.");
        // n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    @Override
    public String visit(PrintStatement n, String argu) throws Exception {
        String _ret = null;
        // n.f0.accept(this, argu);
        // n.f1.accept(this, argu);
        String exprType = n.f2.accept(this, argu);
        if( exprType != "array" && exprType != "int" && exprType != "boolean")
            throw new Exception("Scope: " + argu + "\n\tError: Print statement can only have variables of primitive type as arguments.");
        // n.f3.accept(this, argu);
        // n.f4.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> AndExpression() | CompareExpression() | PlusExpression() |
     * MinusExpression() | TimesExpression() | ArrayLookup() | ArrayLength() |
     * MessageSend() | Clause()
     */
    @Override
    public String visit(Expression n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    @Override
    public String visit(AndExpression n, String argu) throws Exception {
        String clause1 = n.f0.accept(this, argu);
        // n.f1.accept(this, argu);
        String clause2 = n.f2.accept(this, argu);
        if (clause1 != "boolean" || clause2 != "boolean")
            throw new Exception("Scope: " + argu + "\n\tError: && operator supports only arguments of type boolean.");

        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(CompareExpression n, String argu) throws Exception {
        String expr1 = n.f0.accept(this, argu);
        // n.f1.accept(this, argu);
        String expr2 = n.f2.accept(this, argu);

        if (expr1 != "int" || expr2 != "int")
            throw new Exception("Scope: " + argu + "\n\tError: < operator supports only arguments of type integer.");

        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(PlusExpression n, String argu) throws Exception {
        String expr1 = n.f0.accept(this, argu);
        // n.f1.accept(this, argu);
        String expr2 = n.f2.accept(this, argu);

        if (expr1 != "int" || expr2 != "int")
            throw new Exception("Scope: " + argu + "\n\tError: + operator supports only arguments of type integer.");
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(MinusExpression n, String argu) throws Exception {
        String expr1 = n.f0.accept(this, argu);
        // n.f1.accept(this, argu);
        String expr2 = n.f2.accept(this, argu);

        if (expr1 != "int" || expr2 != "int")
            throw new Exception("Scope: " + argu + "\n\tError: - operator supports only arguments of type integer.");
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(TimesExpression n, String argu) throws Exception {
        
        String expr1 = n.f0.accept(this, argu);
        // n.f1.accept(this, argu);
        String expr2 = n.f2.accept(this, argu);

        if (expr1 != "int" || expr2 != "int")
            throw new Exception("Scope: " + argu + "\n\tError: * operator supports only arguments of type integer.");
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    @Override
    public String visit(ArrayLookup n, String argu) throws Exception {
        String arrayExpr = n.f0.accept(this, argu);
        if( arrayExpr != "array")
            throw new Exception("Scope: " + argu + "\n\tError: [] operator can only be applied to an array variable.");
        
        //n.f1.accept(this, argu);
        String indexExpr = n.f2.accept(this, argu);
        if( indexExpr != "int")
            throw new Exception("Scope: " + argu + "\n\tError: Array index must be of integer type.");
        //n.f3.accept(this, argu);
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    @Override
    public String visit(ArrayLength n, String argu) throws Exception {
        String varType = n.f0.accept(this, argu);
        if( varType != "array" )
            throw new Exception("Scope: " + argu + "\n\tError: .length operator can only be used on an array variable.");
        //n.f1.accept(this, argu);
        //n.f2.accept(this, argu);
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    @Override
    public String visit(MessageSend n, String argu) throws Exception {
        String classType = n.f0.accept(this, argu);
        //n.f1.accept(this, argu);
        String methodName = n.f2.accept(this, argu);
        List<String> methodData = findMethodData(methodName, classType);
        //n.f3.accept(this, argu);

        if(methodData == null)
            throw new Exception("Scope: " + argu + "\n\tError: Method " + methodName + " not defined in class " + classType + " or any of it's superclasses.");
        
        if( n.f4.present() )
        {
            n.f4.accept(this, argu);
            
            List<String> methodArgs = methodData.subList(1, methodData.size());
            if(methodArgs.size() != argList.size())
                throw new Exception("Scope: " + argu + "\n\tError: No method " + argu + "::" + methodName + " with " + argList.size() + " argument(s) has been defined.");

            for(int i = 0; i < methodArgs.size(); i++)
                if( !isAncestorOf(argList.get(i), methodArgs.get(i)) ) // O(1) time due to using ArrayList
                    throw new Exception("Scope: " + argu + "\n\tError: Method " + classType + "::" + methodName + " expects argument of type " + methodArgs.get(i) + " at argument index " + i + ".");
            
            argList.clear();
        }

        //n.f5.accept(this, argu);
        return methodData.get(0);
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    @Override
    public String visit(ExpressionList n, String argu) throws Exception {
        String _ret = null;
        argList.add(n.f0.accept(this, argu));
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    @Override
    public String visit(ExpressionTail n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    @Override
    public String visit(ExpressionTerm n, String argu) throws Exception {
        String _ret = null;
        //n.f0.accept(this, argu);
        argList.add(n.f1.accept(this, argu));
        return _ret;
    }

    /**
     * f0 -> NotExpression() | PrimaryExpression()
     */
    @Override
    public String visit(Clause n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> IntegerLiteral() | TrueLiteral() | FalseLiteral() | Identifier() |
     * ThisExpression() | ArrayAllocationExpression() | AllocationExpression() |
     * BracketExpression()
     */
    @Override
    public String visit(PrimaryExpression n, String argu) throws Exception {
        if( n.f0.which == 3 )
        {
            String varName = n.f0.accept(this, argu);
            String varType = findVarType(varName, argu);

            if(varType == null)
                throw new Exception("Scope: " + argu + "\n\tError: Identifier " + varName + " not found.");

            return varType;
        }

        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    @Override
    public String visit(IntegerLiteral n, String argu) throws Exception {
        return "int";
        //n.f0.accept(this, argu);
    }

    /**
     * f0 -> "true"
     */
    @Override
    public String visit(TrueLiteral n, String argu) throws Exception {
        return "boolean"; 
        //n.f0.accept(this, argu);
    }

    /**
     * f0 -> "false"
     */
    @Override
    public String visit(FalseLiteral n, String argu) throws Exception {
        //n.f0.accept(this, argu);
        return "boolean"; 
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    @Override
    public String visit(Identifier n, String argu) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> "this"
     */
    @Override
    public String visit(ThisExpression n, String argu) throws Exception {
        return argu.split("::", 2)[0];
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    @Override
    public String visit(ArrayAllocationExpression n, String argu) throws Exception {
        //n.f0.accept(this, argu);
        //n.f1.accept(this, argu);
        //n.f2.accept(this, argu);
        String countExpr = n.f3.accept(this, argu);
        if( countExpr != "int" )
            throw new Exception("Scope: " + argu + "\n\tError: Array index must be of integer type.");
        //n.f4.accept(this, argu);
        return "array";
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    @Override
    public String visit(AllocationExpression n, String argu) throws Exception {
        //n.f0.accept(this, argu);
        String className = n.f1.accept(this, argu);
        if(!classToMethods.containsKey(className) )
            throw new Exception("Scope: " + argu + "\n\tError: Class " + className + " has not been defined.");
        //n.f2.accept(this, argu);
        //n.f3.accept(this, argu);
        return className;
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    @Override
    public String visit(NotExpression n, String argu) throws Exception {
        //n.f0.accept(this, argu);
        if(n.f1.accept(this, argu) != "boolean")
            throw new Exception("Scope: " + argu + "\n\tError: ! operator requires a boolean type argument.");
        
        return "boolean";
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    @Override
    public String visit(BracketExpression n, String argu) throws Exception {
        return n.f1.accept(this, argu);
    }
}