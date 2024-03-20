package absyn;

public interface AbsynVisitor {
    public void visit(NameTy type, int level, boolean isAddress);

    public void visit(IndexVar var, int level, boolean isAddress);

    public void visit(SimpleVar var, int level, boolean isAddress);

    public void visit(AssignExp exp, int level, boolean isAddress);

    public void visit(BoolExp exp, int level, boolean isAddress);

    public void visit(CallExp exp, int level, boolean isAddress);

    public void visit(CompoundExp exp, int level, boolean isAddress);

    public void visit(IfExp exp, int level, boolean isAddress);

    public void visit(IntExp exp, int level, boolean isAddress);

    public void visit(NilExp exp, int level, boolean isAddress);

    public void visit(OpExp exp, int level, boolean isAddress);

    public void visit(ReturnExp exp, int level, boolean isAddress);

    public void visit(VarExp exp, int level, boolean isAddress);

    public void visit(WhileExp exp, int level, boolean isAddress);

    public void visit(FunctionDec dec, int level, boolean isAddress);

    public void visit(SimpleDec dec, int level, boolean isAddress);

    public void visit(ArrayDec dec, int level, boolean isAddress);

    public void visit(DecList list, int level, boolean isAddress);

    public void visit(VarDecList list, int level, boolean isAddress);

    public void visit(ExpList list, int level, boolean isAddress);
}
