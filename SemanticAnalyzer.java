import absyn.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SemanticAnalyzer implements AbsynVisitor {
    public static final int ROOT_LEVEL = 0;
    public Map<String, List<NodeType>> symbolTable;

    private StringBuilder output;

    private final int SPACES = 4;
    private final String MAIN_FUNCTION_NAME = "main";

    public SemanticAnalyzer() {
        initSymbolTable();
    }

    public void analyze(DecList program, boolean showTable) {
        output = new StringBuilder();
        output.append("Entering the global scope:\n");
        program.accept(this, ROOT_LEVEL + 1);
        removeDecListSymbols(program, ROOT_LEVEL + 1);
        output.append("Leaving the global scope\n");
        if (!isMainDeclared()) {
            reportError("Main function is not declared");
        }

        // Display the final symbol table
        if (showTable) {
            System.out.print(output.toString());
        }
    }

    private void indent(int level) {
        for (int i = 0; i < level * SPACES; i++) {
            output.append(" ");
        }
    }

    private void initSymbolTable() {
        symbolTable = new HashMap<String, List<NodeType>>();

        final FunctionDec INPUT_FUNC = new FunctionDec(
                -1, -1, new NameTy(-1, -1, NameTy.INT),
                "input", null, new NilExp(-1, -1));
        final FunctionDec OUTPUT_FUNC = new FunctionDec(
                -1, -1, new NameTy(-1, -1, NameTy.VOID),
                "output", new VarDecList(
                        new SimpleDec(
                                -1, -1, new NameTy(-1, -1, NameTy.INT), "x"),
                        null),
                null);
        addSymbol(INPUT_FUNC, ROOT_LEVEL);
        addSymbol(OUTPUT_FUNC, ROOT_LEVEL);
    }

    private void reportError(int row, int col, String msg) {
        System.err.println("Error on line " + (row + 1) + ", column " + (col + 1) + ": " + msg);
    }

    private void reportError(String msg) {
        System.err.println("Error: " + msg);
    }

    private boolean isMainDeclared() {
        return symbolTable.containsKey(MAIN_FUNCTION_NAME);
    }

    private NodeType checkDecConflict(List<NodeType> decStack, Dec dec) {
        for (NodeType type : decStack) {
            if (type.name.equals(dec.getName())) {
                if ((type.def instanceof VarDec && dec instanceof VarDec) ||
                        (type.def instanceof FunctionDec && dec instanceof FunctionDec)) {
                    return type;
                }
            }
        }
        return null;
    }

    private void addSymbol(Dec dec, int level) {
        final String name = dec.getName();
        final NodeType type = new NodeType(name, dec, level);
        List<NodeType> decStack = null;
        NodeType conflict = null;

        if (symbolTable.containsKey(name)) {
            decStack = symbolTable.get(name);
            if ((conflict = checkDecConflict(decStack, dec)) != null) {
                Dec conflictDec = conflict.def;
                if (conflictDec instanceof FunctionDec && ((FunctionDec) conflictDec).body instanceof NilExp
                        && !(((FunctionDec) dec).body instanceof NilExp)) {
                    return;
                }
                String idType = dec instanceof FunctionDec ? "Function" : "Variable";
                reportError(dec.row, dec.col, idType + " \"" + dec.getName() + "\" is already declared");
            } else {
                decStack.add(0, type);
            }
        } else {
            decStack = new ArrayList<NodeType>();
            decStack.add(type);
            symbolTable.put(name, decStack);
        }
    }

    private boolean removeSymbol(Dec dec) {
        final String name = dec.getName();
        List<NodeType> decStack = null;

        if (symbolTable.containsKey(name)) {
            decStack = symbolTable.get(name);
            if (decStack.size() > 0) {
                decStack.remove(0);
                return true;
            }
        }
        return false;
    }

    private Dec getDecType(String decName) {
        List<NodeType> decStack = symbolTable.getOrDefault(decName, null);

        if (decStack != null && decStack.size() > 0) {
            return decStack.get(0).def;
        }

        return null;
    }

    private boolean isType(Dec dec, int type) {
        if (dec != null && dec instanceof VarDec) {
            return ((VarDec) dec).type.type == type;
        }
        return false;
    }

    private boolean isInt(Dec dec) {
        return isType(dec, NameTy.INT);
    }

    private boolean isBool(Dec dec) {
        return isType(dec, NameTy.BOOL);
    }

    private Dec getDummyDec(int type) {
        return new SimpleDec(-1, -1, new NameTy(-1, -1, type), null);
    }

    private boolean isDeclared(Var var, int level) {
        List<NodeType> decStack = symbolTable.getOrDefault(var.name, null);

        if (decStack != null) {
            for (NodeType node : decStack) {
                if (node.name.equals(var.name) && node.level <= level) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isDeclared(CallExp exp, int level) {
        List<NodeType> decStack = symbolTable.getOrDefault(exp.func, null);

        if (decStack != null) {
            for (NodeType node : decStack) {
                if (node.name.equals(exp.func) && node.level <= level) {
                    return true;
                }
            }
        }

        return false;
    }

    private void removeVarDecListSymbols(VarDecList varDecs, int level) {
        VarDecList dec = varDecs;
        while (dec != null && dec.head != null) {
            if (removeSymbol(dec.head)) {
                indent(level);
                displayDec(dec.head);
            }
            dec = dec.tail;
        }
    }

    private void removeDecListSymbols(DecList decs, int level) {
        DecList dec = decs;
        while (dec != null && dec.head != null) {
            if (removeSymbol(dec.head)) {
                indent(level);
                displayDec(dec.head);
            }
            dec = dec.tail;
        }
    }

    private void displayDec(Dec dec) {
        if (dec instanceof VarDec) {
            VarDec varDec = (VarDec) dec;
            output.append(varDec.getName() + ": " + varDec.type.getTypeString().toLowerCase());
            if (dec instanceof ArrayDec) {
                output.append("[]");
            }
            output.append("\n");
        } else if (dec instanceof FunctionDec) {
            displayFunctionDec((FunctionDec) dec);
        }
    }

    private void displayFunctionDec(FunctionDec dec) {
        output.append(dec.func + ": (");

        int numParams = 0;
        VarDecList params = dec.params;
        if (params != null && params.head != null) {
            while (params != null && params.head != null) {
                if (numParams > 0) {
                    output.append(", ");
                }
                output.append(params.head.type.getTypeString().toLowerCase());
                params = params.tail;
                numParams++;
            }
        } else {
            output.append("void");
        }

        output.append(") -> " + dec.result.getTypeString().toLowerCase() + "\n");
    }

    private boolean arrContains(int[] arr, int obj) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == obj) {
                return true;
            }
        }
        return false;
    }

    private int getOperatorInputType(int operator) {
        final int[] bothOperators = { OpExp.EQUAL, OpExp.NEQUAL };
        final int[] intOperators = { OpExp.ADD, OpExp.SUBT, OpExp.MULT, OpExp.DIV, OpExp.UMINUS, OpExp.LT, OpExp.GT,
                OpExp.LTE, OpExp.GTE };
        final int[] boolOperators = { OpExp.OR, OpExp.AND, OpExp.BNOT };

        if (arrContains(bothOperators, operator)) {
            return 3;
        } else if (arrContains(intOperators, operator)) {
            return NameTy.INT;
        } else if (arrContains(boolOperators, operator)) {
            return NameTy.BOOL;
        }

        return -1;
    }

    private int getOperatorOutputType(int operator) {
        final int[] intOperators = { OpExp.ADD, OpExp.SUBT, OpExp.MULT, OpExp.MULT, OpExp.UMINUS };
        final int[] boolOperators = { OpExp.LT, OpExp.GT, OpExp.LTE, OpExp.GTE, OpExp.EQUAL, OpExp.NEQUAL, OpExp.OR,
                OpExp.AND, OpExp.BNOT };

        if (arrContains(intOperators, operator)) {
            return NameTy.INT;
        } else if (arrContains(boolOperators, operator)) {
            return NameTy.BOOL;
        }

        return -1;
    }

    public void visit(NameTy type, int level) {
    }

    public void visit(IndexVar var, int level) {
        var.index.accept(this, level);
    }

    public void visit(SimpleVar var, int level) {
    }

    public void visit(AssignExp exp, int level) {
        exp.lhs.accept(this, level);
        exp.rhs.accept(this, level);
        exp.dtype = exp.lhs.dtype;
        if (exp.lhs.dtype.getType().type != exp.rhs.dtype.getType().type) {
            reportError(exp.row, exp.col, "Types do not match");
        }
    }

    public void visit(BoolExp exp, int level) {
        exp.dtype = getDummyDec(NameTy.BOOL);
    }

    public void visit(CallExp exp, int level) {
        exp.args.accept(this, level);
        if (!isDeclared(exp, level)) {
            reportError(exp.row, exp.col, "Function \"" + exp.func + "\" called before declaration");
        }
        exp.dtype = getDecType(exp.func);
    }

    public void visit(CompoundExp exp, int level) {
        // Only show for non-function blocks
        if (level != 1) {
            indent(level);
            output.append("Entering block:\n");
        }

        exp.decs.accept(this, level + 1);
        exp.exps.accept(this, level + 1);
        removeVarDecListSymbols(exp.decs, level + 1);

        // Only show for non-function blocks
        if (level != 1) {
            indent(level);
            output.append("Leaving block\n");
        }
    }

    public void visit(IfExp exp, int level) {
        exp.thenpart.accept(this, level + 1);
        exp.elsepart.accept(this, level + 1);
        if (!isBool(exp.test.dtype)) {
            reportError(exp.test.row, exp.test.col, "Expression type must be boolean");
        }
    }

    public void visit(IntExp exp, int level) {
        exp.dtype = getDummyDec(NameTy.INT);
    }

    public void visit(NilExp exp, int level) {
    }

    public void visit(OpExp exp, int level) {
        exp.left.accept(this, level);
        exp.right.accept(this, level);
        int inputType = getOperatorInputType(exp.op);
        int outputType = getOperatorOutputType(exp.op);
        exp.dtype = getDummyDec(outputType);

        // Special case: operators that accept multiple types
        if (inputType == 3) {
            // Left and right param types must match
            if (exp.left.dtype.getType().type != exp.right.dtype.getType().type) {
                reportError(exp.row, exp.col, "Invalid expression type for operator");
            }
        } else {
            // Left and right param types must match the operator type (if they exist)

            if (!(exp.left instanceof NilExp) && exp.left.dtype.getType().type != inputType) {
                System.out.println(inputType + " " + exp.left.dtype.getType().type);
                reportError(exp.left.row, exp.left.col, "Invalid expression type for operator");
            }

            if (!(exp.right instanceof NilExp) && exp.right.dtype.getType().type != inputType) {
                System.out.println(inputType + " " + exp.right.dtype.getType().type);
                reportError(exp.right.row, exp.right.col, "Invalid expression type for operator");
            }
        }
    }

    public void visit(ReturnExp exp, int level) {
        exp.exp.accept(this, level);
        exp.dtype = exp.exp.dtype;
    }

    public void visit(VarExp exp, int level) {
        exp.var.accept(this, level);
        exp.dtype = getDecType(exp.var.name);
        if (!isDeclared(exp.var, level)) {
            reportError(exp.var.row, exp.var.col, "Variable \"" + exp.var.name + "\" used before declaration");
        }
    }

    public void visit(WhileExp exp, int level) {
        exp.test.accept(this, level);
        exp.body.accept(this, level);
        if (!isBool(exp.test.dtype)) {
            reportError(exp.test.row, exp.test.col, "Expression type must be boolean");
        }
    }

    public void visit(ExpList expList, int level) {
        while (expList != null && expList.head != null) {
            expList.head.accept(this, level);
            expList = expList.tail;
        }
    }

    public void visit(DecList decList, int level) {
        while (decList != null && decList.head != null) {
            decList.head.accept(this, level);
            decList = decList.tail;
        }
    }

    public void visit(VarDecList varDecList, int level) {
        while (varDecList != null && varDecList.head != null) {
            varDecList.head.accept(this, level);
            varDecList = varDecList.tail;
        }
    }

    public void visit(FunctionDec dec, int level) {
        if (!(dec.body instanceof NilExp)) {
            indent(level);
            output.append("Entering the scope for function " + dec.getName() + ":\n");
        }
        dec.params.accept(this, level + 1);
        dec.body.accept(this, level);

        // Validate return type
        if (dec.body instanceof CompoundExp) {
            CompoundExp body = (CompoundExp) dec.body;
            ExpList exp = body.exps;
            while (exp != null && exp.head != null) {
                if (exp.head instanceof ReturnExp) {
                    ReturnExp rexp = (ReturnExp) exp.head;
                    if (rexp.dtype.getType().type != dec.result.type) {
                        reportError(rexp.exp.row, rexp.exp.col,
                                "Return expression does not match function return type");
                    }
                    break;
                }
                exp = exp.tail;
            }
        }

        removeVarDecListSymbols(dec.params, level + 1);
        addSymbol(dec, level);

        if (!(dec.body instanceof NilExp)) {
            indent(level);
            output.append("Leaving the function scope\n");
        }
    }

    public void visit(ArrayDec varDec, int level) {
        if (varDec.type.type == NameTy.VOID) {
            reportError(varDec.row, varDec.col, "The type \"void\" is not allowed for variables");
        } else {
            addSymbol(varDec, level);
        }
    }

    public void visit(SimpleDec varDec, int level) {
        if (varDec.type.type == NameTy.VOID) {
            reportError(varDec.row, varDec.col, "The type \"void\" is not allowed for variables");
        } else {
            addSymbol(varDec, level);
        }
    }
}
