package absyn;

public abstract class VarDec extends Dec {
    public NameTy type;
    public String name;

    public NameTy getType() {
        return type;
    }
}
