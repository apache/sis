There is no `AbstractParameterValue` class because we do not need it.
However if a public `AbstractParameterValue` class is provided in the
future, then we could simplify a little bit the JAXB annotations on
`CC_GeneralParameterValue.getElement()`. We don't do that for now
because this is a too minor convenience for growing the public API.
