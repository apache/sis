# Implementation notes

## Choice of errors in linear regression method
Before to compute a localization grid, the `LocalizationGridBuilder` class
first computes an *affine transform* from grid indices to geospatial coordinates.
That affine transform is an approximation computed by linear regression using least-squares method.
In an equation of the form _y_ = _C₀_ + _C₁_ × _x_ where the _C₀_ and _C₁_ coefficients are determined by linear regression,
typical linear regression method assumes that _x_ values are exact and all errors are in _y_ values.
Applied to the `LocalizationGridBuilder` context, it means that linear regression method
assumes that grid indices are exact and all errors are in geospatial coordinates.

The assumption that all errors are in geospatial coordinates is reasonable if the linear regression is used directly.
But in `LocalizationGridBuilder` context, having the smallest errors on geospatial coordinates may not be so important
because those errors are corrected by the residual grids during *forward* transformations.
However, during *inverse* transformations, it may be useful that grid indices estimated by the linear regression
are as close as possible to the real grid indices in order to allow iterations to converge faster
(such iterations exist only in inverse operations, not in forward operations).
For that reason, `LocalizationGridBuilder` may want to minimize errors on grid indices instead of geospatial coordinates.
We could achieve this result by computing linear regression coefficients for an equation of the form
_x_ = _C₀_ + _C₁_ × _y_, then inverting that equation.

This approach was attempted in a previous version and gave encouraging results, but we nevertheless reverted that change.
One reason is that even if inverting the linear regression calculation allowed iterations to converge more often with curved
localization grids, it was not sufficient anyway: we still had too many cases where inverse transformations did not converge.
Since a more sophisticated iteration method is needed anyway, we can avoid the additional complexity introduced by application
of linear regression in reverse way. Some examples of additional complexities were `LinearTransformBuilder.correlation()`
getting a meaning different than what its Javadoc said (values for each source dimensions instead of target dimensions),
additional constraints brought by that approach (source coordinates must be a two-dimensional grid,
source and target dimensions must be equal in order to have a square matrix), _etc._
A more minor reason is that keeping the original approach (minimize errors on geospatial coordinates)
help to improve accuracy by resulting in storage of smaller values in `float[]` arrays of `ResidualGrid`.

For experimenting linear regressions in the reverse way, revert commit `c0944afb6e2cd77194ea71504ce6d74b651f72b7`.
