package nl.knaw.huygens.alexandria.dropwizard.resources;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableList;

import nl.knaw.huygens.alexandria.dropwizard.api.DocumentInfo;
import nl.knaw.huygens.alexandria.dropwizard.api.DocumentService;
import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.exporter.LMNLExporter;

@Path("/documents")
@Produces(MediaType.APPLICATION_JSON)
public class DocumentsResource {

  final DocumentService documentService;
  private LMNLExporter lmnlExporter;

  public DocumentsResource(DocumentService documentService, LMNLExporter lmnlExporter) {
    this.documentService = documentService;
    this.lmnlExporter = lmnlExporter;
  }

  @GET
  @Timed
  public List<URI> getDocumentURIs() {
    List<URI> list = ImmutableList.of(URI.create("http://localhost:8080/documents/UUID"));
    return list;
  }

  @POST
  @Timed
  public Response addDocument() {
    UUID documentId = UUID.randomUUID();
    return Response.created(URI.create("http://localhost:8080/documents/" + documentId)).build();
  }

  @PUT
  @Path("{uuid}")
  @Timed
  public Response setDocument(@PathParam("uuid") final UUID uuid) {
    return Response.created(URI.create("http://localhost:8080/documents/" + uuid)).build();
  }

  @GET
  @Path("{uuid}")
  @Timed
  public Response getDocumentInfo(@PathParam("uuid") final UUID uuid) {
    DocumentInfo documentInfo = documentService.getDocumentInfo(uuid)//
        .orElseThrow(NotFoundException::new);
    return Response.ok(documentInfo).build();
  }

  @GET
  @Path("{uuid}/lmnl")
  @Timed
  @Produces(MediaType.TEXT_PLAIN)
  public Response getLMNL(@PathParam("uuid") final UUID uuid) {
    Document document = documentService.getDocument(uuid)//
        .orElseThrow(NotFoundException::new);
    String lmnl = lmnlExporter.toLMNL(document);
    return Response.ok(lmnl).build();
  }

  @GET
  @Path("{uuid}/latex")
  @Timed
  @Produces(MediaType.TEXT_PLAIN)
  public Response getLaTeXVisualization(@PathParam("uuid") final UUID uuid) {
    String latex = "";
    return Response.ok(latex).build();
  }

}
