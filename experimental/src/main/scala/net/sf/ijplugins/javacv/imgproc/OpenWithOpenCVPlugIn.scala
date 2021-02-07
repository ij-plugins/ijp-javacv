package net.sf.ijplugins.javacv.imgproc

import ij.io.OpenDialog
import ij.plugin.PlugIn
import net.sf.ijplugins.javacv.ImagePlusFrameConverter
import net.sf.ijplugins.javacv.util.OpenCVUtils
import org.bytedeco.javacv.OpenCVFrameConverter
import org.bytedeco.opencv.global.opencv_core.merge
import org.bytedeco.opencv.global.opencv_imgcodecs.{IMREAD_UNCHANGED, imreadmulti}
import org.bytedeco.opencv.opencv_core.{Mat, MatVector}

import java.io.{File, IOException}

class OpenWithOpenCVPlugIn extends PlugIn {
  override def run(arg: String): Unit = {
    val od = new OpenDialog("Open image with OpenCV codecs")

    val path = od.getPath

    val file = new File(path)

    // Read input image
    val matV = new MatVector()
    val ok = imreadmulti(file.getAbsolutePath, matV, IMREAD_UNCHANGED)
    if (!ok) {
      throw new IOException("Couldn't load image: " + file.getAbsolutePath)
    }

    val mat = new Mat()
    merge(matV, mat)

    OpenCVUtils.printInfo(mat)

    val frame = new OpenCVFrameConverter.ToMat().convert(mat)
    val imp = new ImagePlusFrameConverter().convert(frame)
    imp.setTitle(file.getName)
    imp.show()
  }
}
