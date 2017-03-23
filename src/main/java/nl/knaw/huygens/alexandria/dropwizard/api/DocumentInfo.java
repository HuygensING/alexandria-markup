package nl.knaw.huygens.alexandria.dropwizard.api;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;

public class DocumentInfo {
  Instant created;
  Instant modified;
  private UUID documentId;

  public DocumentInfo(UUID documentId) {
    this.documentId = documentId;
  }

  public DocumentInfo setCreated(Instant created) {
    this.created = created;
    return this;
  }

  @JsonSerialize(using = InstantSerializer.class)
  public Instant getCreated() {
    return created;
  }

  public DocumentInfo setModified(Instant modified) {
    this.modified = modified;
    return this;
  }

  @JsonSerialize(using = InstantSerializer.class)
  public Instant getModified() {
    return modified;
  }

  @JsonProperty("^lmnl")
  public URI getLMNLURI() {
    return URI.create("/documents/" + documentId + "/lmnl");
  }

  @JsonProperty("^latex1")
  public URI getLaTeX1() {
    return URI.create("/documents/" + documentId + "/latex1");
  }

  @JsonProperty("^latex2")
  public URI getLaTeX2() {
    return URI.create("/documents/" + documentId + "/latex2");
  }

}
