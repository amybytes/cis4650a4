import absyn.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ShowTreeVisitor implements AbsynVisitor {
    static final int SPACES = 4;

    private StringBuilder output = null;

    public void showTree(Absyn tree, String outputFile) {
        output = new StringBuilder();
        tree.accept(this, 0, false);
        if (writeFile(outputFile)) {
            System.out.println("Syntax tree written to \"" + outputFile + "\".");
        }
    }

    private boolean writeFile(String outputFile) {
        try {
            FileOutputStream outputStream = new FileOutputStream(new File(outputFile));
            outputStream.write(output.toString().getBytes());
            outputStream.close();
            return true;
        } catch (IOException e) {
            System.err.println("Failed to write syntax tree to \"" + outputFile + "\".");
            return false;
        }
    }

    private void appendOutput(String s) {
        output.append(s);
    }

    private void appendOutputLine(String s) {
        output.append(s + "\n");
    }

    private void indent(int level) {
        for (int i = 0; i < level * SPACES; i++) {
            appendOutput(" ");
        }
    }

    public void visit(NameTy type, int level, boolean isAddress) {
        indent(level);
        appendOutputLine("NameTy: " + type.getTypeString());
    }

    public void visit(IndexVar var, int level, boolean isAddress) {
        indent(level);
        appendOutputLine("IndexVar: " + var.name);
        var.index.accept(this, ++level, false);
    }

    public void visit(SimpleVar var, int level, boolean isAddress) {
        indent(level);
        appendOutputLine("SimpleVar: " + var.name);
    }

    public void visit(AssignExp exp, int level, boolean isAddress) {
        indent(level);
        appendOutputLine("AssignExp:");
        level++;
        exp.lhs.accept(this, level, false);
        exp.rhs.accept(this, level, false);
    }

    public void visit(BoolExp exp, int level, boolean isAddress) {
        indent(level);
        appendOutputLine("BoolExp: " + exp.value);
    }

    public void visit(CallExp exp, int level, boolean isAddress) {
        indent(level);
        appendOutputLine("CallExp: " + exp.func);
        exp.args.accept(this, ++level, false);
    }

    public void visit(CompoundExp exp, int level, boolean isAddress) {
        indent(level);
        appendOutputLine("CompoundExp:");
        level++;
        if (exp.decs != null) {
            exp.decs.accept(this, level, false);
        }
        if (exp.exps != null) {
            exp.exps.accept(this, level, false);
        }
    }

    public void visit(IfExp exp, int level, boolean isAddress) {
        indent(level);
        appendOutputLine("IfExp:");
        level++;
        exp.test.accept(this, level, false);
        exp.thenpart.accept(this, level, false);
        if (exp.elsepart != null) {
            exp.elsepart.accept(this, level, false);
        }
    }

    public void visit(IntExp exp, int level, boolean isAddress) {
        indent(level);
        appendOutputLine("IntExp: " + exp.value);
    }

    public void visit(NilExp exp, int level, boolean isAddress) {
        indent(level);
        appendOutputLine("NilExp");
    }

    public void visit(OpExp exp, int level, boolean isAddress) {
        indent(level);
        appendOutput("OpExp:");
        switch (exp.op) {
            case OpExp.LT:
                appendOutputLine(" < ");
                break;
            case OpExp.GT:
                appendOutputLine(" > ");
                break;
            case OpExp.LTE:
                appendOutputLine(" <= ");
                break;
            case OpExp.GTE:
                appendOutputLine(" >= ");
                break;
            case OpExp.EQUAL:
                appendOutputLine(" == ");
                break;
            case OpExp.NEQUAL:
                appendOutputLine(" != ");
                break;
            case OpExp.OR:
                appendOutputLine(" || ");
                break;
            case OpExp.AND:
                appendOutputLine(" && ");
                break;
            case OpExp.BNOT:
                appendOutputLine(" ~ ");
                break;
            case OpExp.ADD:
                appendOutputLine(" + ");
                break;
            case OpExp.SUBT:
                appendOutputLine(" - ");
                break;
            case OpExp.MULT:
                appendOutputLine(" * ");
                break;
            case OpExp.DIV:
                appendOutputLine(" / ");
                break;
            case OpExp.UMINUS:
                appendOutputLine(" - ");
                break;
            default:
                appendOutputLine("Unrecognized operator at line " + exp.row + " and column " + exp.col);
        }
        level++;
        if (exp.left != null) {
            exp.left.accept(this, level, false);
        }
        exp.right.accept(this, level, false);
    }

    public void visit(ReturnExp exp, int level, boolean isAddress) {
        indent(level);
        appendOutputLine("ReturnExp:");
        if (exp.exp != null) {
            exp.exp.accept(this, ++level, false);
        }
    }

    public void visit(VarExp exp, int level, boolean isAddress) {
        indent(level);
        appendOutputLine("VarExp:");
        exp.var.accept(this, ++level, false);
    }

    public void visit(WhileExp exp, int level, boolean isAddress) {
        indent(level);
        appendOutputLine("WhileExp:");
        level++;
        exp.test.accept(this, level, false);
        exp.body.accept(this, level, false);
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
        indent(level);
        appendOutputLine("FunctionDec: " + dec.func);
        level++;
        dec.result.accept(this, level, false);
        dec.params.accept(this, level, false);
        if (dec.body != null) {
            dec.body.accept(this, level, false);
        }
    }

    public void visit(ArrayDec varDec, int level, boolean isAddress) {
        indent(level);
        appendOutputLine("ArrayDec: " + varDec.name +
                " (size=" + varDec.size + ")");
        varDec.type.accept(this, ++level, false);
    }

    public void visit(SimpleDec varDec, int level, boolean isAddress) {
        indent(level);
        appendOutputLine("SimpleDec: " + varDec.name);
        varDec.type.accept(this, ++level, false);
    }
}
