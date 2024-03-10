package absyn;

public abstract class Absyn {
    public int row, col;

    public abstract void accept(AbsynVisitor visitor, int level);
}
