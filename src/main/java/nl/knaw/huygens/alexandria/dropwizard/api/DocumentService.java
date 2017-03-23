package nl.knaw.huygens.alexandria.dropwizard.api;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import nl.knaw.huygens.alexandria.lmnl.data_model.Document;

public class DocumentService {

  public Optional<DocumentInfo> getDocumentInfo(UUID uuid) {
    DocumentInfo documentInfo = new DocumentInfo(uuid)//
        .setCreated(Instant.now())//
        .setModified(Instant.now());
    return Optional.of(documentInfo);
  }

  public Optional<Document> getDocument(UUID uuid) {
    return Optional.empty();
  }

}
