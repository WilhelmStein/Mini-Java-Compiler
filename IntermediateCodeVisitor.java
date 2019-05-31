import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;
import syntaxtree.*;
import visitor.GJDepthFirst;

public class IntermediateCodeVisitor extends GJDepthFirst<String, String> {

    Map<String, Map<String, List<Argument>>> classToMethods;
    Map<String, Map<String, String>> scopeToVars;
    Map<String, String> inheritanceChain;
    Map<String, OffsetMaps> classToOffsetMap;

    private String metaVar;

    private FileOutputStream llvmFos;
    private String tabsToEmit;
    private int regCount;

    private Stack<List<String>> argListStack;

    public IntermediateCodeVisitor( String fileName,
                                    Map<String, OffsetMaps> classToOffsetMap,
                                    Map<String, Map<String, String>> scopeToVars,
                                    Map<String, String> inheritanceChain,
                                    Map<String, Map<String, List<Argument>>> classToMethods) throws Exception
    {
        super();

        this.metaVar = "";
        this.tabsToEmit = "";
        this.regCount = 0;

        this.classToMethods = classToMethods;
        this.scopeToVars = scopeToVars;
        this.inheritanceChain = inheritanceChain;
        this.classToOffsetMap = classToOffsetMap;

        this.argListStack = new Stack<List<String>>();

        String llvmFileName;

        if(fileName.endsWith(".javaa") || fileName.endsWith(".java"))
        {
            String baseFile = fileName.substring(0, fileName.lastIndexOf("."));
            llvmFileName = baseFile + ".ll";
        }
        else
        {
            llvmFileName = fileName + ".ll";
        }

        // Open llvm output file
        File llvmFile = new File(llvmFileName);
        llvmFos = new FileOutputStream(llvmFile);

        // Create Vtable

        Map<String, OffsetMaps> mergedOffsetMaps = new LinkedHashMap<String, OffsetMaps>();

        for (Entry<String, OffsetMaps> offsetMap : classToOffsetMap.entrySet()) {
            
            pureEmit("@." + offsetMap.getKey() + "_vtable = global [");


            OffsetMaps mergedMap = mergeOffsetMaps(offsetMap.getKey(), offsetMap.getKey());
            mergedMap.totalVarOffset = offsetMap.getValue().totalVarOffset;
            mergedMap.totalMethodOffset = offsetMap.getValue().totalMethodOffset;
            mergedOffsetMaps.put(offsetMap.getKey(), mergedMap);

            if(!mergedMap.methodOffsets.isEmpty())
            {
                
                pureEmit(mergedMap.methodOffsets.size() + " x i8*] [");
                boolean firstMethod = true;
                for(Entry<String, OffsetMapData> entry : mergedMap.methodOffsets.entrySet()) {
                    

                    if(!firstMethod)
                        pureEmit(",\n\t");
                    else
                        firstMethod = false;
                    
                    List<Argument> methodData = findMethodData(entry.getKey(), offsetMap.getKey());
                    List<Argument> args = methodData.subList(1, methodData.size());    
                    
                    pureEmit("i8* bitcast (" + javaToLlvmType(methodData.get(0).argumentType) + " (i8*");

                    for (Argument arg : args) {
                        pureEmit(", " + javaToLlvmType(arg.argumentType));
                    }

                    pureEmit(")* @" + entry.getValue().className + "." + entry.getKey() + " to i8*)");

                }
                pureEmit("]\n");

            }
            else
            {
                pureEmit("0 x i8*] []\n\n");
            }
        }

        this.classToOffsetMap = mergedOffsetMaps;

        pureEmit( "declare i8* @calloc(i32, i32)\n"
        + "declare i32 @printf(i8*, ...)\n"
        + "declare void @exit(i32)\n\n"
        + "@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n"
        + "@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n"
        + "@_testMsg = constant [6 x i8] c\"TEST\\0a\\00\"\n"
        + "define void @print_int(i32 %i) {\n"
        + "\t%_str = bitcast [4 x i8]* @_cint to i8*\n"
        + "\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n"
        + "\tret void\n"
        + "}\n\n"
        + "define void @throw_oob() {\n"
        + "\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n"
        + "\tcall i32 (i8*, ...) @printf(i8* %_str)\n"
        + "\tcall void @exit(i32 1)\n"
        + "\tret void\n"
        + "}\n\n"
        + "define void @TEST() {\n"
        + "\t%_str = bitcast [6 x i8]* @_testMsg to i8*\n"
        + "\tcall i32 (i8*, ...) @printf(i8* %_str)\n"
        + "\tret void\n"
        + "}\n\n");
        
    }

    // Utility Functions

    private void emit(String code) throws Exception {
        llvmFos.write((tabsToEmit + code + "\n").getBytes());
    }

    private void pureEmit(String code) throws Exception {
        llvmFos.write(code.getBytes());
    }

    private String javaToLlvmType(String type) {
        switch(type)
        {
            case "int":
                return "i32";

            case "boolean":
                return "i1";
            
            case "array":
                return "i32*";

            default:
                return "i8*";
        }
    }

    private String getMeta() {
        String tmp = new String(this.metaVar);
        this.metaVar = "";
        return tmp;
    }

    private int nextReg() {
        return regCount++;
    }

    private void increaseTabs()
    {
        tabsToEmit += "\t";
    }

    private void decreaseTabs()
    {
        tabsToEmit = tabsToEmit.substring(0, tabsToEmit.length() - 1);
    }

    private OffsetMaps mergeOffsetMaps(String className, String bottomClass) {
        String parentClass = inheritanceChain.get(className);

        if(parentClass == null)
        {
            OffsetMaps map = classToOffsetMap.get(className);
            OffsetMaps out = new OffsetMaps(className);

            
            for (Map.Entry<String, OffsetMapData> variable : map.variableOffsets.entrySet()) {
                out.variableOffsets.put(variable.getKey(), variable.getValue());
            }

            for (Map.Entry<String, OffsetMapData> method : map.methodOffsets.entrySet()) {
                
                OffsetMapData temp = new OffsetMapData(method.getValue().offset, method.getValue().className);
                if(classToMethods.get(bottomClass).containsKey(method.getKey()))
                {
                    temp.className = bottomClass;
                }

                out.methodOffsets.put(method.getKey(), temp);
            }

            return out;
        }

        OffsetMaps output = mergeOffsetMaps(parentClass, bottomClass);

        OffsetMaps map = classToOffsetMap.get(className);
        for (Map.Entry<String, OffsetMapData> variable : map.variableOffsets.entrySet()) {
            output.variableOffsets.put(variable.getKey(), variable.getValue());
        }

        for (Map.Entry<String, OffsetMapData> method : map.methodOffsets.entrySet()) {

            OffsetMapData temp = new OffsetMapData(method.getValue().offset, method.getValue().className);
            if(classToMethods.get(bottomClass).containsKey(method.getKey()))
            {
                temp.className = bottomClass;
            }

            output.methodOffsets.put(method.getKey(), temp);
        }

        return output;
    }

    private List<Argument> findMethodData(String methodName, String startScope) {

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


        // After the traversal of the AST, close the output file
        llvmFos.close();
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
        regCount = 0;
        emit("define i32 @main() {\n");

        increaseTabs();

        if(n.f14.present())
            n.f14.accept(this, className + ".main");
            
        if(n.f15.present())
            n.f15.accept(this, className + ".main");
        
        emit("ret i32 0");

        decreaseTabs();
        emit("}\n");

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
        
        if( n.f3.present() )
            n.f3.accept(this, className);

        //increaseTabs();
        
        if( n.f4.present() )
            n.f4.accept(this, className);
        
        //decreaseTabs();

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

        if( n.f5.present() )
            n.f5.accept(this, className);

        //increaseTabs();

        if( n.f6.present() )    
            n.f6.accept(this, className);
        
        //decreaseTabs();

        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    @Override
    public String visit(VarDeclaration n, String argu) throws Exception {

        if( !tabsToEmit.equals("") )
        {
            String llvmType = javaToLlvmType(n.f0.accept(this, argu));
            String varName = n.f1.accept(this, argu);

            emit("%" + varName + " = alloca " + llvmType + "\n");
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
        regCount = 0;
        String llvmMethodType = javaToLlvmType(n.f1.accept(this, argu));
        String methodName = n.f2.accept(this, argu);

        String currScope = argu + "." + methodName;

        List<Argument> methodData = findMethodData(methodName, argu);
        List<Argument> args = methodData.subList(1, methodData.size());
        String argLlvmCode = "";

        for (Argument arg : args) {
            argLlvmCode += (", " + javaToLlvmType(arg.argumentType) + " %." + arg.argumentName);
        }

        emit("define " + llvmMethodType + " @" + currScope + "(i8* %this" + argLlvmCode + ") {");

        increaseTabs();

        for (Argument arg : args) {
            String llvmType = javaToLlvmType(arg.argumentType);
            emit("%" + arg.argumentName + " = alloca " + llvmType);
            emit("store " + llvmType + "%." + arg.argumentName + ", " + llvmType + "* %" + arg.argumentName);
        }

        if( n.f7.present() )
            n.f7.accept(this, currScope);

        if( n.f8.present() )
            n.f8.accept(this, currScope);

        String returnReg = n.f10.accept(this, currScope);
        emit("ret " + llvmMethodType + " " + returnReg );
        decreaseTabs();
        emit("}\n");

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
        String varType = findVarType(varName, argu);
        String llvmType = javaToLlvmType(varType);
        String exprReg = n.f2.accept(this, argu);

        String varReg;

        if(!scopeToVars.get(argu).containsKey(varName))
        {
            String elementPtrReg = "%_" + nextReg();
            String bitcastReg = "%_" + nextReg();

            int varOffset = classToOffsetMap.get(argu.split("\\.")[0]).variableOffsets.get(varName).offset + 8;

            emit(elementPtrReg + " = getelementptr i8, i8* %this, i32 " + varOffset);
            emit(bitcastReg + " = bitcast i8* " + elementPtrReg + " to " + llvmType + "*");
            varReg = bitcastReg;
        }
        else
            varReg = "%" + varName;
        
        emit("store " + llvmType  + " " + exprReg + ", " + llvmType + "* " + varReg);

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

        String indexExprReg = n.f2.accept(this, argu);
        String exprReg = n.f5.accept(this, argu);


        String arraySizeReg = "%_" + nextReg();
        String icmpReg = "%_" + nextReg();
 
        String oobEntry = "oob" + nextReg();
        String oobExit = "oob" + nextReg();

        String arrayReg;
        String lookupReg = "%_" + nextReg();

        String lookupIndexReg = "%_" + nextReg();


        if(!scopeToVars.get(argu).containsKey(varName))
        {
            String elementPtrReg = "%_" + nextReg();
            String bitcastReg = "%_" + nextReg();

            int varOffset = classToOffsetMap.get(argu.split("\\.")[0]).variableOffsets.get(varName).offset + 8;

            emit(elementPtrReg + " = getelementptr i8, i8* %this, i32 " + varOffset);
            emit(bitcastReg + " = bitcast i8* " + elementPtrReg + " to i32**");

            arrayReg = "%_" + nextReg();
            emit(arrayReg + " = load i32*, i32** " + bitcastReg);
        }
        else
        {
            String arrayRegPtr = "%" + varName;
            arrayReg = "%_" + nextReg();
            emit(arrayReg + " = load i32*, i32** " + arrayRegPtr);
        }

        emit(arraySizeReg + " = load i32, i32* " + arrayReg);
        emit(icmpReg + " = icmp ult i32 " + indexExprReg + ", " + arraySizeReg);
        emit("br i1 " + icmpReg + ", label %" + oobExit + ", label %" + oobEntry + "\n" + oobEntry + ":");
        emit("call void @throw_oob()");
        emit("br label %" + oobExit + "\n" + oobExit + ":");
        emit(lookupIndexReg + " = add i32 " + indexExprReg + ", 1");
        emit(lookupReg + " = getelementptr i32, i32* " + arrayReg + ", i32 " + lookupIndexReg);
        // emit(lookupReg + " = getelementptr " + llvmType + ", " + llvmType + "*, i32 " + lookupIndexReg);
        emit("store i32 " + exprReg + ", i32* " + lookupReg);

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
        pureEmit("\n");
        String condExprRet = n.f2.accept(this, argu);

        String ifEntry = "if" + nextReg();
        String elseEntry = "if" + nextReg();
        String totalExit = "if" + nextReg(); 

        emit("br i1 " + condExprRet + ", label %" + ifEntry + ", label %" + elseEntry + "\n" + ifEntry + ":\n");

        increaseTabs();
        n.f4.accept(this, argu);

        emit("br label %" + totalExit + "\n" + elseEntry +":\n");
        decreaseTabs();      

        increaseTabs();
        n.f6.accept(this, argu);
        emit("br label %" + totalExit + "\n" + totalExit +":\n");
        decreaseTabs();

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

        String aboveLoop = "loop" + nextReg();
        pureEmit("\n");
        emit("br label %" + aboveLoop + "\n" + aboveLoop + ":");

        String condExprRet = n.f2.accept(this, argu);

        String loopEntry = "loop" + nextReg();
        String loopExit = "loop" + nextReg();

        emit("br i1 " + condExprRet + ", label %" + loopEntry + ", label %" + loopExit + "\n" + loopEntry + ":");

        increaseTabs();
        n.f4.accept(this, argu);
        emit("br label %" + aboveLoop + "\n" + loopExit + ":");
        decreaseTabs();

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
        String exprReg = n.f2.accept(this, argu);

        emit("call void (i32) @print_int(i32 " + exprReg + ")");
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

        String phiReg = "%_" + nextReg();
        String and1 = "and" + nextReg();
        String and2 = "and" + nextReg();
        String and3 = "and" + nextReg();
        String and4 = "and" + nextReg();

        emit("br label %" + and1 + "\n" + and1 + ":");
        emit("br i1 " + clause1 + ", label %" + and2 + ", label %" + and4 + "\n" + and2 + ":");

        String clause2 = n.f2.accept(this, argu);

        emit("br label %" + and3 + "\n" + and3 + ":");
        emit("br label %" + and4 + "\n" + and4 + ":");

        emit(phiReg + " = phi i1 [ 0, %" + and1 + "], [" + clause2 + ", %" + and3 + "]");     

        return phiReg;
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

        emit("%_" + regCount + " = icmp slt i32 " + expr1 + ", " + expr2);

        return "%_" + (regCount++);
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

        emit("%_" + regCount + " = add i32 " + expr1 + ", " + expr2);

        return "%_" + (regCount++);
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

        emit("%_" + regCount + " = sub i32 " + expr1 + ", " + expr2);

        return "%_" + (regCount++);
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

        emit("%_" + regCount + " = mul i32 " + expr1 + ", " + expr2);

        return "%_" + (regCount++);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    @Override
    public String visit(ArrayLookup n, String argu) throws Exception {
        String exprReg = n.f0.accept(this, argu);
        String indexReg = n.f2.accept(this, argu);
        
        String arraySizeReg = "%_" + nextReg();
        String icmpReg = "%_" + nextReg();

        String oobEntry = "oob" + nextReg();
        String oobExit = "oob" + nextReg();

        String offsetReg = "%_" + nextReg();
        String valuePtr = "%_" + nextReg();
        String valueReg = "%_" + nextReg();

        emit(arraySizeReg + " = load i32, i32* " + exprReg);
        emit(icmpReg + " = icmp ult i32 " + indexReg + ", " + arraySizeReg); // Keep unsigned?
        emit("br i1 " + icmpReg + ", label %" + oobExit + ", label %" + oobEntry + "\n" + oobEntry + ":");
        emit("call void @throw_oob()");
        emit("br label %" + oobExit + "\n" + oobExit + ":");
        emit(offsetReg + " = add i32 1, " + indexReg);
        emit(valuePtr + " = getelementptr i32, i32* " + exprReg + ", i32 " + offsetReg);
        emit(valueReg + " = load i32, i32*" + valuePtr);
        
        return valueReg;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    @Override
    public String visit(ArrayLength n, String argu) throws Exception {
        String exprReg = n.f0.accept(this, argu);
        String returnReg = "%_" + nextReg();
        //perhaps bitcast?
        emit(returnReg + " = load i32, i32* " + exprReg);

        return returnReg;
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
        String exprReg = n.f0.accept(this, argu);
        String className;

        if(exprReg.equals("%this")) // if expr is this
            className = argu.split("\\.")[0];
        else // if expr is AllocExpr or Identifier
            className = getMeta();

        
        String methodName = n.f2.accept(this, argu);
        emit("; Calling " + className + "." + methodName);

        List<Argument> methodData = findMethodData(methodName, className);
        Integer methodOffset = classToOffsetMap.get(className).methodOffsets.get(methodName).offset;

        String methodDataString = javaToLlvmType(methodData.get(0).argumentType) + " (i8*";
        String methodCallString = "(i8* " + exprReg;

        if( n.f4.present() )
        {
            argListStack.push(new ArrayList<String>());
            n.f4.accept(this, argu);
            
            List<String> argList = argListStack.peek();
            for(int i = 0; i < argList.size(); i++) {
                String argType = javaToLlvmType(methodData.get(i + 1).argumentType);

                methodDataString += (", " + argType);
                methodCallString += (", " + argType + " " + argList.get(i));
            }
            
            argListStack.pop();
        }

        methodDataString += ")*";
        methodCallString += ")";

        emit("%_" + (regCount++) + " = bitcast i8* " + exprReg + " to i8***");
        emit("%_" + (regCount++) + " = load i8**, i8*** %_" + (regCount - 2));
        emit("%_" + (regCount++) + " = getelementptr i8*, i8** %_" + (regCount - 2) + ", i32 " + (methodOffset/8));
        emit("%_" + (regCount++) + " = load i8*, i8** %_" + (regCount - 2));
        emit("%_" + (regCount++) + " = bitcast i8* %_" + (regCount - 2) + " to " + methodDataString);
        emit("%_" + regCount + " = call " + javaToLlvmType(methodData.get(0).argumentType) + " %_" + (regCount - 1) + methodCallString);

        this.metaVar = methodData.get(0).argumentType;
        return "%_" + (regCount++);
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    @Override
    public String visit(ExpressionList n, String argu) throws Exception {
        List<String> argList = argListStack.peek();
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
        List<String> argList = argListStack.peek();
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
        pureEmit("\n");
        if( n.f0.which == 3 ) // If expression is an identifier (in this case a variable)
        {
            String varName = n.f0.accept(this, argu);
            String varType = findVarType(varName, argu);
            String llvmType = javaToLlvmType(varType);
            this.metaVar = new String(varType);

            String varReg;

            if(!scopeToVars.get(argu).containsKey(varName))
            {
                String elementPtrReg = "%_" + nextReg();
                String bitcastReg = "%_" + nextReg();

                int varOffset = classToOffsetMap.get(argu.split("\\.")[0]).variableOffsets.get(varName).offset + 8;

                emit(elementPtrReg + " = getelementptr i8, i8* %this, i32 " + varOffset);
                emit(bitcastReg + " = bitcast i8* " + elementPtrReg + " to " + llvmType + "*");
                varReg = bitcastReg;
            }
            else
                varReg = "%" + varName;

            String returnReg = "%_" + nextReg();

            emit(returnReg + " = load " + llvmType + ", " + llvmType + "* " + varReg);

            return returnReg;
        }

        

        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    @Override
    public String visit(IntegerLiteral n, String argu) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> "true"
     */
    @Override
    public String visit(TrueLiteral n, String argu) throws Exception {
        return "1"; 
    }

    /**
     * f0 -> "false"
     */
    @Override
    public String visit(FalseLiteral n, String argu) throws Exception {
        return "0";
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
        return "%this";
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
        String exprReg = n.f3.accept(this, argu);

        String sizeReg = "%_" + nextReg();
        String ptrReg = "%_" + nextReg();
        String bitcastReg = "%_" + nextReg();
        String icmpReg = "%_" + nextReg();

        String oobEntry = "arr_alloc" + nextReg();
        String oobExit = "arr_alloc" + nextReg();

        emit(icmpReg + " = icmp slt i32 " + exprReg + ", 0");
        emit("br i1 " + icmpReg + ", label %" + oobEntry + ", label %" + oobExit + "\n" + oobEntry + ":");
        emit("call void @throw_oob()");
        emit("br label %" + oobExit + "\n" + oobExit + ":");
        emit(sizeReg + " = add i32 1, " + exprReg);
        emit(ptrReg + " = call i8* @calloc(i32 4, i32 " + sizeReg +  ")");
        emit(bitcastReg + " = bitcast i8* " + ptrReg + " to i32*");
        emit("store i32 " + exprReg + ", i32* " + bitcastReg);
        
        return bitcastReg;
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

        this.metaVar = new String(className);

        int objectSize = classToOffsetMap.get(className).totalVarOffset + 8;

        emit("%_" + (regCount++) + " = call i8* @calloc(i32 " + objectSize + ", i32 1)");
        emit("%_" + (regCount++) + " = bitcast i8* %_" + (regCount - 2) + " to i8***");

        int vtableSize = classToOffsetMap.get(className).methodOffsets.size();
        emit("%_" + (regCount++) + " = getelementptr [" + vtableSize  + " x i8*], [" + vtableSize + " x i8*]* @." + className +"_vtable, i32 0, i32 0");
        emit("store i8** %_" + (regCount - 1)  + ", i8*** %_" + (regCount - 2));

        

        return "%_" + (regCount - 3);
        //return "%" + className;
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    @Override
    public String visit(NotExpression n, String argu) throws Exception {
        String exprReg = n.f1.accept(this, argu);

        emit("%_" + regCount + " = xor i1 1, " + exprReg);

        return "%_" + (regCount++);
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