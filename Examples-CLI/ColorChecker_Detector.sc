import ij_plugins.javacv.mcc.Utils.*
import ij_plugins.javacv.util.OpenCVUtils.*
import org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR
import org.bytedeco.opencv.global.opencv_mcc.*
import org.bytedeco.opencv.opencv_core.*
import org.bytedeco.opencv.opencv_mcc.*

import java.io.File

// We force loading as color image (IMREAD_COLOR), as ColorChecker Detector expects only color images
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
