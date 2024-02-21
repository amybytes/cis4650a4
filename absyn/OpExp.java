package absyn;

public class OpExp extends Exp {
    public final static int LT     = 0;
    public final static int GT     = 1;
    public final static int LTE    = 2;
    public final static int GTE    = 3;
    public final static int EQUAL  = 4;
    public final static int NEQUAL = 5;
    public final static int OR     = 6;
    public final static int AND    = 7;
    public final static int BNOT   = 8;
    public final static int ADD    = 9;
    public final static int SUBT   = 10;
    public final static int MULT   = 11;
    public final static int DIV    = 12;
    public final static int UMINUS = 13;

    public Exp left;
    public int op;
    public Exp right;

    public OpExp(int row, int col, Exp left, int op, Exp right) {
        this.row = row;
        this.col = col;
        this.left = left;
        this.op = op;
        this.right = right;
    }

    public void accept(AbsynVisitor visitor, int level) {
        visitor.visit(this, level);
    }
}
