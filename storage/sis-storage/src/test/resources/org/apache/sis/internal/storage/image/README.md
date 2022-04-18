This repository contains an image of size 128×64 pixels georefenced
as if it was covering the world from −180° to 180° of longitude and
from −90° to 90° of latitude. Pixel values in all bands vary from 0
inclusive to 256 exclusive with the following formulas:

   Band 0: x⋅2
   Band 1: y⋅4
   Band 2: y⋅2 + x

It makes easy to determine the location error when the wrong pixel
is read.
