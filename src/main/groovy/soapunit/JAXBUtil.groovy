package soapunit

import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.stream.XMLInputFactory
import javax.xml.transform.stream.StreamSource

/**
 * @author mohammo on 10/8/2014.
 */
class JAXBUtil {
    /**
     * Parses the XML file given and converts into a JAXB Object
     * @param xmlFile
     * @param stubClass should be a JAXB class annotated with @XmlRootElement
     * @return
     * @throws Exception
     */
    static def xmlFileToJAXBObject(File xmlFile, Class stubClass) throws Exception {
        //If the input stub class is not JAXB class (annotated with @XmlRootElement), throw exception
        if (!isJAXBStubClass(stubClass)) {
            throw new  RuntimeException("Invalid JAXB stub ${stubClass} passed. Class expected to have annotation XmlRootElement ")
        }

        def tagName = getRootElement(stubClass)
        def reader = XMLInputFactory.newFactory().createXMLStreamReader(new StreamSource(xmlFile))

        try {
            reader.nextTag(); // Advance to Envelope tag
            while (!reader.getLocalName().equals(tagName)) {
                reader.nextTag()
            }

            JAXBElement jaxbElement = JAXBContext.newInstance(stubClass).createUnmarshaller().unmarshal(reader, stubClass)
            jaxbElement.getValue();
        } catch (Exception e) {
            println "Error reading file: ${xmlFile}"
            throw new RuntimeException("Error reading file: " + xmlFile, e)
        } finally {
            reader.close()
        }
    }

    static def throwIfNotJAXBStubClass(Class stubClass) {
        if (!isJAXBStubClass(stubClass)) throw new  RuntimeException("Invalid JAXB stub ${stubClass} passed. Class expected to have annotation XmlRootElement ")
    }

    static def isJAXBStubClass(Class stubClass) {
        stubClass.isAnnotationPresent(XmlRootElement)
    }

    static def getRootElement(Class stubClass) {
        stubClass.getAnnotation(XmlRootElement).name()
    }
}
