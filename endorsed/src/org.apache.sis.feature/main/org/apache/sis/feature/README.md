# Design goals and benchmarks

A major design goal of `org.apache.sis.feature` is to reduce memory usage.
Consider a ShapeFile or a database table with millions of records.
Each record is represented by one `Feature` instance.
Sophisticated `DataStore` implementations will create and discard `Feature`
instances on the fly, but not all `DataStore` do that.
As a safety, Apache SIS tries to implement `Feature` in a way that allow applications
to scale higher before to die with an `OutOfMemoryError`.

A simple `Feature` implementation would use a `java.util.HashMap` as below:

```
class SimpleFeature {
    final Map<String,Object> attributes = new HashMap<>(8);
}
```

The above `SimpleFeature` does not supports explicitly multi-valued properties and metadata
about the properties (admittedly multi-values could be stored as `java.util.Collection`,
but this approach has implications on the way we ensure type safety).
A more complete but still straightforward implementation could be:

```
class ComplexFeature {
    final Map<String,Property> properties = new HashMap<>(8);
}
class Property {
    final List<String> values = new ArrayList<>(4);
}
```

A more sophisticated implementation would take advantage of our knowledge that all records in
a table have the same attribute names, and that the vast majority of attributes are singleton.
Apache SIS uses this knowledge, together with lazy instantiations of `Property`.
The above simple implementation has been compared with the Apache SIS one in a micro-benchmark
consisting of the following steps:

* Defines the following feature type:
  * `city`      : `String` (8 characters)
  * `latitude`  : `Float`
  * `longitude` : `Float`
* Launch the micro-benchmarks in Java with a fixed amount of memory.
  This micro-benchmarks used the following command line with Java 1.8.0_05 on MacOS X 10.7.5:
  `java -Xms100M -Xmx100M ` _command_
* Creates `Feature` instances of the above type and store them in a list of fixed size
  until we get `OutOfMemoryError`.


## Results and discussion
The benchmarks have been executed about 8 times for each implementation (_simple_ and _complex_ versus _SIS_).
Results of the simple feature implementation were very stable.
But results of the SIS implementation randomly fall in two modes, one twice faster than the other
(maybe depending on which optimizations have been chosen by the HotSpot compiler):

```
                 Count          Time (seconds)
Run              mean     σ     mean   σ
ComplexFeature:  194262 ± 2     21.8 ± 0.9
SimpleFeature:   319426 ± 4     22.5 ± 0.6
SIS (mode 1):    639156 ± 40    25.6 ± 0.4
SIS (mode 2):    642437 ± 7     12.1 ± 0.8
```

For the trivial `FeatureType` used in this benchmark, the Apache SIS implementation
can load twice more `Feature` instances than the `HashMap<String,Object>`-based
implementation before the application get an `OutOfMemoryError`.
We presume that this is caused by the `Map.Entry` instances
that `HashMap` must create internally for each attribute.
Compared to `ComplexFeature`, SIS allows 3.3 times more instances
while being functionally equivalent.

The speed comparisons are subject to more cautions,
in part because each run has created a different number of instances before the test stopped.
So even the slowest SIS case would be almost twice faster than `SimpleFeature`
because it created two times more instances in an equivalent amount of time.
However, this may be highly dependent on garbage collector activities (it has not been verified).
