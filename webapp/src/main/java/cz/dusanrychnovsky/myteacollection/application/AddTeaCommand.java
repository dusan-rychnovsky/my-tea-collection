package cz.dusanrychnovsky.myteacollection.application;

import cz.dusanrychnovsky.myteacollection.domain.Price;
import cz.dusanrychnovsky.myteacollection.domain.TeaScope;

import java.util.List;
import java.util.Set;

/**
 * Input to the {@link AddTea} use case: everything needed to add a tea, translated by the
 * inbound adapter (web / ingest) into a common shape. Reference data is carried by id
 * (resolved and validated by {@link AddTea}); an absent price is a {@code null} {@link Price};
 * images are ordered, already-compressed bytes.
 */
public record AddTeaCommand(
  String title,
  String name,
  String description,
  String url,
  TeaScope scope,
  Price price,
  String brewingInstructions,
  boolean inStock,
  Long userId,
  Long vendorId,
  Set<Long> typeIds,
  Set<Long> tagIds,
  List<byte[]> images
) {
}
