import absyn.*;

public class ShowTreeVisitor implements AbsynVisitor {
    static final int SPACES = 4;

    private void indent(int level) {
        for (int i = 0; i < level * SPACES; i++) {
            System.out.print(" ");
        }
    }

    public void visit(NameTy type, int level) {
        indent(level);
        System.out.println("NameTy: " + type.getTypeString());
    }

    public void visit(IndexVar var, int level) {
        indent(level);
        System.out.println("IndexVar: " + var.name);
        var.index.accept(this, ++level);
    }

    public void visit(SimpleVar var, int level) {
        indent(level);
        System.out.println("SimpleVar: " + var.name);
    }

    public void visit(AssignExp exp, int level) {
        indent(level);
        System.out.println("AssignExp:");
        level++;
        exp.lhs.accept(this, level);
        exp.rhs.accept(this, level);
    }

    public void visit(BoolExp exp, int level) {
        indent(level);
        System.out.println("BoolExp: " + exp.value);
    }

    public void visit(CallExp exp, int level) {
        indent(level);
        System.out.println("CallExp: " + exp.func);
        exp.args.accept(this, ++level);
    }

    public void visit(CompoundExp exp, int level) {
        indent(level);
        System.out.println("CompoundExp:");
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
        System.out.println("IfExp:");
        level++;
        exp.test.accept(this, level);
        exp.thenpart.accept(this, level);
        if (exp.elsepart != null) {
            exp.elsepart.accept(this, level);
        }
    }

    public void visit(IntExp exp, int level) {
        indent(level);
        System.out.println("IntExp: " + exp.value);
    }

    public void visit(NilExp exp, int level) {
        indent(level);
        System.out.println("NilExp");
    }

    public void visit(OpExp exp, int level) {
        indent(level);
        System.out.print("OpExp:");
        switch (exp.op) {
            case OpExp.LT:
                System.out.println(" < ");
                break;
            case OpExp.GT:
                System.out.println(" > ");
                break;
            case OpExp.LTE:
                System.out.println(" <= ");
                break;
            case OpExp.GTE:
                System.out.println(" >= ");
                break;
            case OpExp.EQUAL:
                System.out.println(" == ");
                break;
            case OpExp.NEQUAL:
                System.out.println(" != ");
                break;
            case OpExp.OR:
                System.out.println(" || ");
                break;
            case OpExp.AND:
                System.out.println(" && ");
                break;
            case OpExp.BNOT:
                System.out.println(" ~ ");
                break;
            case OpExp.ADD:
                System.out.println(" + ");
                break;
            case OpExp.SUBT:
                System.out.println(" - ");
                break;
            case OpExp.MULT:
                System.out.println(" * ");
                break;
            case OpExp.DIV:
                System.out.println(" / ");
                break;
            case OpExp.UMINUS:
                System.out.println(" - ");
                break;
            default:
                System.out.println("Unrecognized operator at line " + exp.row + " and column " + exp.col);
        }
        level++;
        if (exp.left != null) {
            exp.left.accept(this, level);
        }
        exp.right.accept(this, level);
    }

    public void visit(ReturnExp exp, int level) {
        indent(level);
        System.out.println("ReturnExp:");
        if (exp.exp != null) {
            exp.exp.accept(this, ++level);
        }
    }

    public void visit(VarExp exp, int level) {
        indent(level);
        System.out.println("VarExp:");
        exp.var.accept(this, ++level);
    }

    public void visit(WhileExp exp, int level) {
        indent(level);
        System.out.println("WhileExp:");
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
        System.out.println("FunctionDec: " + dec.func);
        level++;
        dec.result.accept(this, level);
        dec.params.accept(this, level);
        if (dec.body != null) {
            dec.body.accept(this, level);
        }
    }

    public void visit(ArrayDec varDec, int level) {
        indent(level);
        System.out.println("ArrayDec: " + varDec.name +
                " (size=" + varDec.size + ")");
        varDec.type.accept(this, ++level);
    }

    public void visit(SimpleDec varDec, int level) {
        indent(level);
        System.out.println("SimpleDec: " + varDec.name);
        varDec.type.accept(this, ++level);
    }
}
