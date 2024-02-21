import absyn.*;

public class ShowTreeVisitor implements AbsynVisitor {
    final static int SPACES = 4;

    private void indent(int level) {
        for(int i = 0; i < level * SPACES; i++) {
            System.out.print(" ");
        }
    }

    public void visit(NameTy type, int level) {
        // TODO
    }

    public void visit(IndexVar var, int level) {
        // TODO
    }

    public void visit(SimpleVar var, int level) {
        // TODO
    }

    public void visit(AssignExp exp, int level) {
        indent(level);
        System.out.println("AssignExp:");
        level++;
        exp.lhs.accept(this, level);
        exp.rhs.accept(this, level);
    }

    public void visit(BoolExp exp, int level) {
        // TODO
    }

    public void visit(CallExp exp, int level) {
        // TODO
    }

    public void visit(CompoundExp exp, int level) {
        // TODO
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
        // TODO
    }

    public void visit(OpExp exp, int level) {
        indent(level);
        System.out.print("OpExp:"); 
        switch(exp.op) {
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

    // public void visit(ReadExp exp, int level) {
    //     indent(level);
    //     System.out.println("ReadExp:");
    //     exp.input.accept(this, ++level);
    // }

    // public void visit(RepeatExp exp, int level) {
    //     indent(level);
    //     System.out.println("RepeatExp:");
    //     level++;
    //     exp.exps.accept(this, level);
    //     exp.test.accept(this, level); 
    // }

    public void visit(ReturnExp exp, int level) {
        // TODO
    }

    public void visit(VarExp exp, int level) {
        indent(level);
        System.out.println("VarExp: " + exp.name);
    }

    // public void visit(WriteExp exp, int level) {
    //     indent(level);
    //     System.out.println("WriteExp:");
    //     //if (exp.output != null)
    //     exp.output.accept(this, ++level);
    // }

    public void visit(WhileExp exp, int level) {
        // TODO
    }

    public void visit(ExpList expList, int level) {
        while(expList != null) {
            expList.head.accept(this, level);
            expList = expList.tail;
        }
    }

    public void visit(DecList decList, int level) {
        // TODO
    }

    public void visit(VarDecList varDecList, int level) {
        // TODO
    }

    public void visit(FunctionDec dec, int level) {
        // TODO
    }

    public void visit(ArrayDec varDec, int level) {
        // TODO
    }

    public void visit(SimpleDec varDec, int level) {
        // TODO
    }
}
