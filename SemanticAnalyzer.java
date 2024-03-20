/*
  Created by: Ethan Rowan
  File Name: SemanticAnalyzer.java
*/

import absyn.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

    public void analyze(DecList program, String outputFile) {
        output = new StringBuilder();
        output.append("Entering the global scope:\n");
        program.accept(this, ROOT_LEVEL + 1, false);
        removeDecListSymbols(program, ROOT_LEVEL + 1);
        output.append("Leaving the global scope\n");
        if (!isMainDeclared()) {
            reportError("Main function is not declared");
        }

        // Display the final symbol table
        if (outputFile != null && writeFile(outputFile)) {
            System.out.println("Symbol table written to \"" + outputFile + "\".");
        }
    }

    private boolean writeFile(String outputFile) {
        try {
            FileOutputStream outputStream = new FileOutputStream(new File(outputFile));
            outputStream.write(output.toString().getBytes());
            outputStream.close();
            return true;
        } catch (IOException e) {
            System.err.println("Failed to write symbol table to \"" + outputFile + "\".");
            return false;
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

    private NodeType checkDecConflict(List<NodeType> decStack, Dec dec, int level) {
        for (NodeType type : decStack) {
            if (type.name.equals(dec.getName()) && level == type.level) {
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
            if ((conflict = checkDecConflict(decStack, dec, level)) != null) {
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

    private int getType(Exp exp) {
        if (exp.dtype != null && exp.dtype.getType() != null) {
            return exp.dtype.getType().type;
        }
        return -1;
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
        final int[] intOperators = { OpExp.ADD, OpExp.SUBT, OpExp.MULT, OpExp.DIV, OpExp.UMINUS };
        final int[] boolOperators = { OpExp.LT, OpExp.GT, OpExp.LTE, OpExp.GTE, OpExp.EQUAL, OpExp.NEQUAL, OpExp.OR,
                OpExp.AND, OpExp.BNOT };

        if (arrContains(intOperators, operator)) {
            return NameTy.INT;
        } else if (arrContains(boolOperators, operator)) {
            return NameTy.BOOL;
        }

        return -1;
    }

    public void visit(NameTy type, int level, boolean isAddress) {
    }

    public void visit(IndexVar var, int level, boolean isAddress) {
        var.index.accept(this, level, false);
        if (!isInt(var.index.dtype)) {
            reportError(var.index.row, var.index.col, "Expression type must be integer");
        }
    }

    public void visit(SimpleVar var, int level, boolean isAddress) {
    }

    public void visit(AssignExp exp, int level, boolean isAddress) {
        exp.lhs.accept(this, level, false);
        exp.rhs.accept(this, level, false);
        exp.dtype = exp.lhs.dtype;
        if (getType(exp.lhs) != getType(exp.rhs)) {
            reportError(exp.row, exp.col, "Types do not match");
        }
    }

    public void visit(BoolExp exp, int level, boolean isAddress) {
        exp.dtype = getDummyDec(NameTy.BOOL);
    }

    public void visit(CallExp exp, int level, boolean isAddress) {
        exp.args.accept(this, level, false);
        if (!isDeclared(exp, level)) {
            reportError(exp.row, exp.col, "Function \"" + exp.func + "\" called before declaration");
        }
        exp.dtype = getDecType(exp.func);

        // Validate argument types match the function declaration
        if (exp.dtype != null) {
            ExpList expList = exp.args;
            VarDecList params = ((FunctionDec) exp.dtype).params;

            // Check if the function declaration or argument list have 1 mismatching
            // parameter
            Exp arg1 = expList != null ? expList.head : null;
            VarDec param1 = params != null ? params.head : null;
            if ((arg1 != null && param1 == null) || (arg1 == null && param1 != null)) {
                reportError(exp.row, exp.col, "Arguments do not match function declaration");
            }

            // Check if the argument list and function parameters have the same types
            while (expList != null && expList.head != null && params != null && params.head != null) {
                if (getType(expList.head) != params.head.type.type || (params.tail != null && expList.tail == null)
                        || (params.tail == null && expList.tail != null)) {
                    reportError(expList.head.row, expList.head.col, "Arguments do not match function declaration");
                    break;
                }
                params = params.tail;
                expList = expList.tail;
            }
        }
    }

    public void visit(CompoundExp exp, int level, boolean isAddress) {
        // Only show for non-function blocks
        if (level != 1) {
            indent(level);
            output.append("Entering block:\n");
        }

        exp.decs.accept(this, level + 1, false);
        exp.exps.accept(this, level + 1, false);
        removeVarDecListSymbols(exp.decs, level + 1);

        // Only show for non-function blocks
        if (level != 1) {
            indent(level);
            output.append("Leaving block\n");
        }
    }

    public void visit(IfExp exp, int level, boolean isAddress) {
        exp.test.accept(this, level, false);
        exp.thenpart.accept(this, level, false);
        exp.elsepart.accept(this, level, false);
        if (!isBool(exp.test.dtype)) {
            reportError(exp.test.row, exp.test.col, "Expression type must be boolean");
        }
    }

    public void visit(IntExp exp, int level, boolean isAddress) {
        exp.dtype = getDummyDec(NameTy.INT);
    }

    public void visit(NilExp exp, int level, boolean isAddress) {
    }

    public void visit(OpExp exp, int level, boolean isAddress) {
        exp.left.accept(this, level, false);
        exp.right.accept(this, level, false);
        int inputType = getOperatorInputType(exp.op);
        int outputType = getOperatorOutputType(exp.op);
        exp.dtype = getDummyDec(outputType);

        // Special case: operators that accept multiple types
        if (inputType == 3) {
            // Left and right param types must match
            if (getType(exp.left) != getType(exp.right)) {
                reportError(exp.row, exp.col, "Invalid expression type for operator");
            }
        } else {
            // Left and right param types must match the operator type (if they exist)

            if (!(exp.left instanceof NilExp) && getType(exp.left) != inputType) {
                reportError(exp.left.row, exp.left.col, "Invalid expression type for operator");
            }

            if (!(exp.right instanceof NilExp) && getType(exp.right) != inputType) {
                reportError(exp.right.row, exp.right.col, "Invalid expression type for operator");
            }
        }
    }

    public void visit(ReturnExp exp, int level, boolean isAddress) {
        exp.exp.accept(this, level, false);
        exp.dtype = exp.exp.dtype;
    }

    public void visit(VarExp exp, int level, boolean isAddress) {
        exp.var.accept(this, level, false);
        exp.dtype = getDecType(exp.var.name);
        if (!isDeclared(exp.var, level)) {
            reportError(exp.var.row, exp.var.col, "Variable \"" + exp.var.name + "\" used before declaration");
        }
    }

    public void visit(WhileExp exp, int level, boolean isAddress) {
        exp.test.accept(this, level, false);
        exp.body.accept(this, level, false);
        if (!isBool(exp.test.dtype)) {
            reportError(exp.test.row, exp.test.col, "Expression type must be boolean");
        }
    }

    public void visit(ExpList expList, int level, boolean isAddress) {
        while (expList != null && expList.head != null) {
            expList.head.accept(this, level, false);
            expList = expList.tail;
        }
    }

    public void visit(DecList decList, int level, boolean isAddress) {
        while (decList != null && decList.head != null) {
            decList.head.accept(this, level, false);
            decList = decList.tail;
        }
    }

    public void visit(VarDecList varDecList, int level, boolean isAddress) {
        while (varDecList != null && varDecList.head != null) {
            varDecList.head.accept(this, level, false);
            varDecList = varDecList.tail;
        }
    }

    public void visit(FunctionDec dec, int level, boolean isAddress) {
        boolean isPrototype = dec.body instanceof NilExp;

        addSymbol(dec, level);

        if (!isPrototype) {
            indent(level);
            output.append("Entering the scope for function " + dec.getName() + ":\n");

            dec.params.accept(this, level + 1, false);
            dec.body.accept(this, level, false);

            // Validate return type
            if (dec.body instanceof CompoundExp) {
                CompoundExp body = (CompoundExp) dec.body;
                ExpList exp = body.exps;
                while (exp != null && exp.head != null) {
                    if (exp.head instanceof ReturnExp) {
                        ReturnExp rexp = (ReturnExp) exp.head;
                        if (getType(rexp) != dec.result.type) {
                            reportError(rexp.exp.row, rexp.exp.col,
                                    "Return expression does not match function return type");
                        }
                    }
                    exp = exp.tail;
                }
            }
            removeVarDecListSymbols(dec.params, level + 1);
        }

        if (!isPrototype) {
            indent(level);
            output.append("Leaving the function scope\n");
        }
    }

    public void visit(ArrayDec varDec, int level, boolean isAddress) {
        if (varDec.type.type == NameTy.VOID) {
            reportError(varDec.row, varDec.col, "The type \"void\" is not allowed for variables");
        } else {
            addSymbol(varDec, level);
        }
    }

    public void visit(SimpleDec varDec, int level, boolean isAddress) {
        if (varDec.type.type == NameTy.VOID) {
            reportError(varDec.row, varDec.col, "The type \"void\" is not allowed for variables");
        } else {
            addSymbol(varDec, level);
        }
    }
}
