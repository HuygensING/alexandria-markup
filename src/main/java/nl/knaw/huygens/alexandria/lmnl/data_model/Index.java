package nl.knaw.huygens.alexandria.lmnl.data_model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by bramb on 2-3-2017.
 */
public class Index {
  enum Dimension {textNode, textRange}

  private class KDNode extends IndexPoint {
    KDNode leftNode;
    KDNode rightNode;
    Dimension dimension;

    public KDNode(int textNodeIndex, int textRangeIndex, Dimension dimension) {
      super(textNodeIndex, textRangeIndex);
      this.dimension = dimension;
    }

    public KDNode getLeftNode() {
      return leftNode;
    }

    public void setLeftNode(KDNode leftNode) {
      this.leftNode = leftNode;
    }

    public KDNode getRightNode() {
      return rightNode;
    }

    public void setRightNode(KDNode rightNode) {
      this.rightNode = rightNode;
    }
  }

  KDNode root;


  public Index(Limen limen) {
    List<TextRange> textRanges = limen.textRangeList;
    this.root = new KDNode(0, 0, Dimension.textNode);
  }

  public List<TextNode> getTextNodesinTextRange(TextRange textRange) {
    List<TextNode> textNodes = new ArrayList<>();
    return textNodes;
  }
}
