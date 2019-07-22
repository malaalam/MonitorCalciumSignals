# MonitorCalciumSignals
An ImageJ plugin to image Calcium (Ca2+) activity in worms

## An example of volumetric images acquired from camera-2 (CM02) and camera-3 (CM03) are shown. 

<p float="center"><figure>
<img src="https://github.com/MLbyML/MLbyML.github.io/blob/master/images/2019-07-15/CM02_390.gif" width= "300" />
<img src="https://github.com/MLbyML/MLbyML.github.io/blob/master/images/2019-07-15/CM03_390.gif" width ="300"/>
<figcaption>
[Figure One : From left to right (a) Volumetric image of the embryo acquired using camera CM02. The first half of the z-slices are in focus (b) Volumetric image of the same embryo acquired using camera CM03. The second half of the z-slices are in focus]</figcaption></figure>
</p>

## Image from camera-2 (CM02) is registered to the image from camera (CM03)

<p float="center"><figure>
<img src="https://github.com/MLbyML/MLbyML.github.io/blob/master/images/2019-07-15/CM02_390_flipped.gif" width= "300" />
<img src="https://github.com/MLbyML/MLbyML.github.io/blob/master/images/2019-07-15/CM03_390.gif" width ="300"/>
<figcaption>
[Figure Two : From left to right (a) Volumetric image of the embryo acquired using camera CM02, flipped about the y-axis and translated (b) Volumetric image of the same embryo acquired using camera CM03]</figcaption></figure>
</p>

## "Care" is used to denoise the second half of registered images from CM02 and the first half of images from CM03

<p float="center"><figure>
<img src="https://github.com/MLbyML/MLbyML.github.io/blob/master/images/2019-07-15/CM02_390_flipped.gif" width= "300" />
<img src="https://github.com/MLbyML/MLbyML.github.io/blob/master/images/2019-07-15/CM02_390_flipped_denoised.gif" width ="300"/>
<figcaption>
[Figure Three : From left to right (a) Volumetric image of the embryo acquired using camera CM02, flipped about the y-axis and translated (b) Flipped image from CM02, denoised with Care]</figcaption></figure>
</p>

<p float="center"><figure>
<img src="https://github.com/MLbyML/MLbyML.github.io/blob/master/images/2019-07-15/CM03_390.gif" width= "300" />
<img src="https://github.com/MLbyML/MLbyML.github.io/blob/master/images/2019-07-15/CM03_390_denoised.gif" width ="300"/>
<figcaption>
[Figure Four : From left to right (a) Volumetric image of the embryo acquired using camera CM03 (b) Image from CM03, denoised with Care. Some artifacts are visible in the first few z-slices of the image]</figcaption></figure>
</p>

<hr>

Artifacts are visible in the denoising. Perhaps, a denoising strategy should be used in conjuction with a deblurring/deconvolution strategy for better results?
