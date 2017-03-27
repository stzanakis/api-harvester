package eu.europeana.harvest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.utils.URIBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2017-03-27
 */
public class ApiHarvester {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiHarvester.class);
  private String apiEndpoint;
  private String recordListField;
  private String limitParameterName;
  private int limit;
  private String offsetParameterName;
  private int offset;
  private File singleHarvestOutputDirectory;

  public ApiHarvester(String apiEndpoint, String recordListField,
      String offsetParameterName, int offset, String limitParameterName, int limit,
      String harvestOutputDirectory) throws IOException {
    this.apiEndpoint = apiEndpoint;
    this.offsetParameterName = offsetParameterName;
    this.offset = offset;
    this.recordListField = recordListField;
    this.limitParameterName = limitParameterName;
    this.limit = limit;
    this.singleHarvestOutputDirectory = new File(harvestOutputDirectory,
        "harvest_wtih_timestamp_uid");
    if (this.singleHarvestOutputDirectory.exists()) {
      FileUtils.deleteDirectory(singleHarvestOutputDirectory);
    }
    this.singleHarvestOutputDirectory.mkdir();
  }

  public void harvest(boolean parallel, boolean singleDirectory)
      throws IOException, URISyntaxException, ParseException {
    LOGGER.info("Harvesting is starting..");

    //Check if url is valid
    new URL(apiEndpoint);
    harvestIterativeSingleDirectory();
//    if (parallel && singleDirectory) {
//      harvestParallelSingleDirectory();
//    } else if (parallel && !singleDirectory) {
//      harvestParallel();
//    } else if (!parallel && singleDirectory) {
//      harvestIterativeSingleDirectory();
//    } else if (!parallel && !singleDirectory) {
//      harvestIterative();
//    }
  }

//  private void harvestParallel() {
//
//  }
//
//  private void harvestParallelSingleDirectory() {
//
//  }
//
//  private void harvestIterative() {
//    DirectoryController directoryController = new DirectoryController(10, singleHarvestOutputDirectory);
//    File fileToWriteOnDirectoryStructure = directoryController
//        .getFileToWriteOnDirectoryStructure(offset, limit);
//
//
//  }

  private void harvestIterativeSingleDirectory()
      throws URISyntaxException, IOException, ParseException {
    String response;
    int recordsCounter = 0;
    String recordResult;
    int responseRecordsCount;
    do {
      recordResult = null;
      responseRecordsCount = 0;
      response = getNextResult(offset, limit);
      ResultFormat resultFormat;
      if (response != null && !response.equals("")) {
        resultFormat = identifyResultFormat(response);
        responseRecordsCount = getTotalRecordsInResponse(resultFormat, response);
        recordResult = getStringRepresentation(resultFormat, response);

        LOGGER.info("Response records: " + responseRecordsCount);
        if (responseRecordsCount != 0) {
          File output = new File(singleHarvestOutputDirectory, "request." + offset + "-" +
              (responseRecordsCount < limit ? (offset + responseRecordsCount) : (offset + limit))
              + ".txt");
          FileUtils.writeStringToFile(output, recordResult);
        }
        offset += limit;
        recordsCounter += responseRecordsCount;
        LOGGER.info("Records harvested until now: " + recordsCounter);
      }
    } while (recordResult != null && responseRecordsCount != 0);
    LOGGER.info("Harvest finished with total records harvested: " + recordsCounter);
  }

  private String getNextResult(int offset, int limit)
      throws URISyntaxException, IOException, ParseException {
    String urlString = apiEndpoint;
    URIBuilder uriBuilder = new URIBuilder(urlString);
    uriBuilder.setParameter(limitParameterName, String.valueOf(limit));
    uriBuilder.setParameter(offsetParameterName, String.valueOf(offset));
    URI uri = uriBuilder.build();

    LOGGER.info("Request GET from: " + uri);

    HttpURLConnection con = (HttpURLConnection) uri.toURL().openConnection();
    int responseCode = con.getResponseCode();
    LOGGER.info("Response status: " + responseCode);

    BufferedReader in = new BufferedReader(
        new InputStreamReader(con.getInputStream()));
    String inputLine;
    StringBuilder response = new StringBuilder();

    while ((inputLine = in.readLine()) != null) {
      response.append(inputLine);
    }
    in.close();

    String responseString = response.toString();
    return responseString.equals("")?null:responseString;
  }


  private ResultFormat identifyResultFormat(String result) {
    if (result.startsWith("<")) {
      return ResultFormat.XML;
    } else {
      return ResultFormat.JSON;
    }
  }

  private String getStringRepresentation(ResultFormat resultFormat, String result)
      throws ParseException {
    String prettyRecords = null;
    switch (resultFormat) {
      case JSON:
        JSONObject json = (JSONObject) new JSONParser().parse(result);
        JSONArray jsonArray = (JSONArray) json.get(recordListField);
        String records = jsonArray.toJSONString();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(records);
        prettyRecords = gson.toJson(je);
        break;
      case XML:
        break;
    }
    return prettyRecords;
  }

  private int getTotalRecordsInResponse(ResultFormat resultFormat, String response)
      throws ParseException {
    switch (resultFormat) {
      case JSON:
        JSONObject json = (JSONObject) new JSONParser().parse(response);
        JSONArray jsonArray = (JSONArray) json.get(recordListField);
        return jsonArray.size();
      case XML:
        break;
    }
    return 0;
  }

  public enum ResultFormat {
    JSON, XML
  }
}
