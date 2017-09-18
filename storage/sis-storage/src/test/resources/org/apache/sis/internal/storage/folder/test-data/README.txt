This directory contains data to be read by the folder data store.
We need to put those data in a different directory than the Store
implementation class because otherwise, opening the folder would
scan all *.class files in addition to test files. Implementation
should be robust to that, but we nevertheless keep those files
separated for more predictable tests.
