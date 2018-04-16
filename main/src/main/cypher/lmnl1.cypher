# [l [n}144{n]}He manages to keep the upper hand{l]
create (d:Document{id:"d1"})-[:FIRST_TEXT_SEGMENT]->(ts:TextSegment{text:"He manages to keep the upper hand"}),
       (d)-[:HAS_RANGE{n:1}]->(r:Range{name:"l",id:"r1"}),
       (r)-[:FIRST_TEXT_SEGMENT]->(ts),
       (r)-[:LAST_TEXT_SEGMENT]->(ts),
       (r)-[:HAS_ANNOTATION{n:1}]->(a:Annotation{name:"n"})-[:FIRST_TEXT_SEGMENT]->(at:TextSegment{text:"144"});
match (d:Document{id:"d1"})-[*]-(o) return d,o;