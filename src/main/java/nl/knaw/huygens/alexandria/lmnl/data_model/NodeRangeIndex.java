package nl.knaw.huygens.alexandria.lmnl.data_model;

import java.util.List;

public class NodeRangeIndex {

  private final List<IndexPoint> indexPoints;
  private final KdTree<IndexPoint> kdTree;

  public NodeRangeIndex(Limen limen) {
    indexPoints = limen.getIndexPoints();
    kdTree = new KdTree<>(indexPoints);
  }

  public List<IndexPoint> getIndexPoints() {
    return indexPoints;
  }

  public KdTree<IndexPoint> getKdTree() {
    return kdTree;
  }

}
