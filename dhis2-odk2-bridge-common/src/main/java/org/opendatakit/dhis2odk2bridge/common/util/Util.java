package org.opendatakit.dhis2odk2bridge.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import org.apache.logging.log4j.LogManager;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.dhis2odk2bridge.common.consts.MimeType;

import java.io.IOException;
import java.util.Objects;

public class Util {
  public static <T> T convertToObj(Class<T> klass, JSONObject winkJsonObj, ObjectMapper mapper) throws IOException {
    return mapper.readerFor(klass).readValue(winkJsonObj.toString());
  }

  public static <T> RequestBody createRequestBody(Class<T> payloadClass, T payload) {
    return createRequestBody(payloadClass, payload, MediaType.parse(MimeType.MIME_APPLICATION_JSON), new ObjectMapper());
  }

  public static <T> RequestBody createRequestBody(Class<T> payloadClass, T payload, ObjectMapper mapper) {
    return createRequestBody(payloadClass, payload, MediaType.parse(MimeType.MIME_APPLICATION_JSON), mapper);
  }

  public static <T> RequestBody createRequestBody(Class<T> payloadClass, T payload, MediaType mediaType, ObjectMapper mapper) {
    Objects.requireNonNull(mediaType);
    Objects.requireNonNull(mapper);

    return new RequestBody() {
      @Override
      public MediaType contentType() {
        return mediaType;
      }

      @Override
      public void writeTo(BufferedSink sink) throws IOException {
        LogManager.getLogger().debug(new JacksonObjectMessageSupplier<>(mapper, payload, payloadClass));

        mapper.writerFor(payloadClass).writeValue(sink.outputStream(), payload);
      }
    };
  }
}
