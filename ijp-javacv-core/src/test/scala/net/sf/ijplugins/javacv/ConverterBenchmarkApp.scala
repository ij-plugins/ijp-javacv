package net.sf.ijplugins.javacv

import ij.process.ImageProcessor
import net.sf.ijplugins.javacv.IJOpenCVConverters.toImageProcessor
import net.sf.ijplugins.javacv.util.OpenCVUtils
import org.bytedeco.javacv.{Frame, Java2DFrameConverter, OpenCVFrameConverter}
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.opencv_core.{Mat, Scalar}


object ConverterBenchmarkApp extends App {

  val mat = new Mat(9000, 9000, CV_8UC3, new Scalar(10, 100, 150, 0));
  //    val mat = new Mat(9000, 9000, CV_8U, new Scalar(10));

  OpenCVUtils.printInfo(mat, "Mat to be converted to ImageProcessor")

  for (i <- 1 to 10) {
    val tb0 = System.currentTimeMillis()
    val ipB = convertUsingJava2DFrame(mat)
    val tb1 = System.currentTimeMillis()
    assert(ipB != null)

    val ti0 = System.currentTimeMillis()
    val ipI = convertUsingIJFrame(mat)
    val ti1 = System.currentTimeMillis()
    assert(ipI != null)

    val tf0 = System.currentTimeMillis()
    val frame = covertToFrame(mat)
    val tf1 = System.currentTimeMillis()
    assert(frame != null)

    println(s"Conversion time: through BufferedImage: ${tb1 - tb0}ms, direct: ${ti1 - ti0}ms, just frame ${tf1 - tf0}ms")
  }


  def covertToFrame(mat: Mat): Frame = {
    val converter = new OpenCVFrameConverter.ToMat()
    converter.convert(mat)
  }

  def convertUsingJava2DFrame(mat: Mat): ImageProcessor = {
    val converter = new OpenCVFrameConverter.ToMat()
    val frame = converter.convert(mat)
    val bi = new Java2DFrameConverter().convert(frame)
    toImageProcessor(bi)
  }

  def convertUsingIJFrame(mat: Mat): ImageProcessor = {
    val converter = new OpenCVFrameConverter.ToMat()
    val frame = converter.convert(mat)
    new ImageProcessorFrameConverter().convert(frame)
  }

}
