import org.bytedeco.opencv.opencv_mcc.CCheckerDetector
import org.bytedeco.opencv.opencv_mcc.CCheckerDraw

import static ij_plugins.javacv.mcc.Utils.scalar
import static ij_plugins.javacv.util.OpenCVUtils.loadAndShow
import static ij_plugins.javacv.util.OpenCVUtils.show
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR
import static org.bytedeco.opencv.global.opencv_mcc.MCC24

// CCheckerDetector expects only color images
mat = loadAndShow(new File("../data/IMG_0903_025p-crop.png"), IMREAD_COLOR)

// Create detector
detector = CCheckerDetector.create()

// Detect chart and display tiles, if found
if (detector.process(mat, MCC24)) {
    // Get detected chart
    c = detector.getBestColorChecker()

    // Draw detected tiles
    CCheckerDraw
            .create(c, scalar(255, 255, 255), 1)
            .draw(mat)

    show(mat, "Detected ColorChecker tiles")
} else
    println("ColorChecker not detected")
