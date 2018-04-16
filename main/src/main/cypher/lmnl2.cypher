# [s}[a}Bees [b}make{a] honey{b]{s]
create (d:Document{id:"d2"})-[:FIRST_TEXT_SEGMENT]->(t1:TextSegment{text:"Bees "})-[:NEXT]->(t2:TextSegment{text:"make"})-[:NEXT]->(t3:TextSegment{text:" honey"}),
       (d)-[:HAS_RANGE{n:1}]->(r1:Range{name:"s",id:"r1"}),
       (r1)-[:FIRST_TEXT_SEGMENT]->(t1),
       (r1)-[:LAST_TEXT_SEGMENT]->(t3),

       (d)-[:HAS_RANGE{n:2}]->(r2:Range{name:"a",id:"r2"}),
       (r2)-[:FIRST_TEXT_SEGMENT]->(t1),
       (r2)-[:LAST_TEXT_SEGMENT]->(t2),

       (d)-[:HAS_RANGE{n:3}]->(r3:Range{name:"b",id:"r3"}),
       (r3)-[:FIRST_TEXT_SEGMENT]->(t2),
       (r3)-[:LAST_TEXT_SEGMENT]->(t3)
;
match (d:Document{id:"d2"})-[*]-(o) return d,o;
