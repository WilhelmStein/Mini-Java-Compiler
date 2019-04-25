import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import syntaxtree.*;
import visitor.GJDepthFirst;


public class MainVisitor extends GJDepthFirst<String, String> {

    HashMap<String, HashMap<String, List<String>>> classToMethods;
    HashMap<String, HashMap<String, String>> scopeToVars;
    HashMap<String, String> inheritanceChain;

    private List<String> argList;

    public MainVisitor( HashMap<String, HashMap<String, List<String>>> classToMethods,
                        HashMap<String, HashMap<String, String>> scopeToVars,
                        HashMap<String, String> inheritanceChain ) throws Exception 
    {
        super();
        this.classToMethods = classToMethods;
        this.scopeToVars = scopeToVars;
        this.inheritanceChain = inheritanceChain;
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

        classToMethods.put(className + ".main", new HashMap<String, List<String>>());
        scopeToVars.put(className + ".main", new HashMap<String, String>());
        inheritanceChain.put(className + ".main", className);

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
            n.f14.accept(this, className + ".main");
            
        if(n.f15.present())
            n.f15.accept(this, className + ".main");
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
        //n.f2.accept(this, argu);
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
            throw new Exception("Redefinition Error: variable \"" + varName + "\" has already been defined as type "
                    + seekType + ".");
        } else {
            currScope.put(varName, type);
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

        String currScope = argu + "." + methodName;
        
        if( n.f7.present() )
            n.f7.accept(this, currScope);

        if( n.f8.present() )
            n.f8.accept(this, currScope);

        //n.f9.accept(this, argu);
        String returnExprType = n.f10.accept(this, currScope);
        if( returnExprType != methodType )
            throw new Exception("Error: Cannot return value of type " + returnExprType + " when expecting type " +  methodType + ".");
        //n.f11.accept(this, argu);
        //n.f12.accept(this, argu);
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
            throw new Exception("Error: Variable " + varName + " has not been declared.");

        // n.f1.accept(this, argu);
        String exprType = n.f2.accept(this, argu);
        if (exprType != soughtVarType)
            throw new Exception("Error: Cannot assign value of type " + exprType + " to variable  " + varName
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
            throw new Exception("Error: Array variable " + varName + " has not been declared.");

        // n.f1.accept(this, argu);
        String indexExprType = n.f2.accept(this, argu);
        if (indexExprType != "int")
            throw new Exception("Error: Array index must be of integer type.");

        // n.f3.accept(this, argu);
        // n.f4.accept(this, argu);
        String assignmentExprType = n.f5.accept(this, argu);
        if (assignmentExprType != "int" || soughtVarType != "array")
            throw new Exception("Error: Cannot assign value of type " + assignmentExprType + " to array variable  "
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
            throw new Exception("Error: Condition value must be of boolean type.");
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
            throw new Exception("Error: Condition value must be of boolean type.");
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
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
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
            throw new Exception("Error: && operator supports only arguments of type boolean.");

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
            throw new Exception("Error: < operator supports only arguments of type integer.");

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
            throw new Exception("Error: + operator supports only arguments of type integer.");
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
            throw new Exception("Error: - operator supports only arguments of type integer.");
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
            throw new Exception("Error: * operator supports only arguments of type integer.");
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
            throw new Exception("Error: [] operator can only be applied to an array variable.");
        
        //n.f1.accept(this, argu);
        String indexExpr = n.f2.accept(this, argu);
        if( indexExpr != "int")
            throw new Exception("Error: Array index must be of integer type.");
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
            throw new Exception("Error: .length operator can only be used on an array variable.");
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
            throw new Exception("Error: Method " + methodName + " not defined in class " + classType + " or any of it's superclasses.");
        
        if( n.f4.present() )
        {
            n.f4.accept(this, argu);

            if( !methodData.subList(1, methodData.size()).equals(argList) )
                throw new Exception("Error: Argument list not as expected.");

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
                throw new Exception("Error: Identifier " + varName + " not found.");

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
        return argu.split("\\.", 2)[0];
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
            throw new Exception("Error: Array index must be of integer type.");
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
            throw new Exception("Error: Class " + className + " has not been defined.");
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
            throw new Exception("Error: ! operator requires a boolean type argument.");
        
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