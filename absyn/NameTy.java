package absyn;

public class NameTy extends Absyn {
    public static final int BOOL = 0;
    public static final int INT  = 1;
    public static final int VOID = 2;
    
    public int type;

    public NameTy(int row, int col, int type) {
        this.row = row;
        this.col = col;
        this.type = type;
    }

    public String getTypeString() {
        switch (type) {
            case BOOL: return "BOOL";
            case INT:  return "INT";
            case VOID: return "VOID";
            default:   return "UNKNOWN";
        }
    }

    public void accept(AbsynVisitor visitor, int level) {
        visitor.visit(this, level);
    }
}
