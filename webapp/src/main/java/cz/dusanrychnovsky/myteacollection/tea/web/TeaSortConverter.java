package cz.dusanrychnovsky.myteacollection.tea.web;

import cz.dusanrychnovsky.myteacollection.tea.query.TeaSort;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TeaSortConverter implements Converter<String, TeaSort> {

  @Override
  public TeaSort convert(String source) {
    return TeaSort.fromQueryValue(source);
  }
}
