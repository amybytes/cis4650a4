package absyn;

public class BoolExp extends Exp {
    public boolean value;

    public BoolExp(int row, int col, boolean value) {
        this.row = row;
        this.col = col;
        this.value = value;
    }
    
    public void accept(AbsynVisitor visitor, int level) {
        visitor.visit(this, level);
    }
}
