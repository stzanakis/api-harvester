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
import java.util.List;
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
    this.singleHarvestOutputDirectory = new File(harvestOutputDirectory, "harvest_wtih_timestamp_uid");
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
    List<String> records;
    int recordsCounter = 0;
    do {
      records = getNextResult(offset, limit);
      int responseRecordsCount = 0;
      if (records != null) {
        responseRecordsCount = records.size();
      }

      LOGGER.info("Response records: " + responseRecordsCount);
      if (responseRecordsCount != 0) {
        File output = new File(singleHarvestOutputDirectory, "request." + offset + "-" +
            (responseRecordsCount < limit ? (offset + responseRecordsCount) : (offset + limit))
            + ".txt");
        String jsonString = JSONArray.toJSONString(records);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(jsonString);
        String prettyJsonString = gson.toJson(je);
        FileUtils.writeStringToFile(output, prettyJsonString);
      }
      offset += limit;
      recordsCounter += responseRecordsCount;
      LOGGER.info("Records harvested until now: " + recordsCounter);
    } while (records != null && records.size() != 0);
    LOGGER.info("Harvest finished with total records harvested: " + recordsCounter);
  }

  private List<String> getNextResult(int offset, int limit)
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

    JSONObject json = (JSONObject) new JSONParser().parse(response.toString());
    List<String> records = (List) json.get(recordListField);
    return records;
  }

}
