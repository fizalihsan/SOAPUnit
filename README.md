SOAPUnit
========

##Overview
Groovy based utility program to compare SOAP web service responses. This comes very handy especially during automated unit testing and regression testing.

## Features

### Compares JAXB objects or XML files
It can compare either 2 JAXB objects or 2 XML files. If the input is an XML file, it converts it into a JAXB object. 

The input JAXB object is considered valid only if it is annotated with [@XmlRootElement](http://docs.oracle.com/javase/7/docs/api/javax/xml/bind/annotation/XmlRootElement.html). 

### Compares collection of elements in different order

If the responses have collection of elements that appear in different order, then a sorting order can be provided to compare regardless of the order of elements.

Consider the below response below. 

```xml
<GetWeatherInformationResult>
    <WeatherDescription>
       <WeatherID>1</WeatherID>
       <Description>Thunder Storms</Description>
       <PictureURL>http://ws.cdyne.com/WeatherWS/Images/thunderstorms.gif</PictureURL>
    </WeatherDescription>
    <WeatherDescription>
       <WeatherID>2</WeatherID>
       <Description>Partly Cloudy</Description>
       <PictureURL>http://ws.cdyne.com/WeatherWS/Images/partlycloudy.gif</PictureURL>
    </WeatherDescription>
    <WeatherDescription>
       <WeatherID>3</WeatherID>
       <Description>Mostly Cloudy</Description>
       <PictureURL>http://ws.cdyne.com/WeatherWS/Images/mostlycloudy.gif</PictureURL>
    </WeatherDescription>
<GetWeatherInformationResult>
```

In order to sort <weatherDescription> elements by <weatherID> and <Description> fields, try this:

```groovy
def collectionElementSortKeyMap = ["WeatherDescription": ["WeatherID", "Description"]]
def compare = new SOAPResponseCompare(stubClass: GetWeatherInformationResponse.class, collectionElementSortKeyMap: collectionElementSortKeyMap)
```

### Mismatch report

Any mismatch found is reported with the exact element name and values that are not matching.

For example, the below message says there is a difference in <description> under 5th <weatherDescription> element.

```
Mismatch found in field 'description'. . Expression: (pre == post). Values: pre = Rainy, post = Rain
Not identical - Mismatch found at : getWeatherInformationResult > weatherDescription > 5 > description > 
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
def compare = new SOAPResponseCompare(stubClass: stubClass, collectionElementSortKeyMap: collectionElementSortKeyMap)

//Example on comparing XML files
this.compare {
    compare.compareFiles(beforeFile, afterFile)
}

//Example on comparing JAXB objects
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
