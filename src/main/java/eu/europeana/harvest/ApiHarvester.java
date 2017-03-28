package eu.europeana.harvest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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
  private File rootHarvestOutputDirectory;
  private boolean jsonConvertToXml;

  public ApiHarvester(String apiEndpoint, String directoryNamePrefix, boolean jsonConvertToXml, String recordListField,
      String offsetParameterName, int offset, String limitParameterName, int limit,
      String harvestOutputDirectory) throws IOException {
    this.apiEndpoint = apiEndpoint;
    this.offsetParameterName = offsetParameterName;
    this.offset = offset;
    this.recordListField = recordListField;
    this.limitParameterName = limitParameterName;
    this.limit = limit;
    this.jsonConvertToXml = jsonConvertToXml;

    DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HHmm");
    Date date = new Date();

    this.rootHarvestOutputDirectory = new File(harvestOutputDirectory,
        directoryNamePrefix + "-" + dateFormat.format(date));
    if (this.rootHarvestOutputDirectory.exists()) {
      FileUtils.deleteDirectory(rootHarvestOutputDirectory);
    }
    this.rootHarvestOutputDirectory.mkdir();
  }

  public void harvest(boolean parallel, boolean singleDirectory)
      throws IOException, URISyntaxException, TransformerException, ParserConfigurationException, SAXException {
    LOGGER.info("Harvesting is starting with directory: " + rootHarvestOutputDirectory);

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
//    DirectoryController directoryController = new DirectoryController(10, rootHarvestOutputDirectory);
//    File fileToWriteOnDirectoryStructure = directoryController
//        .getFileToWriteOnDirectoryStructure(offset, limit);
//
//
//  }

  private void harvestIterativeSingleDirectory()
      throws URISyntaxException, IOException, TransformerException, ParserConfigurationException, SAXException {
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
        recordResult = getStringRepresentation(resultFormat, jsonConvertToXml, response);

        LOGGER.info("Response records: " + responseRecordsCount);
        if (responseRecordsCount != 0) {
          File output = new File(rootHarvestOutputDirectory, "request." + offset + "-" +
              (responseRecordsCount < limit ? (offset + responseRecordsCount) : (offset + limit))
              + ((resultFormat == resultFormat.JSON && jsonConvertToXml == false)?".json":".xml"));
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
      throws URISyntaxException, IOException {
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

    String responseString = response.toString().trim();
    return responseString.equals("") ? null : responseString;
  }


  private ResultFormat identifyResultFormat(String result) {
    if (result.trim().startsWith("<")) {
      return ResultFormat.XML;
    } else {
      return ResultFormat.JSON;
    }
  }

  private String getStringRepresentation(ResultFormat resultFormat, boolean jsonConvertToXml, String result)
      throws TransformerException {
    String prettyRecords = null;
    switch (resultFormat) {
      case JSON:
        if (jsonConvertToXml) {
          JSONObject json = new JSONObject(result);
          JSONArray jsonArray = json.getJSONArray(recordListField);
          String xmlString = XML.toString(jsonArray, "record");
          xmlString = "<root>" + xmlString + "</root>";

          Transformer transformer = TransformerFactory.newInstance().newTransformer();
          transformer.setOutputProperty(OutputKeys.INDENT, "yes");
          transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
          StreamResult resultxml = new StreamResult(new StringWriter());
          DOMSource source = new DOMSource(parseXmlFile(xmlString));
          transformer.transform(source, resultxml);
          prettyRecords = resultxml.getWriter().toString();
        }
        else {
          JSONObject json = new JSONObject(result);
          JSONArray jsonArray = json.getJSONArray(recordListField);
          prettyRecords = jsonArray.toString(4);
        }

        break;
      case XML:
        StringWriter sw = new StringWriter();
        Document document = parseXmlFile(result);
        Node node = document.getElementsByTagName(recordListField).item(0);
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.transform(new DOMSource(node), new StreamResult(sw));
        prettyRecords = sw.toString();
        break;
    }
    return prettyRecords;
  }

  private int getTotalRecordsInResponse(ResultFormat resultFormat, String response)
      throws ParserConfigurationException, IOException, SAXException {
    switch (resultFormat) {
      case JSON:
        JSONObject json = new JSONObject(response);
        JSONArray jsonArray = (JSONArray) json.get(recordListField);
        return jsonArray.length();
      case XML:
        Document document = parseXmlFile(response);
        return document.getElementsByTagName(recordListField).getLength();
    }
    return 0;
  }

  private enum ResultFormat {
    JSON, XML
  }

  private Document parseXmlFile(String in) {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      InputSource is = new InputSource(new StringReader(in));
      return db.parse(is);
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    } catch (SAXException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
