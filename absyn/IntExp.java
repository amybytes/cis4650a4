package absyn;

public class IntExp extends Exp {
  public int value;

  public IntExp(int row, int col, int value) {
    this.row = row;
    this.col = col;
    this.value = value;
  }

  public void accept(AbsynVisitor visitor, int level, boolean isAddress) {
    visitor.visit(this, level, isAddress);
  }
}
