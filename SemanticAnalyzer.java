import absyn.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SemanticAnalyzer implements AbsynVisitor {
    public static final int ROOT_LEVEL = 0;
    public Map<String, List<NodeType>> symbolTable;

    private final int SPACES = 4;

    public SemanticAnalyzer() {
        initSymbolTable();
    }

    public void analyze(DecList program) {
        System.out.println("Entering the global scope:");
        program.accept(this, ROOT_LEVEL + 1);
        removeDecListSymbols(program, ROOT_LEVEL + 1);
        System.out.println("Leaving the global scope");
    }

    private void indent(int level) {
        for (int i = 0; i < level * SPACES; i++) {
            System.out.print(" ");
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
                // TODO: report error
                System.err.println("ERROR " + conflict.def.getName());
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
            return false;
        } else {
            System.err.println("ERROR");
            return false;
        }
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
            System.out.print(varDec.getName() + ": " + varDec.type.getTypeString().toLowerCase());
            if (dec instanceof ArrayDec) {
                System.out.print("[]");
            }
            System.out.println();
        } else if (dec instanceof FunctionDec) {
            displayFunctionDec((FunctionDec) dec);
        }
    }

    private void displayFunctionDec(FunctionDec dec) {
        System.out.print(dec.func + ": (");

        int numParams = 0;
        VarDecList params = dec.params;
        if (params != null && params.head != null) {
            while (params != null && params.head != null) {
                if (numParams > 0) {
                    System.out.print(", ");
                }
                System.out.print(params.head.type.getTypeString().toLowerCase());
                params = params.tail;
                numParams++;
            }
        } else {
            System.out.print("void");
        }

        System.out.println(") -> " + dec.result.getTypeString().toLowerCase());
    }

    public void visit(NameTy type, int level) {
        // TODO:
        System.out.println("visiting NameTy");
    }

    public void visit(IndexVar var, int level) {
        var.index.accept(this, level);
        indent(level);
        System.out.println("visiting IndexVar");
        if (!isDeclared(var, level)) {
            // TODO: show error
            System.err.println("ERROR (not declared)");
        }
    }

    public void visit(SimpleVar var, int level) {
        // TODO:
        indent(level);
        System.out.println("visiting SimpleVar");
        if (!isDeclared(var, level)) {
            // TODO: show error
            System.err.println("ERROR (not declared)");
        }
    }

    public void visit(AssignExp exp, int level) {
        exp.lhs.accept(this, level);
        exp.rhs.accept(this, level);
        indent(level);
        System.out.println("visiting AssignExp");
    }

    public void visit(BoolExp exp, int level) {
        // TODO:
        indent(level);
        System.out.println("visiting BoolExp");
    }

    public void visit(CallExp exp, int level) {
        exp.args.accept(this, level);
        indent(level);
        System.out.println("visiting CallExp");
    }

    public void visit(CompoundExp exp, int level) {
        level++;
        exp.decs.accept(this, level);
        exp.exps.accept(this, level);
        removeVarDecListSymbols(exp.decs, level);
    }

    public void visit(IfExp exp, int level) {
        exp.thenpart.accept(this, level + 1);
        exp.elsepart.accept(this, level + 1);
        indent(level);
        System.out.println("visiting IfExp");
    }

    public void visit(IntExp exp, int level) {
        // TODO:
        indent(level);
        System.out.println("visiting IntExp");
    }

    public void visit(NilExp exp, int level) {
        // TODO:
        indent(level);
        System.out.println("visiting NilExp");
    }

    public void visit(OpExp exp, int level) {
        exp.left.accept(this, level);
        exp.right.accept(this, level);
        indent(level);
        System.out.println("visiting OpExp");
    }

    public void visit(ReturnExp exp, int level) {
        exp.exp.accept(this, level);
        indent(level);
        System.out.println("visiting ReturnExp");
    }

    public void visit(VarExp exp, int level) {
        exp.var.accept(this, level);
        indent(level);
        System.out.println("visiting VarExp");
    }

    public void visit(WhileExp exp, int level) {
        exp.test.accept(this, level);
        exp.body.accept(this, level);
        indent(level);
        System.out.println("visiting WhileExp");
    }

    public void visit(ExpList expList, int level) {
        while (expList != null && expList.head != null) {
            expList.head.accept(this, level);
            expList = expList.tail;
        }
        indent(level);
        System.out.println("visiting ExpList");
    }

    public void visit(DecList decList, int level) {
        while (decList != null && decList.head != null) {
            decList.head.accept(this, level);
            decList = decList.tail;
        }
        indent(level);
        System.out.println("visiting DecList");
    }

    public void visit(VarDecList varDecList, int level) {
        while (varDecList != null && varDecList.head != null) {
            varDecList.head.accept(this, level);
            varDecList = varDecList.tail;
        }
        indent(level);
        System.out.println("visiting VarDecList " + level);
    }

    public void visit(FunctionDec dec, int level) {
        indent(level);
        System.out.println("Entering the scope for function " + dec.getName() + ":");
        dec.params.accept(this, level + 1);
        dec.body.accept(this, level);

        removeVarDecListSymbols(dec.params, level + 1);
        addSymbol(dec, level);

        indent(level);
        System.out.println("Leaving the function scope");
    }

    public void visit(ArrayDec varDec, int level) {
        addSymbol(varDec, level);
    }

    public void visit(SimpleDec varDec, int level) {
        addSymbol(varDec, level);
    }
}
