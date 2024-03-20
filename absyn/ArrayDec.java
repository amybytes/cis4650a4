package absyn;

public class ArrayDec extends VarDec {
    public static final int UNKNOWN_SIZE = -1;

    public int size;

    public ArrayDec(int row, int col, NameTy type, String name, int size) {
        this.row = row;
        this.col = col;
        this.type = type;
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void accept(AbsynVisitor visitor, int level, boolean isAddress) {
        visitor.visit(this, level, isAddress);
    }
}
