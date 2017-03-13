package nl.knaw.huygens.alexandria.lmnl.modifier;

public class Position {
  Integer offset = 0;
  Integer length = 0;

  public Position(int offset, int length) {
    this.offset = offset;
    this.length = length;
  }

  public Integer getOffset() {
    return offset;
  }

  public Integer getLength() {
    return length;
  }
}
