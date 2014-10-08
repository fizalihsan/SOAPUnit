package soapunit

class ComparisonException extends Exception{
    def mismatchAt
    def rootCause
}
