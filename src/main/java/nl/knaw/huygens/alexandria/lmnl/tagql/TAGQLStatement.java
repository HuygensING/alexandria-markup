package nl.knaw.huygens.alexandria.lmnl.tagql;

import nl.knaw.huygens.alexandria.lmnl.data_model.Limen;
import nl.knaw.huygens.alexandria.lmnl.query.TAGQLResult;

import java.util.function.Function;

public interface TAGQLStatement {

  Function<Limen, TAGQLResult> getLimenProcessor();

}
