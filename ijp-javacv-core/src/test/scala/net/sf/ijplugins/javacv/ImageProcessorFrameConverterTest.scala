package net.sf.ijplugins.javacv

import ij.process.{ByteProcessor, ColorProcessor}
import org.bytedeco.javacv.Frame
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

class ImageProcessorFrameConverterTest extends AnyFlatSpec {


  it should "convert GRAY8 to Frame" in {
    val bp = new ByteProcessor(13, 17)

    val converter = new ImageProcessorFrameConverter()

    val frame = converter.convert(bp)

    frame.imageDepth should be(Frame.DEPTH_UBYTE)
    frame.imageWidth should be(13)
    frame.imageHeight should be(17)
    frame.imageChannels should be(1)
  }

  it should "convert RGB to Frame" in {
    val cp = new ColorProcessor(13, 17)

    val converter = new ImageProcessorFrameConverter()

    val frame = converter.convert(cp)

    frame.imageDepth should be(Frame.DEPTH_UBYTE)
    frame.imageWidth should be(13)
    frame.imageHeight should be(17)
    frame.imageChannels should be(3)
  }

  //  it should "convert RGB to Frame from disk" in {
  //    val srcFile = new File("data/chariot.jpg").getCanonicalFile
  //    assert(srcFile.exists())
  //
  //    val srcImp = IJ.openImage(srcFile.getCanonicalPath)
  //    srcImp should not be (null)
  //
  //    val srcIP = srcImp.getProcessor
  //    // Crop to size that will force the stride be different than width
  //    srcIP.setRoi(new Rectangle(0, 0, 477, 357))
  //    val ip2 = srcIP.crop()
  //
  //    val ipConverter = new ImageProcessorFrameConverter()
  //
  //    val dstFrame = ipConverter.convert(ip2)
  //    dstFrame should not be (null)
  //    dstFrame.imageWidth should be(477)
  //    dstFrame.imageHeight should be(357)
  //
  //    val openCVConverter = new OpenCVFrameConverter.ToMat()
  //    val dstMat = openCVConverter.convert(dstFrame)
  //
  //    OpenCVUtils.save(new File("tmp_ImageProcessorFrameConverterTest.png"), dstMat)
  //  }

  //  it should "convert Frame to RGB from disk" in {
  //    val srcFile = new File("data/chariot.jpg").getCanonicalFile
  //    assert(srcFile.exists())
  //
  //
  //    val srcMat = OpenCVUtils.load(srcFile, IMREAD_COLOR)
  //    srcMat should not be (null)
  //
  //    val openCVConverter = new OpenCVFrameConverter.ToMat()
  //
  //    val srcFrame = openCVConverter.convert(srcMat)
  //    srcFrame should not be (null)
  //
  //    val ipConverter = new ImageProcessorFrameConverter()
  //    val dstIP = ipConverter.convert(srcFrame)
  //    dstIP should not be (null)
  //
  //    IJ.save(new ImagePlus("", dstIP), "tmp_ImageProcessorFrameConverterTest_from_frame.png")
  //  }


}
