package nl.knaw.huygens.alexandria.texmecs.importer;

public class TexMECSSyntaxError extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public TexMECSSyntaxError(String message) {
    super(message);
  }
}
