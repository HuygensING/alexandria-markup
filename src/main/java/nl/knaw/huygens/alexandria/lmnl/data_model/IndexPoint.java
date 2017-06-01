package nl.knaw.huygens.alexandria.lmnl.data_model;

/**
 * Created by bramb on 3-3-2017.
 */
public class IndexPoint {
  final int textNodeIndex;
  final int markupIndex;

  public IndexPoint(int textNodeIndex, int markupIndex) {
    this.textNodeIndex = textNodeIndex;
    this.markupIndex = markupIndex;
  }

  public int getTextNodeIndex() {
    return textNodeIndex;
  }

  public int getMarkupIndex() {
    return markupIndex;
  }

  @Override
  public String toString() {
    return "(" + textNodeIndex + "," + markupIndex + ")";
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof IndexPoint) {
      IndexPoint other = (IndexPoint) obj;
      return (other.markupIndex == markupIndex) && (other.textNodeIndex == textNodeIndex);
    }
    return false;
  }

}
