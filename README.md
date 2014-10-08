SOAPUnit
========

##Overview
Groovy based utility program to compare SOAP web service responses. This comes very handy especially during unit testing and regression testing.

Clients can compare either 2 JAXB objects or 2 XML files. If the input is an XML file, it automatically converts into a JAXB object. The input object is considered a JAXB object only if it is annotated with [@XmlRootElement](http://docs.oracle.com/javase/7/docs/api/javax/xml/bind/annotation/XmlRootElement.html). 

Mismatch found is reported as follows:

```
Mismatch found in field 'description'. . Expression: (pre == post). Values: pre = Rainy, post = Rain
Not identical - Mismatch found at : getWeatherInformationResult > weatherDescription > 5 > description > getWeatherInformationResult > weatherDescription > 5 > description > 
```

## Sample Usage

```groovy
import hello.wsdl.GetWeatherInformationResponse
import soapunit.ComparisonException
import soapunit.JAXBUtil
import soapunit.SOAPResponseCompare


def beforeFile = new File("C:\\SOAPUnit\\BEFORE_123.xml")
def afterFile = new File("C:\\SOAPUnit\\AFTER_123.xml")
def stubClass = GetWeatherInformationResponse.class
def collectionElementSortKeyMap = ["WeatherDescription": "WeatherID"]
def compare = new SOAPResponseCompare(stubClass: GetWeatherInformationResponse.class, collectionElementSortKeyMap: collectionElementSortKeyMap)

this.compare {
    compare.compareFiles(beforeFile, afterFile)
}

this.compare {
    def beforeObject = JAXBUtil.xmlFileToJAXBObject(beforeFile, stubClass)
    def afterObject = JAXBUtil.xmlFileToJAXBObject(afterFile, stubClass)
    compare.compareJAXBObjects(beforeObject, afterObject)
}


def compare(def closure) {
    try {
        closure.call()
        println "Identical"
    } catch (ComparisonException e) {
        println "Not identical - Mismatch found at : ${e.mismatchAt}"
        e.rootCause.printStackTrace()
    }
}
```
