Hyper-Collate and Alexandria

In common:
Both modeled as hypergraph.

Text Graph:
- Hyper-Collate:
    - graph class: VariantWitnessGraph
    - The TextNode subgraph has to start and end with special nodes, so there is a single start and end node, even when there is interdocumentary variation at the start or at the end.
    - Sigil
- Alexandria:
    - graph class: Limen
    - No interdocumentary variation, so the document can point straight to the first TextToken
  


TextNode
- Alexandria:
    - class: TextNode
    - content
    - links to previous & next TextNodes (can move to Graph)
- Hyper-Collate:
    - class: SimpleTokenVertex
    - token
    - lists of incoming & outgoing TokenVertex
    - branchPath: in case of variation: list of parent branch ids
    - sigil

TextNode - TextNode Edge
- Alexandria:
    - just 1 outgoing edge between adjacent TextNodes
- Hyper-Collate:
    - Multiple outgoing edges in case of intradocumentary variation. 

Markup
- Alexandria:
    - recursive (lmnl)
    - parent limen (because of the recursion)
    - tag
    - id (from lmnl, must be unique)
    - suffix (from TexMECS, doesn't need to be unique)
    - annotations
    - textNodes (the markup-textnode hyperedge should move to the graph)
    - domination info (which markup dominates/is dominated by this markup, should also move to the graph)
- Hyper-Collate:
    - hierarchical, not recursive (xml)
    - tagName
    - attributeMap
    - depth (since it's hierarchical, how far from the root markup)
    - hierarchy info not stored.
    
Markup - Markup (Hyper)Edge
    - none yet, but could be used to model markup dominance
  
Markup - TextNode HyperEdge
  - in both, a single Markup has a hyperedge with multiple TextNodes
  - 
  

Common:
- TextGraph<TextNode,Markup>

  
  