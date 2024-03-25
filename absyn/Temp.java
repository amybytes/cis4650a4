package absyn;

public class Temp {
    public static int GLOBAL_SCOPE = 0;
    public static int LOCAL_SCOPE = 1;

    public int offset;
    public int scope;

    public Temp(int offset, int scope) {
        this.offset = offset;
        this.scope = scope;
    }
}
