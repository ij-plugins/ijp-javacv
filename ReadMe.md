IJP-JavaCV
==========

[![Scala CI](https://github.com/ij-plugins/ijp-javacv/actions/workflows/scala.yml/badge.svg)](https://github.com/ij-plugins/ijp-javacv/actions/workflows/scala.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.sf.ij-plugins/ijp-javacv-core_3/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.sf.ij-plugins/ijp-javacv-core_3)
[![Scaladoc](https://javadoc.io/badge2/net.sf.ij-plugins/ijp-javacv-core_3/scaladoc.svg)](https://javadoc.io/doc/net.sf.ij-plugins/ijp-javacv-core_3)

IJP-JavaCV intends to help in working with [OpenCV] and [JavaCV] from JVM languages, like Scala. It is focused on
working with [ImageJ], but can be used without it.

Core functionality
------------------

* Utilities for loading and displaying images using JavaCV
* Helper for converting between OpenCV and Scala/Java constructs
* API for conversion between ImageJ and OpenCV types

ImageJ Scription
----------------

All of [OpenCV]/[JavaCV] can be accessed from ImageJ through scripting. You can use any ImageJ scripting
language, including Scala, JavaScript, Python (Jython). Intention of IJP-JavaCV is to make that scripting less verbose,
proving utilities to make interaction with [OpenCV] API simpler.

### Examples

Sample Scala scripts can be found in [Examples-ImageJ].

Here is an example of a ImageJ's Scala script that is using [CCheckerDetector] (ColorChecker calibration chart
detector):

```scala
import ij.IJ
import ij_plugins.javacv.IJOpenCVConverters.toMat
import ij_plugins.javacv.mcc.Utils.*
import org.bytedeco.opencv.global.opencv_mcc.*
import org.bytedeco.opencv.opencv_mcc.*

// Load image
val imp = IJ.openImage("data/IMG_0903_025p-crop.png")
if (imp != null) {
  imp.show()
  // ColorChecker detector requires RGB image as input
  val cp       = toColorProcessor(imp)
  // Convert RGB image to OpenCV representation
  val mat      = toMat(cp)
  // Create chart detector
  val detector = CCheckerDetector.create()
  // Detect chart and display chart outline as ROI
  if (detector.process(mat, MCC24)) {
    val checker = detector.getBestColorChecker
    imp.setRoi(toPolygonRoi(checker.getBox))
  } else
    IJ.showMessage("ColorChecker not detected")
} else
  IJ.noImage()
```

### Additional Examples

Many examples of using [OpenCV] and [JavaCV] can be found
in [OpenCV Cookbook Examples](https://github.com/bytedeco/javacv-examples/tree/master/OpenCV_Cookbook) project.

Use without ImageJ
------------------

The core functionality can be used as a library without ImageJ. You will need to add dependency on "net.sf.ij-plugins:
ijp-javacv-core_3". For instance, for SBT:

```scala
libraryDependencies += "net.sf.ij-plugins" % "ijp-javacv-core_3" % version
```

Sample Scala scripts can be found in [Examples-CLI]. Here is an example of a ImageJ's Scala script that is
using [CCheckerDetector] (ColorChecker calibration chart detector):

```scala
import ij_plugins.javacv.mcc.Utils.*
import ij_plugins.javacv.util.OpenCVUtils.*
import org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR
import org.bytedeco.opencv.global.opencv_mcc.*
import org.bytedeco.opencv.opencv_core.*
import org.bytedeco.opencv.opencv_mcc.*

import java.io.File

// We force loading as color image (IMREAD_COLOR), 
// as ColorChecker Detector expects only color images
val mat = loadAndShow(new File("data/IMG_0903_025p-crop.png"), IMREAD_COLOR)

// Create detector
val detector = CCheckerDetector.create()

// Detect chart and display tiles, if found
if (detector.process(mat, MCC24)) {
  // Get detected chart
  val checker = detector.getBestColorChecker

  // Draw detected tiles
  CCheckerDraw
    .create(checker, scalar(255, 255, 255), 1)
    .draw(mat)

  show(mat, "Detected ColorChecker tiles")
} else
  println("ColorChecker not detected")
```

ImageJ Plugins
--------------
Some of the functionality of [OpenCV]/[JavaCV] is exposed through ImageJ plugins

* ColorChecker Detector
* Hough Circles
* Interactive segmentation using GrabCut algorithm

More information about plugins is in the [Wiki]

### Installation

Plugin binaries are provided on the [Releases] page


[ImageJ]: http://imagej.net/index.html

[JavaCV]: https://github.com/bytedeco/javacv

[OpenCV]: http://opencv.org/

[Examples-ImageJ]: Examples-ImageJ

[Releases]: https://github.com/ij-plugins/ijp-javacv/releases

[Wiki]: https://github.com/ij-plugins/ijp-javacv/wiki

[CCheckerDetector]: https://docs.opencv.org/4.7.0/d9/d53/classcv_1_1mcc_1_1CCheckerDetector.html
