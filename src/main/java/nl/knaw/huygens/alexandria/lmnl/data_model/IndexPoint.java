package nl.knaw.huygens.alexandria.lmnl.data_model;

/**
 * Created by bramb on 3-3-2017.
 */
public class IndexPoint {
  int textNodeIndex;
  int textRangeIndex;

  public IndexPoint(int textNodeIndex, int textRangeIndex) {
    this.textNodeIndex = textNodeIndex;
    this.textRangeIndex = textRangeIndex;
  }

  public int getTextNodeIndex() {
    return textNodeIndex;
  }

  public int getTextRangeIndex() {
    return textRangeIndex;
  }

  @Override
  public String toString() {
    return "(" + textNodeIndex + "," + textRangeIndex + ")";
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof IndexPoint) {
      IndexPoint other = (IndexPoint) obj;
      return (other.textRangeIndex == textRangeIndex) && (other.textNodeIndex == textNodeIndex);
    }
    return false;
  }

}
