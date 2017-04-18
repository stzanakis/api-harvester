package eu.europeana.harvest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2017-03-27
 */
public class Main {

  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
  private static final String propertiesPath = "harvester.properties";

  public static void main(String[] args)
      throws IOException, URISyntaxException, TransformerException, ParserConfigurationException, SAXException, ParseException {

    Options options = new Options();
    options.addOption("h", "help", false, "help output");
    options.addOption("c", "convert", false, "enable only convertion of input");
    options.addOption("d", "outputDirectoryPath", true, "output directory path");
    options.addOption("o", "outputFileName", true, "output filename");
    options.addOption("l", "listElementNames", true, "if json is list, set the element names");
    options.addOption("r", "rootElementName", true, "root element name");
    options.addOption("f", "filePath", true, "file to read from");

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    if (cmd.hasOption("h") || cmd.hasOption("help")) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("xml converter", options);
      return;
    }

    if (cmd.hasOption("c") || cmd.hasOption("convert")) {
      LOGGER.info("Json convert enabled");
      jsonConvert(cmd);
      LOGGER.info("Json conversion finished");
    } else {
      LOGGER.info("Harvesting enabled");
      apiHarverst();
      LOGGER.info("Harvest finished");
    }
  }

  private static void jsonConvert(CommandLine cmd) throws IOException, TransformerException {
    String outputDirectoryPath = null;
    String outputFileName = null;
    String filePath = null;
    String rootElementName = null;
    String listElementNames = null;
    if (cmd.hasOption("d"))
      outputDirectoryPath = cmd.getOptionValue("d");
    else if (cmd.hasOption("outputDirectoryPath"))
      outputDirectoryPath = cmd.getOptionValue("outputDirectoryPath");

    if (cmd.hasOption("o"))
      outputFileName = cmd.getOptionValue("o");
    else if (cmd.hasOption("outputFileName"))
      outputFileName = cmd.getOptionValue("outputFileName");

    if (cmd.hasOption("l"))
      listElementNames = cmd.getOptionValue("l");
    else if (cmd.hasOption("listElementNames"))
      listElementNames = cmd.getOptionValue("listElementNames");

    if (cmd.hasOption("r"))
      rootElementName = cmd.getOptionValue("r");
    else if (cmd.hasOption("rootElementName"))
      rootElementName = cmd.getOptionValue("rootElementName");

    if (cmd.hasOption("f"))
      filePath = cmd.getOptionValue("f");
    else if (cmd.hasOption("filePath"))
      filePath = cmd.getOptionValue("filePath");


    String json = new String(Files.readAllBytes(Paths.get(filePath)));
    String convertedXml = StringUtils.convertJsonToXml(json, listElementNames, rootElementName);

    if (outputDirectoryPath.lastIndexOf("/") != outputDirectoryPath.length() - 1) {
      outputDirectoryPath = outputFileName + "/";
    }
    PrintWriter out = new PrintWriter(outputDirectoryPath + outputFileName);
    out.println(convertedXml);
    out.close();
  }

  private static void apiHarverst()
      throws IOException, ParserConfigurationException, TransformerException, SAXException, URISyntaxException {
    LOGGER.info("Initialize Properties");
    File propertiesFile = new File(propertiesPath);
    Properties properties = new Properties();
    //Properties on the level of the jar or in the resources directory
    if (propertiesFile.exists()) {
      properties.load(new FileInputStream(propertiesPath));
    } else {
      properties.load(Main.class.getResourceAsStream("/" + propertiesPath));
    }
    String apiEndpoint = properties.getProperty("api.endpoint");
    String directoryNamePrefix = properties.getProperty("directory.name.prefix");
    boolean jsonConvertToXml = Boolean
        .parseBoolean(properties.getProperty("json.convert.to.xml"));
    String offsetParameterName = properties.getProperty("offset.parameter.name");
    String limitParameterName = properties.getProperty("limit.parameter.name");
    int offset = Integer.parseInt(properties.getProperty("offset"));
    int limit = Integer.parseInt(properties.getProperty("limit"));
    String recordListField = properties.getProperty("record.list.field");
    String harvestOutputDirectory = properties.getProperty("harvest.output.directory");
    String rootElementName = properties.getProperty("root.element.name");
    String listElementNames = properties.getProperty("list.element.names");

    ApiHarvester apiHarvester = new ApiHarvester(apiEndpoint, directoryNamePrefix,
        jsonConvertToXml, recordListField, offsetParameterName, offset, limitParameterName,
        limit, harvestOutputDirectory);
    if (jsonConvertToXml) {
      apiHarvester.setRootElementName(rootElementName);
      apiHarvester.setListElementNames(listElementNames);
    }
    LOGGER.info("Initiate Harvest");
    apiHarvester.harvest(false, true);
  }
}
