package eu.europeana.harvest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2017-03-27
 */
public class Main
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final String propertiesPath = "harvester.properties";
    public static void main( String[] args )
        throws IOException, URISyntaxException, TransformerException, ParserConfigurationException, SAXException {
        LOGGER.info("Initialize Properties");
        File propertiesFile = new File(propertiesPath);
        Properties properties = new Properties();
        //Properties on the level of the jar or in the resources directory
        if(propertiesFile.exists())
            properties.load(new FileInputStream(propertiesPath));
        else
            properties.load(Main.class.getResourceAsStream("/" + propertiesPath));
        String apiEndpoint = properties.getProperty("api.endpoint");
        String directoryNamePrefix = properties.getProperty("directory.name.prefix");
        boolean jsonConvertToXml = Boolean.parseBoolean(properties.getProperty("json.convert.to.xml"));
        String offsetParameterName = properties.getProperty("offset.parameter.name");
        String limitParameterName = properties.getProperty("limit.parameter.name");
        int offset = Integer.parseInt(properties.getProperty("offset"));
        int limit = Integer.parseInt(properties.getProperty("limit"));
        String recordListField = properties.getProperty("record.list.field");
        String harvestOutputDirectory = properties.getProperty("harvest.output.directory");
        String rootElementName = properties.getProperty("root.element.name");

        ApiHarvester apiHarvester = new ApiHarvester(apiEndpoint, directoryNamePrefix, jsonConvertToXml, recordListField, offsetParameterName, offset, limitParameterName, limit, harvestOutputDirectory);
        if (jsonConvertToXml)
            apiHarvester.setRootElementName(rootElementName);
        LOGGER.info("Initiate Harvest");
        apiHarvester.harvest(false, true);
    }
}
