import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import syntaxtree.*;
import visitor.GJDepthFirst;


public class MainVisitor extends GJDepthFirst<String, String> {

    Map<String, Map<String, List<Argument>>> classToMethods;
    Map<String, Map<String, String>> scopeToVars;
    Map<String, String> inheritanceChain;

    private HashMap<String, Integer> classToVarOffset;
    private HashMap<String, Integer> classToMethodOffset;

    private Integer currVarOffset;
    private Integer currMethodOffset;

    Map<String, OffsetMaps> classToOffsetMap;

    private List<String> argList;

    public MainVisitor( Map<String, Map<String, List<Argument>>> classToMethods,
                        Map<String, Map<String, String>> scopeToVars,
                        Map<String, String> inheritanceChain,
                        Map<String, OffsetMaps> classToOffsetMap ) throws Exception 
    {
        super();
        this.classToMethods = classToMethods;
        this.scopeToVars = scopeToVars;
        this.inheritanceChain = inheritanceChain;

        this.classToVarOffset = new HashMap<String, Integer>();
        this.classToMethodOffset = new HashMap<String, Integer>();
        this.classToOffsetMap = classToOffsetMap;

        this.currMethodOffset = 0;
        this.currVarOffset = 0;

        this.argList = new ArrayList<String>();
    }

    // Utility Functions

    private String findVarType(String varName, String startScope) { // Given a variable name and a scope, return it's type if it exists, otherwise return null

        if(startScope == null)
            return null;

        Map<String, String> vars = scopeToVars.get(startScope);
        String varType = vars.get(varName);
        if ( varType == null)
        {
            String parentScope = inheritanceChain.get(startScope);
            return findVarType(varName, parentScope);
        }

        return varType;
    }

    private List<Argument> findMethodData(String methodName, String startScope) { // Given a method name and a scope, return it's data if it exists, otherwise return null

        if(startScope == null)
            return null;

        Map<String, List<Argument>> methods = classToMethods.get(startScope);
        List<Argument> args = methods.get(methodName);
        if( args == null )
        {
            String parentClass = inheritanceChain.get(startScope);
            return findMethodData(methodName, parentClass);
        }
        
        return args;
    }

    private boolean isAncestorOf(String offspringClass, String ancestorClass) { // Check if offspringClass could be traced back to ancestorClass through inheritance

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

    private boolean overrides(String methodName, String className) { // Check if method methodName would cause overriding if it were to be a member of class className 

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
        classToVarOffset.put(className, 0);
        classToMethodOffset.put(className, 0);

        classToOffsetMap.put(className, new OffsetMaps());

        if(n.f14.present())
            n.f14.accept(this, className + ".main");
            
        if(n.f15.present())
            n.f15.accept(this, className + ".main");

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

        // Init class offset
        
        currVarOffset = 0;
        currMethodOffset = 0;

        classToOffsetMap.put(className, new OffsetMaps());

        if( n.f3.present() )
            n.f3.accept(this, className);

        if( n.f4.present() )
            n.f4.accept(this, className);

        classToVarOffset.put(className, currVarOffset);
        classToMethodOffset.put(className, currMethodOffset);

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
        String parentClass = n.f3.accept(this, argu);

        int currVarOffset = classToVarOffset.get(parentClass);
        int currMethodOffset = classToMethodOffset.get(parentClass);

        classToOffsetMap.put(className, new OffsetMaps());

        if( n.f5.present() )
            n.f5.accept(this, className);

        if( n.f6.present() )    
            n.f6.accept(this, className);

        // Init class offset, keeping in mind the parent offset
        classToVarOffset.put(className, currVarOffset);
        classToMethodOffset.put(className, currMethodOffset);

        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    @Override
    public String visit(VarDeclaration n, String argu) throws Exception {
        String type = n.f0.accept(this, argu);

        // Check if class TYPE already exists
        if( type != "int" && type != "array" && type != "boolean" && classToMethods.get(type) == null )
            throw new Exception("Scope: " + argu + "\n\tError: Class " + type + " has not been defined.");

        String varName = n.f1.accept(this, argu);
        Map<String, String> currScope = scopeToVars.get(argu);
        String seekType = currScope.get(varName);

        if (seekType != null) {
            throw new Exception("Scope: " + argu + "\n\tRedefinition Error: variable \"" + varName + "\" has already been defined as type "
                    + seekType + ".");
        } else {
            currScope.put(varName, type);

            // Calculate offsets if at class variable declaration scope and not inside a method
            if(!argu.contains("."))
            {
                String currClass = argu;

                OffsetMaps mp = classToOffsetMap.get(currClass);
                mp.variableOffsets.put(varName, currVarOffset);
                switch(type)
                {
                    case "int": currVarOffset += 4; mp.totalVarOffset += 4; break;

                    case "boolean": currVarOffset += 1; mp.totalVarOffset += 1; break;

                    default: currVarOffset += 8; mp.totalVarOffset += 8; break;
                }
            }
        }

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

        String currScope = argu + "." + methodName;
        
        if( n.f7.present() )
            n.f7.accept(this, currScope);

        if( n.f8.present() )
            n.f8.accept(this, currScope);

        String returnExprType = n.f10.accept(this, currScope);

        // Check for if return type is correct (also checks for polymorphism)
        if( !isAncestorOf(returnExprType, methodType) )
            throw new Exception("Scope: " + currScope + "\n\tError: Cannot return value of type " + returnExprType + " when expecting type " +  methodType + ".");

        // Check if the method declared will override another method in order to calculate the offsets correctly
        if( !overrides(methodName, argu) )
        {
            OffsetMaps mp = classToOffsetMap.get(argu);
            mp.methodOffsets.put(methodName, currMethodOffset);
            mp.totalMethodOffset += 8;
            currMethodOffset += 8;
        }

        return null;
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

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    @Override
    public String visit(AssignmentStatement n, String argu) throws Exception {
        String varName = n.f0.accept(this, argu);
        String soughtVarType = findVarType(varName, argu);

        if (soughtVarType == null)
            throw new Exception("Scope: " + argu + "\n\tError: Variable " + varName + " has not been declared.");


        String exprType = n.f2.accept(this, argu);

        // Check if assignment type is correct (checks for polymorphism)
        if (!isAncestorOf(exprType, soughtVarType))
            throw new Exception("Scope: " + argu + "\n\tError: Cannot assign value of type " + exprType + " to variable " + varName
                    + " of type " + soughtVarType + ".");

        return null;
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
        String varName = n.f0.accept(this, argu);
        String soughtVarType = findVarType(varName, argu);

        if (soughtVarType == null)
            throw new Exception("Scope: " + argu + "\n\tError: Array variable " + varName + " has not been declared.");

        String indexExprType = n.f2.accept(this, argu);
        if (indexExprType != "int")
            throw new Exception("Scope: " + argu + "\n\tError: Array index must be of integer type.");

        String assignmentExprType = n.f5.accept(this, argu);
        if (assignmentExprType != "int" || soughtVarType != "array")
            throw new Exception("Scope: " + argu + "\n\tError: Cannot assign value of type " + assignmentExprType + " to array variable "
                    + varName + " of type " + soughtVarType + ".");

        return null;
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
        String condExprType = n.f2.accept(this, argu);

        if (condExprType != "boolean")
            throw new Exception("Scope: " + argu + "\n\tError: Condition value must be of boolean type.");

        n.f4.accept(this, argu);
        n.f6.accept(this, argu);

        return null;
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
        String condExprType = n.f2.accept(this, argu);

        if (condExprType != "boolean")
            throw new Exception("Scope: " + argu + "\n\tError: Condition value must be of boolean type.");

        n.f4.accept(this, argu);
        return null;
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
        String exprType = n.f2.accept(this, argu);

        if( exprType != "int" && exprType != "boolean" )
            throw new Exception("Scope: " + argu + "\n\tError: Print statement can only have variables of primitive type as arguments.");

        return null;
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    @Override
    public String visit(AndExpression n, String argu) throws Exception {
        String clause1 = n.f0.accept(this, argu);
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
        
        String indexExpr = n.f2.accept(this, argu);
        if( indexExpr != "int")
            throw new Exception("Scope: " + argu + "\n\tError: Array index must be of integer type.");

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
        String methodName = n.f2.accept(this, argu);
        List<Argument> methodData = findMethodData(methodName, classType); // methodData have a strict format: at index 0, the return type resides and at all other indexes, the argument types are placed
        
        if(methodData == null)
            throw new Exception("Scope: " + argu + "\n\tError: Method " + methodName + " not defined in class " + classType + " or any of it's superclasses.");
        
        if( n.f4.present() )
        {
            n.f4.accept(this, argu);
            
            List<Argument> methodArgs = methodData.subList(1, methodData.size()); // Get only the argument types and not the return type
            List<String> methodArgTypes = Argument.typeList(methodArgs);
            if(methodArgTypes.size() != argList.size())
                throw new Exception("Scope: " + argu + "\n\tError: No method " + argu + "." + methodName + " with " + argList.size() + " argument(s) has been defined.");

            for(int i = 0; i < methodArgTypes.size(); i++)
                if( !isAncestorOf(argList.get(i), methodArgTypes.get(i)) ) // O(1) time complexity on List.get() due to using ArrayList
                    throw new Exception("Scope: " + argu + "\n\tError: Method " + classType + "." + methodName + " expects argument of type " + methodArgTypes.get(i) + " at argument index " + i + ".");
            
            argList.clear();
        }

        return methodData.get(0).argumentType; // If all checks have been successful, then return the method return type as this expression's type
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    @Override
    public String visit(ExpressionList n, String argu) throws Exception {
        argList.add(n.f0.accept(this, argu));
        n.f1.accept(this, argu);
        return null;
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    @Override
    public String visit(ExpressionTerm n, String argu) throws Exception {
        argList.add(n.f1.accept(this, argu));
        return null;
    }

    /**
     * f0 -> IntegerLiteral() | TrueLiteral() | FalseLiteral() | Identifier() |
     * ThisExpression() | ArrayAllocationExpression() | AllocationExpression() |
     * BracketExpression()
     */
    @Override
    public String visit(PrimaryExpression n, String argu) throws Exception {
        if( n.f0.which == 3 ) // If expression is an identifier (in this case a variable), return the variable's type
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
    }

    /**
     * f0 -> "true"
     */
    @Override
    public String visit(TrueLiteral n, String argu) throws Exception {
        return "boolean"; 
    }

    /**
     * f0 -> "false"
     */
    @Override
    public String visit(FalseLiteral n, String argu) throws Exception {
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
        return argu.split("\\.")[0];
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
        String className = n.f1.accept(this, argu);

        if(!classToMethods.containsKey(className) )
            throw new Exception("Scope: " + argu + "\n\tError: Class " + className + " has not been defined.");

        return className;
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    @Override
    public String visit(NotExpression n, String argu) throws Exception {
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