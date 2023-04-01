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
  val cp = toColorProcessor(imp)

  // Convert RGB image to OpenCV representation
  val mat = toMat(cp)

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
