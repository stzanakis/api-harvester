package eu.europeana.harvest;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2017-04-18
 */
public class StringUtils {

  public static ResultFormat identifyResultFormat(String result) {
    if (result.trim().startsWith("<")) {
      return ResultFormat.XML;
    } else {
      return ResultFormat.JSON;
    }
  }

  public static String convertJsonToXml(String jsonString, String listElementNames, String rootElementName)
      throws TransformerException {
    JSONObject json;
    if (jsonString.trim().startsWith("[")) {
      JSONArray jsonArray = new JSONArray(jsonString);
      json = new JSONObject();
      json.put(listElementNames, jsonArray);
    }
    else
      json = new JSONObject(jsonString);
    String xmlString = XML.toString(json);
    if (rootElementName != null)
      xmlString = "<"+ rootElementName +">" + xmlString + "</"+rootElementName+">";

    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
    StreamResult resultxml = new StreamResult(new StringWriter());
    DOMSource source = new DOMSource(parseXmlFile(xmlString));
    transformer.transform(source, resultxml);
    String prettyXml = resultxml.getWriter().toString();

    return prettyXml;
  }

  public static Document parseXmlFile(String in) {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      InputSource is = new InputSource(new StringReader(in));
      return db.parse(is);
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new RuntimeException(e);
    }
  }

}
