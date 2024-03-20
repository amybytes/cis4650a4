package absyn;

public class CompoundExp extends Exp {
    public VarDecList decs;
    public ExpList exps;

    public CompoundExp(int row, int col, VarDecList decs, ExpList exps) {
        this.row = row;
        this.col = col;
        this.decs = decs;
        this.exps = exps;
    }

    public void accept(AbsynVisitor visitor, int level, boolean isAddress) {
        visitor.visit(this, level, isAddress);
    }
}
