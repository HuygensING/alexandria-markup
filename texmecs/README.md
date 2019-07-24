# texmecs

This library contains the ANTLR4-generated `TexMECSLexer` and `TexMECSParser` for tokenizing and parsing TexMECS documents.

## maven usage

add this dependency to your `pom.xml` 

```xml
<dependency>
  <groupId>nl.knaw.huygens.alexandria</groupId>
  <artifactId>texmecs</artifactId>
  <version>2.2</version>
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
