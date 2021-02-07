package net.sf.ijplugins.javacv

import ij.process.{ByteProcessor, ColorProcessor}
import ij.{IJ, ImagePlus}
import net.sf.ijplugins.javacv.util.OpenCVUtils.loadMulti
import org.bytedeco.javacv.{Frame, OpenCVFrameConverter}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

import java.io.File

class ImagePlusFrameConverterTest extends AnyFlatSpec {

  it should "convert GRAY8 to Frame" in {

    val bp = new ByteProcessor(13, 17)
    val imp = new ImagePlus("", bp)

    val converter = new ImagePlusFrameConverter()

    val frame = converter.convert(imp)

    frame.imageDepth should be(Frame.DEPTH_UBYTE)
    frame.imageWidth should be(13)
    frame.imageHeight should be(17)
    frame.imageChannels should be(1)
  }


  it should "convert RGB to Frame" in {

    val cp = new ColorProcessor(13, 17)
    val imp = new ImagePlus("", cp)

    val converter = new ImagePlusFrameConverter()

    val frame = converter.convert(imp)

    frame.imageDepth should be(Frame.DEPTH_UBYTE)
    frame.imageWidth should be(13)
    frame.imageHeight should be(17)
    frame.imageChannels should be(3)
  }


  it should "convert Frame to GRAY8" in {

    val frame = new Frame(13, 17, Frame.DEPTH_UBYTE, 1)

    val converter = new ImagePlusFrameConverter()

    val imp = converter.convert(frame)

    imp.getType should be(ImagePlus.GRAY8)
    imp.getWidth should be(13)
    imp.getHeight should be(17)
    imp.getStackSize should be(1)
  }


  it should "convert Frame to GRAY8 Stack (from disk)" in {
    val srcFile = new File("../test/data/mri-stack_c.tif").getCanonicalFile
    assert(srcFile.exists())

    val srcMat = loadMulti(srcFile)
    srcMat should not be (null)

    val openCVConverter = new OpenCVFrameConverter.ToMat()
    val srcFrame = openCVConverter.convert(srcMat)
    srcFrame should not be (null)

    val converter = new ImagePlusFrameConverter()
    val dstImp = converter.convert(srcFrame)
    dstImp should not be (null)

    dstImp.getType should be(ImagePlus.GRAY8)
    dstImp.getWidth should be(186)
    dstImp.getHeight should be(226)
    dstImp.getStackSize should be(27)

    // Read using ImageJ so we can compare conversion
    val srcImp = IJ.openImage(srcFile.getCanonicalPath)
    // Sanity checks
    srcImp should not be (null)
    srcImp.getWidth should be(186)
    srcImp.getHeight should be(226)
    srcImp.getStackSize should be(27)

    val srcImageArray = srcImp.getStack.getImageArray
    val dstImageArray = dstImp.getStack.getImageArray
    for (i <- 0 until srcImp.getStackSize) {
      srcImageArray(i) should be(dstImageArray(i))
    }
  }
}
