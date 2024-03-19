import absyn.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ShowTreeVisitor implements AbsynVisitor {
    static final int SPACES = 4;

    private StringBuilder output = null;

    public void showTree(Absyn tree, String outputFile) {
        output = new StringBuilder();
        tree.accept(this, 0);
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

    public void visit(NameTy type, int level) {
        indent(level);
        appendOutputLine("NameTy: " + type.getTypeString());
    }

    public void visit(IndexVar var, int level) {
        indent(level);
        appendOutputLine("IndexVar: " + var.name);
        var.index.accept(this, ++level);
    }

    public void visit(SimpleVar var, int level) {
        indent(level);
        appendOutputLine("SimpleVar: " + var.name);
    }

    public void visit(AssignExp exp, int level) {
        indent(level);
        appendOutputLine("AssignExp:");
        level++;
        exp.lhs.accept(this, level);
        exp.rhs.accept(this, level);
    }

    public void visit(BoolExp exp, int level) {
        indent(level);
        appendOutputLine("BoolExp: " + exp.value);
    }

    public void visit(CallExp exp, int level) {
        indent(level);
        appendOutputLine("CallExp: " + exp.func);
        exp.args.accept(this, ++level);
    }

    public void visit(CompoundExp exp, int level) {
        indent(level);
        appendOutputLine("CompoundExp:");
        level++;
        if (exp.decs != null) {
            exp.decs.accept(this, level);
        }
        if (exp.exps != null) {
            exp.exps.accept(this, level);
        }
    }

    public void visit(IfExp exp, int level) {
        indent(level);
        appendOutputLine("IfExp:");
        level++;
        exp.test.accept(this, level);
        exp.thenpart.accept(this, level);
        if (exp.elsepart != null) {
            exp.elsepart.accept(this, level);
        }
    }

    public void visit(IntExp exp, int level) {
        indent(level);
        appendOutputLine("IntExp: " + exp.value);
    }

    public void visit(NilExp exp, int level) {
        indent(level);
        appendOutputLine("NilExp");
    }

    public void visit(OpExp exp, int level) {
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
            exp.left.accept(this, level);
        }
        exp.right.accept(this, level);
    }

    public void visit(ReturnExp exp, int level) {
        indent(level);
        appendOutputLine("ReturnExp:");
        if (exp.exp != null) {
            exp.exp.accept(this, ++level);
        }
    }

    public void visit(VarExp exp, int level) {
        indent(level);
        appendOutputLine("VarExp:");
        exp.var.accept(this, ++level);
    }

    public void visit(WhileExp exp, int level) {
        indent(level);
        appendOutputLine("WhileExp:");
        level++;
        exp.test.accept(this, level);
        exp.body.accept(this, level);
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
        indent(level);
        appendOutputLine("FunctionDec: " + dec.func);
        level++;
        dec.result.accept(this, level);
        dec.params.accept(this, level);
        if (dec.body != null) {
            dec.body.accept(this, level);
        }
    }

    public void visit(ArrayDec varDec, int level) {
        indent(level);
        appendOutputLine("ArrayDec: " + varDec.name +
                " (size=" + varDec.size + ")");
        varDec.type.accept(this, ++level);
    }

    public void visit(SimpleDec varDec, int level) {
        indent(level);
        appendOutputLine("SimpleDec: " + varDec.name);
        varDec.type.accept(this, ++level);
    }
}
