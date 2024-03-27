package absyn;

public class FunctionDec extends Dec {
    public NameTy result;
    public String func;
    public int funaddr;
    public VarDecList params;
    public Exp body;

    public FunctionDec(int row, int col, NameTy result, String func, VarDecList params, Exp body) {
        this.row = row;
        this.col = col;
        this.result = result;
        this.func = func;
        this.params = params;
        this.body = body;
        funaddr = 1;
    }

    public String getName() {
        return func;
    }

    public void accept(AbsynVisitor visitor, int level, boolean isAddress) {
        visitor.visit(this, level, isAddress);
    }

    public NameTy getType() {
        return result;
    }
}
