package nl.knaw.huygens.alexandria.lmnl.lql;

import java.util.function.Function;

import nl.knaw.huygens.alexandria.lmnl.data_model.Limen;
import nl.knaw.huygens.alexandria.lmnl.query.LQLResult;

public interface LQLStatement {

  Function<Limen, LQLResult> getLimenProcessor();

}
