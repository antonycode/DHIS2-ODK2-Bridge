package org.opendatakit.dhis2odk2bridge.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstances;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.importexport.ImportStrategy;
import org.opendatakit.dhis2odk2bridge.common.consts.Dhis2Path;
import org.opendatakit.dhis2odk2bridge.common.consts.MimeType;
import org.opendatakit.dhis2odk2bridge.common.model.*;
import org.opendatakit.dhis2odk2bridge.common.util.JacksonObjectMessageSupplier;
import org.opendatakit.dhis2odk2bridge.common.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        .addMixIn(Pager.class, PagerMixin.class);

    this.logger = LogManager.getLogger();
  }

  public DataSets getDataSets() throws IOException {
    return getNameIdPairContainer(DataSets.class, Dhis2Path.DATA_SETS, "failed to get data sets");
  }

  public Optional<String> getDataSetId(String name) throws IOException {
    Objects.requireNonNull(name);
    return findNameIdPairByName(getDataSets(), name);
  }

  public TrackedEntities getTrackedEntities() throws IOException {
    return getNameIdPairContainer(TrackedEntities.class, Dhis2Path.TRACKED_ENTITIES, "failed to get tracked entities");
  }

  public Optional<String> getTrackedEntityId(String name) throws IOException {
    Objects.requireNonNull(name);
    return findNameIdPairByName(getTrackedEntities(), name);
  }

  public OrgUnits getOrgUnits() throws IOException {
    return getNameIdPairContainer(OrgUnits.class, Dhis2Path.ORG_UNITS, "failed to get organization units");
  }

  public Optional<String> getOrgUnitId(String name) throws IOException {
    Objects.requireNonNull(name);
    return findNameIdPairByName(getOrgUnits(), name);
  }

  public TrackedEntityAttributes getTrackedEntityAttributes() throws IOException {
    return getNameIdPairContainer(TrackedEntityAttributes.class, Dhis2Path.TRACKED_ENTITY_ATTR, "failed to get tracked entity attributes");
  }

  public DataValueSet getDataValueSets(String dataSet, List<String> orgUnit, String startDate, String endDate,
                                       boolean children, boolean includeDeleted) throws IOException {
    Objects.requireNonNull(dataSet);
    Objects.requireNonNull(orgUnit);
    Objects.requireNonNull(startDate);
    Objects.requireNonNull(endDate);

    HttpUrl.Builder builder = baseUrl
        .newBuilder()
        .addPathSegment(Dhis2Path.DATA_VALUE_SETS)
        .addQueryParameter("dataSet", dataSet)
        .addQueryParameter("startDate", startDate)
        .addQueryParameter("endDate", endDate)
        .addQueryParameter("children", Boolean.toString(children))
        .addQueryParameter("includeDeleted", Boolean.toString(includeDeleted))
        .addQueryParameter("idScheme", IdScheme.NAME.getIdentifiableString());

    orgUnit.forEach(ou -> builder.addQueryParameter("orgUnit", ou));

    HttpUrl url = builder.build();

    Request request = baseRequest
        .newBuilder()
        .url(url)
        .build();

    try (Response response = httpClient.newCall(request).execute()) {
      return extractEntityFromResponse(DataValueSet.class, response, objectMapper, "failed to get data value sets");
    }
  }

  public TrackedEntityInstances getTrackedEntityInstances(String trackedEntity, List<String> orgUnit,
                                                          OrganisationUnitSelectionMode ouMode,
                                                          boolean includeDeleted) throws IOException {
    Objects.requireNonNull(trackedEntity);
    Objects.requireNonNull(orgUnit);
    Objects.requireNonNull(ouMode);

    HttpUrl.Builder builder = baseUrl
        .newBuilder()
        .addPathSegment(Dhis2Path.TEI)
        .addQueryParameter("trackedEntity", trackedEntity)
        .addQueryParameter("ouMode", ouMode.name())
        .addQueryParameter("includeDeleted", Boolean.toString(includeDeleted));

    if (ouMode != OrganisationUnitSelectionMode.ALL) {
      builder.addQueryParameter("ou", String.join(";", orgUnit));
    }

    HttpUrl url = builder.build();

    Request request = baseRequest
        .newBuilder()
        .url(url)
        .build();

    try (Response response = httpClient.newCall(request).execute()) {
      return extractEntityFromResponse(TrackedEntityInstances.class, response, objectMapper, "failed to get tracked entity instances");
    }
  }

  public ImportSummary postDataValueSet(DataValueSet toPost, ImportOptions importOptions) throws IOException {
    Objects.requireNonNull(toPost);
    Objects.requireNonNull(importOptions);

    HttpUrl url = baseUrl
        .newBuilder()
        .addPathSegment(Dhis2Path.DATA_VALUE_SETS)
        .addQueryParameter("dryRun", Boolean.toString(importOptions.isDryRun()))
        .addQueryParameter("idScheme", IdScheme.NAME.getIdentifiableString())
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

  public ImportSummaries postTrackedEntityInstances(TrackedEntityInstances toPost, ImportOptions importOptions) throws IOException {
    Objects.requireNonNull(toPost);
    Objects.requireNonNull(importOptions);

    HttpUrl url = baseUrl
        .newBuilder()
        .addPathSegment(Dhis2Path.TEI)
        .addQueryParameter("strategy", ImportStrategy.CREATE_AND_UPDATE.name())
        .addQueryParameter("dryRun", Boolean.toString(importOptions.isDryRun()))
        // using NAME here causes silent error on the server side
        // the trackedEntityInstance gets persisted
        // but not the attribute values
        // .addQueryParameter("idScheme", IdScheme.NAME.getIdentifiableString())
        .addQueryParameter("async", Boolean.toString(false)) // async import not supported
        .build();

    Request request = baseRequest
        .newBuilder()
        .url(url)
        .post(Util.createRequestBody(TrackedEntityInstances.class, toPost, objectMapper))
        .build();

    try (Response response = httpClient.newCall(request).execute()) {
      return extractEntityFromResponse(WebMessageWithSummaries.class, response, objectMapper, "failed to post tracked entity instances")
          .getResponse();
    }
  }

  private <T extends NameIdPair, U extends NameIdPairContainer<T>> U getNameIdPairContainer(Class<U> klass, String path, String errMsg) throws IOException {
    HttpUrl url = baseUrl
        .newBuilder()
        .addPathSegment(path)
        .addQueryParameter("fields", "id,displayName")
        .build();

    Request request = baseRequest
        .newBuilder()
        .url(url)
        .get()
        .build();

    try (Response response = httpClient.newCall(request).execute()) {
      return extractEntityFromResponse(klass, response, objectMapper, errMsg);
    }
  }

  private <T extends NameIdPair> Optional<String> findNameIdPairByName(NameIdPairContainer<T> list, String name) {
    return list.getList().stream().filter(pair -> pair.getDisplayName().equals(name)).findAny().map(NameIdPair::getId);
  }

  private <T> T extractEntityFromResponse(Class<T> klass, Response response,
                                          ObjectMapper mapper, String fallbackErrMsg) throws IOException {
    return extractEntityFromResponse(klass, response, mapper, () -> createDhis2IoException(fallbackErrMsg, response.body()));
  }

  private <T, E extends Throwable> T extractEntityFromResponse(Class<T> klass, Response response,
                                                                      ObjectMapper mapper, Supplier<E> exceptionSupplier)
      throws IOException, E {
    if (!response.isSuccessful()) {
      throw exceptionSupplier.get();
    }

    try (ResponseBody body = response.body()) {
      // body should not be null
      try (InputStream stream = body.byteStream()) {
        T respObj = mapper.readerFor(klass).readValue(stream);
        logger.debug(new JacksonObjectMessageSupplier<>(mapper, respObj, klass));

        return respObj;
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
