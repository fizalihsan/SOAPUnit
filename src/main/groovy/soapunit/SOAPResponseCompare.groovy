package soapunit

import javax.xml.datatype.XMLGregorianCalendar
/**
 * @author mohammo on 10/8/2014.
 */
class SOAPResponseCompare {
    // JAXB stub class
    private Class stubClass
    // Stores the tag/element being processed currently for debugging purposes
    private def final tagStack = new Stack()

    // Map of complex element names which are part of a collection and the sub-element name to sort by.
    // For example, if there is a collection of <Client> elements in both responses and if they are in different order,
    // before comparing it has to be sorted first by a sub-element of <Client> like <name>.
    private Map<String, String> collectionElementSortKeyMap = [:]

    private def validateFiles(File beforeFile, File afterFile) {
        throwIfNull beforeFile, "beforeFile cannot be null"
        throwIfNull afterFile, "afterFile cannot be null"
        throwIfTrue(!beforeFile.exists() || !beforeFile.isFile(), "beforeFile either not exists or not a file")
        throwIfTrue(!afterFile.exists() || !afterFile.isFile(), "afterFile is either not exists or not a file")
    }

    /**
     * Compare the given 2 XML files
     * @param beforeFile
     * @param afterFile
     * @return
     */
    def compareFiles(File beforeFile, File afterFile) {
        validateFiles(beforeFile, afterFile)
        println "Comparing files '${beforeFile}' & '${afterFile}'"

        def beforeObject = JAXBUtil.xmlFileToJAXBObject(beforeFile, stubClass)
        def afterObject = JAXBUtil.xmlFileToJAXBObject(afterFile, stubClass)

        compareJAXBObjects(beforeObject, afterObject)
    }

    /**
     * Compare the given 2 JAXB objects
     * @param beforeObject
     * @param afterObject
     * @return
     */
    def compareJAXBObjects(def beforeObject, def afterObject) {
        JAXBUtil.throwIfNotJAXBStubClass(stubClass)
        throwIfNull beforeObject, "beforeObject cannot be null"
        throwIfNull afterObject, "afterObject cannot be null"
        println "Comparing root element in stub objects "
        checkSortKeyMap()

        try {
            recursiveCompare beforeObject, afterObject
        } catch (Error e) {
            def differenceAt = StringBuilder.newInstance()
            for (Object element : tagStack) {
                differenceAt << element << " > "
            }
            println e.message
            throw new ComparisonException(mismatchAt: differenceAt, rootCause: e)
        }
    }

    /**
     * JAXB auto-generates sub-element names with first character in lower case. E.g., <WeatherID> becomes weatherID.
     * This method ensures the key and value provided by the client in the sort key map follows this convention.
     * Without this, the sorting won't work and will cause breaks.
     * @return
     */
    private def checkSortKeyMap(){
        if(!collectionElementSortKeyMap.isEmpty()){
            def newMap = new HashMap(collectionElementSortKeyMap.size())

            collectionElementSortKeyMap.each { key, value ->
                throwIfTrue !(key instanceof String), "collectionElementSortKeyMap key should be String"
                throwIfTrue !(value instanceof String), "collectionElementSortKeyMap value should be String"
                newMap[firstCharToLowerCase(key)] = firstCharToLowerCase(value)
            }

            collectionElementSortKeyMap = newMap
        }
    }

    /**
     * Recursively walks through the object graph to compare same field from each object. An assertion error is thrown
     * in one of the following cases:
     * 1. If one of the value is null, and the other is not
     * 2. If the value type is a collection, and their sizes don't match
     * 3. If the values are of simple type and their values don't match
     * @param before
     * @param after
     */
    private def recursiveCompare(final before, final after) {
        def map1 = before?.properties?.sort()
        def map2 = after?.properties?.sort()

        for (def field1 : map1.keySet()) {
            if (field1 == "class") continue
            def pre = map1[field1]
            def post = map2[field1]

            //pushing the current tag being processed for debugging purposes
            tagStack.push(field1)
            assert (before == null) ? after == null : after != null, "Null value found. "

            if (!isSimpleType(pre)) {
                if (!pre) {
                    tagStack.pop()
                    continue
                }
                recursiveCompare pre, post
            } else if (pre instanceof Collection) {
                assert pre.size() == post.size(), "Collection size mismatch at <$field1> in $before - Pre = ${pre.size()}, Post = ${post.size()}"

                def sortKey = collectionElementSortKeyMap[field1]
                if (sortKey) {
                    pre = sort pre, sortKey
                    post = sort post, sortKey
                } else {
                    pre = pre.sort()
                    post = post.sort()
                }
                for (int i = 0; i < pre.size(); i++) {
                    tagStack.push(i + 1)
                    recursiveCompare pre[i], post[i]
                    tagStack.pop()
                }
            } else {
                assert pre == post, "Mismatch found in field '$field1'. "
            }
            tagStack.pop()
        }
    }

    /**
     * Check if the given input type is a simple type, and not a complex type
     * @param type
     * @return
     */
    private static def isSimpleType(def type) {
        type instanceof String ||
                type instanceof Number ||
                type instanceof Boolean ||
                type instanceof Date ||
                type instanceof XMLGregorianCalendar ||
                type instanceof Collection ||
                type instanceof Enum
    }

    // ---------------------- Utility methods ----------------------

    /**
     * Generic implementation of Java Comparator
     */
    private static final def GENERIC_COMPARATOR = { property, pre, post ->
        throwIfTrue !pre?.hasProperty(property), "${pre} does not have property: $property"
        throwIfTrue !post?.hasProperty(property), "${post} does not have property: $property"
        throwIfTrue pre?.getClass() != post?.getClass(), "Pre & Post should be of same type. Pre = '${pre}', Post = '${post}'"

        def a1 = pre?.properties[property]
        def b1 = post?.properties[property]
        a1 == null ? (b1 == null ? 0 : Integer.MIN_VALUE) : (a1 == null ? Integer.MAX_VALUE : a1.compareTo(b1))
    }

    /**
     * Sort the collection on the given sortByField using Generic comparator
     * @param collection
     * @param sortByField
     * @return
     */
    private def static sort(def collection, def sortByField) { collection.sort GENERIC_COMPARATOR.curry(sortByField) }

    /**
     * Throw exception with the given error message if the input is null
     * @param input
     * @param errorMessage
     * @return
     */
    private def static throwIfNull(def input, def errorMessage) {
        throwIfTrue !input, errorMessage
    }

    /**
     * Throw exception with the given error message if the condition is true
     * @param condition
     * @param errorMessage
     */
    private def static throwIfTrue(boolean condition, def errorMessage) {
        if (condition) throw new RuntimeException(errorMessage)
    }

    /**
     * 1. If input is null, return null
     * 2. If input is "", return null
     * 3. If input is "a" or "A", return "a"
     * 4. If input is "ClientDetails" or "clientDetalis", return "clientDetails"
     * @param string
     * @return
     */
    private def static firstCharToLowerCase(String string){
        if (!string || string.isEmpty()) string
        else if (string.length() == 1) string.toLowerCase()
        else string[0]?.toLowerCase() + string?.substring(1)
    }
}
