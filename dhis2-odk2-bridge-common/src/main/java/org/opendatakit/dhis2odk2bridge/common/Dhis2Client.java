package org.opendatakit.dhis2odk2bridge.common;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.opendatakit.dhis2odk2bridge.common.model.DataSets;
import org.opendatakit.dhis2odk2bridge.common.model.WebMessage;
import org.opendatakit.dhis2odk2bridge.common.consts.Dhis2Path;
import org.opendatakit.dhis2odk2bridge.common.consts.MimeType;
import org.opendatakit.dhis2odk2bridge.common.model.PagerMixin;
import org.opendatakit.dhis2odk2bridge.common.util.Util;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;

public class Dhis2Client {
  private final OkHttpClient httpClient;
  private final HttpUrl baseUrl;
  private final Request baseRequest;

  private final ObjectMapper objectMapper;

  private final Logger logger;

  public Dhis2Client(String url, String username, String password) {
    Objects.requireNonNull(url);
    Objects.requireNonNull(username);
    Objects.requireNonNull(password);

    this.httpClient = new OkHttpClient.Builder().build();
    this.baseUrl = HttpUrl.parse(url);
    this.baseRequest = new Request.Builder()
        .url(url) // placeholder
        // Cannot use Authenticator, DHIS2 server doesn't return 401 when Authorization header is not supplied
        .header("Authorization", Credentials.basic(username, password))
        .header("Accept", MimeType.MIME_APPLICATION_JSON)
        .build();

    Objects.requireNonNull(this.baseUrl, "Unable to parse url: " + url);

    // DHIS2 has a typo, use case insensitive to get around unknown property exception
    // disable auto detection to avoid serializing fields/getters that should not be serialized
    this.objectMapper = new ObjectMapper()
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        .configure(MapperFeature.AUTO_DETECT_CREATORS, false)
        .configure(MapperFeature.AUTO_DETECT_FIELDS, false)
        .configure(MapperFeature.AUTO_DETECT_GETTERS, false)
        .configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false)
        .addMixIn(Pager.class, PagerMixin.class);

    this.logger = LogManager.getLogger();
  }

  public DataSets getDataSets() throws IOException {
    HttpUrl url = baseUrl
        .newBuilder()
        .addPathSegment(Dhis2Path.DATA_SETS)
        .addQueryParameter("fields", String.join(", ", "id", "name"))
        .build();

    Request request = baseRequest
        .newBuilder()
        .url(url)
        .build();

    try (Response response = httpClient.newCall(request).execute()) {
      return extractEntityFromResponse(DataSets.class, response, objectMapper, "failed to get data sets");
    }
  }

  public DataValueSet getDataValueSets(String dataSet, String orgUnit, String startDate, String endDate,
                                       boolean children, boolean includeDeleted) throws IOException {
    Objects.requireNonNull(dataSet);
    Objects.requireNonNull(orgUnit);
    Objects.requireNonNull(startDate);
    Objects.requireNonNull(endDate);

    HttpUrl url = baseUrl
        .newBuilder()
        .addPathSegment(Dhis2Path.DATA_VALUE_SETS)
        .addQueryParameter("dataSet", dataSet)
        .addQueryParameter("orgUnit", orgUnit)
        .addQueryParameter("startDate", startDate)
        .addQueryParameter("endDate", endDate)
        .addQueryParameter("children", Boolean.toString(children))
        .addQueryParameter("includeDeleted", Boolean.toString(includeDeleted))
        .addQueryParameter("idScheme", IdScheme.NAME.getIdentifiableString())
        .build();

    Request request = baseRequest
        .newBuilder()
        .url(url)
        .build();

    try (Response response = httpClient.newCall(request).execute()) {
      return extractEntityFromResponse(DataValueSet.class, response, objectMapper, "failed to get data value sets");
    }
  }

  public ImportSummary postDataValueSet(DataValueSet toPost, ImportOptions importOptions) throws IOException {
    Objects.requireNonNull(toPost);

    HttpUrl url = baseUrl
        .newBuilder()
        .addPathSegment(Dhis2Path.DATA_VALUE_SETS)
        .addQueryParameter("dryRun", Boolean.toString(importOptions.isDryRun()))
        .addQueryParameter("idScheme", importOptions.getIdSchemes().getIdScheme().getIdentifiableString())
        .addQueryParameter("async", Boolean.toString(false)) // async import not supported
        .build();

    Request request = baseRequest
        .newBuilder()
        .url(url)
        .post(Util.createRequestBody(DataValueSet.class, toPost, objectMapper))
        .build();

    try (Response response = httpClient.newCall(request).execute()) {
      return extractEntityFromResponse(ImportSummary.class, response, objectMapper, "failed to post data value set");
    }
  }

  private <T> T extractEntityFromResponse(Class<T> klass, Response response,
                                                 ObjectMapper mapper, String fallbackErrMsg) throws IOException {
    return extractEntityFromResponse(klass, response, mapper, () -> createDhis2IoException(fallbackErrMsg, response.body()));
  }

  private <T, E extends Throwable> T extractEntityFromResponse(Class<T> klass, Response response,
                                                                      ObjectMapper mapper, Supplier<E> exceptionSupplier)
      throws IOException, E {
    try (ResponseBody body = response.body()) {
      if (response.isSuccessful()) {
        return mapper.readerFor(klass).readValue(body.bytes());
      } else {
        throw exceptionSupplier.get();
      }
    }
  }

  private IOException createDhis2IoException(String genericErrMsg, ResponseBody body) {
    if (body == null) {
      return new IOException(genericErrMsg);
    }

    try {
      return new IOException(new ObjectMapper()
          .readerFor(WebMessage.class)
          .<WebMessage>readValue(body.bytes())
          .getMessage()
      );
    } catch (IOException e) {
      logger.warn("failed to parse error message", e);
      return new IOException(genericErrMsg);
    }
  }
}
