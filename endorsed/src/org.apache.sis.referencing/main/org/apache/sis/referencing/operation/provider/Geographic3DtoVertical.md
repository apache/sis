There is no "Geographic 3D to ellipsoidal height" operation because
such separation is illegal according ISO 19111. However, Apache SIS
may need to perform such separation anyway in some circumstances,
but it should be only in contexts where SIS can keep trace of other
dimensions in an "interpolation CRS".  This happen in the following
method:

```
CoordinateOperationFinder.createOperationStep(GeodeticCRS, VerticalCRS)
```

The above method does inline the work of what would have been a
"Geographic 3D to ellipsoidal height" operation if it existed.
The algorithm is the same as the one in `Geographic3Dto2D.java`:
just drop dimensions with a non-square matrix like below and don't
do unit conversion at this place (unit conversions are the job
of another method):

                    ┌            ┐
                    │ 0  0  1  0 │
                    │ 0  0  0  1 │
                    └            ┘
