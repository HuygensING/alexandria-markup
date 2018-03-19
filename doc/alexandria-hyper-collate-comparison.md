Hyper-Collate and Alexandria

In common:
Both modelled as hypergraph.

Text Graph:
- Hyper-Collate:
    - graph class: VariantWitnessGraph
    - The TextNode subgraph has to start and end with special nodes, so there is a single start and end node, even when there is interdocumentary variation at the start or at the end.
    - property: Sigil
- Alexandria:
    - graph class: Limen
    - No interdocumentary variation, so the document can point straight to the first TextToken
  
TextNode
- Alexandria:
    - class: TextNode
    - property: content
    - property: links to previous & next TextNodes (can move to Graph)
- Hyper-Collate:
    - class: SimpleTokenVertex
    - property: token
    - property: lists of incoming & outgoing TokenVertex
    - property: branchPath: in case of variation: list of parent branch ids
    - property: sigil

TextNode - TextNode Edge
- Alexandria:
    - just 1 outgoing edge between adjacent TextNodes
- Hyper-Collate:
    - Multiple outgoing edges in case of intradocumentary variation. 

Markup
- Alexandria:
    - recursive (lmnl)
    - property: parent limen (because of the recursion)
    - property: tag
    - property: id (from lmnl, must be unique)
    - property: suffix (from TexMECS, doesn't need to be unique)
    - property: annotations
    - property: textNodes (the markup-textnode hyperedge should move to the graph)
    - property: domination info (which markup dominates/is dominated by this markup, should also move to the graph)
- Hyper-Collate:
    - hierarchical, not recursive (xml)
    - property: tagName
    - property: attributeMap
    - property: depth (since it's hierarchical, how far from the root markup)
    - hierarchy info not stored.
    
Markup - Markup (Hyper)Edge
- none yet, but could be used to model markup dominance
  
Markup - TextNode HyperEdge
- in both, a single Markup has a hyperedge with multiple TextNodes
  

Common:
- TextGraph<TextNode,Markup>
