package nl.knaw.huygens.alexandria.lmnl.taagql;

import nl.knaw.huygens.alexandria.lmnl.data_model.Limen;
import nl.knaw.huygens.alexandria.lmnl.query.TAAGQLResult;

import java.util.function.Function;

public interface TAAGQLStatement {

  Function<Limen, TAAGQLResult> getLimenProcessor();

}
