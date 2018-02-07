package org.opendatakit.dhis2odk2bridge.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.message.FormattedMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.MessageSupplier;

public class JacksonObjectMessageSupplier<T> implements MessageSupplier {
  private final ObjectMapper objectMapper;
  private final T object;
  private final Class<T> klass;

  public JacksonObjectMessageSupplier(ObjectMapper objectMapper, T object, Class<T> klass) {
    this.objectMapper = objectMapper;
    this.object = object;
    this.klass = klass;
  }

  @Override
  public Message get() {
    try {
      return new FormattedMessage(objectMapper.writerFor(klass).withDefaultPrettyPrinter().writeValueAsString(object));
    } catch (JsonProcessingException e) {
      return new FormattedMessage("JacksonObjectMessage<{}> failed to serialize object {}", klass.getName(), object);
    }
  }
}
