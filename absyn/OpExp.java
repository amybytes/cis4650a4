package absyn;

public class OpExp extends Exp {
    public static final int LT = 0;
    public static final int GT = 1;
    public static final int LTE = 2;
    public static final int GTE = 3;
    public static final int EQUAL = 4;
    public static final int NEQUAL = 5;
    public static final int OR = 6;
    public static final int AND = 7;
    public static final int BNOT = 8;
    public static final int ADD = 9;
    public static final int SUBT = 10;
    public static final int MULT = 11;
    public static final int DIV = 12;
    public static final int UMINUS = 13;

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

    public String getDisplayOp() {
        switch (op) {
            case LT:
                return "<";
            case GT:
                return ">";
            case LTE:
                return "<=";
            case GTE:
                return ">=";
            case EQUAL:
                return "=";
            case NEQUAL:
                return "!=";
            case OR:
                return "||";
            case AND:
                return "&&";
            case BNOT:
                return "~";
            case ADD:
                return "+";
            case SUBT:
                return "-";
            case MULT:
                return "*";
            case DIV:
                return "/";
            case UMINUS:
                return "-";
            default:
                return null;
        }
    }

    public void accept(AbsynVisitor visitor, int level, boolean isAddress) {
        visitor.visit(this, level, isAddress);
    }
}
