package nl.knaw.huygens.alexandria.lmnl.data_model;

/**
 * Created by Ronald Haentjens Dekker on 29/12/16.
 *
 * A document contains a Limen.
 *
 */
public class Document {
    private final Limen value;

    public Document() {
        this.value = new Limen();
    }

    public Limen value() {
        return value;
    }
}
