# tagql

This library contains the ANTLR4-generated `TAGQLLexer` and `TAGQLParser` for tokenizing and parsing TAGQL queries.

## maven usage

add this dependency to your `pom.xml` 

```xml
<dependency>
  <groupId>nl.knaw.huygens.alexandria</groupId>
  <artifactId>tagql</artifactId>
  <version>2.4.1-SNAPSHOT</version>
</dependency>
```

and this repository definition:
```xml
<repository>
  <id>huygens</id>
  <url>http://maven.huygens.knaw.nl/repository/</url>
  <releases>
    <enabled>true</enabled>
    <updatePolicy>always</updatePolicy>
    <checksumPolicy>warn</checksumPolicy>
  </releases>
  <snapshots>
    <enabled>true</enabled>
    <updatePolicy>always</updatePolicy>
    <checksumPolicy>fail</checksumPolicy>
  </snapshots>
</repository>
```
