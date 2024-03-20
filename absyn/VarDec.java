package absyn;

public abstract class VarDec extends Dec {
    public NameTy type;
    public String name;
    public int offset;
    public int nestLevel;

    public NameTy getType() {
        return type;
    }
}
