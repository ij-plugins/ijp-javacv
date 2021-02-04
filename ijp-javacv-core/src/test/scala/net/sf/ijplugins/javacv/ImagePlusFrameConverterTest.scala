package net.sf.ijplugins.javacv

import ij.ImagePlus
import ij.process.{ByteProcessor, ColorProcessor}
import org.bytedeco.javacv.Frame
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

class ImagePlusFrameConverterTest extends AnyFlatSpec {

  it should "convert GRAY8 toFrame" in {

    val bp = new ByteProcessor(13, 17)
    val imp = new ImagePlus("", bp)

    val converter = new ImagePlusFrameConverter()

    val frame = converter.convert(imp)

    frame.imageDepth should be(Frame.DEPTH_UBYTE)
    frame.imageWidth should be(13)
    frame.imageHeight should be(17)
    frame.imageChannels should be(1)
  }


  it should "convert RGB toFrame" in {

    val cp = new ColorProcessor(13, 17)
    val imp = new ImagePlus("", cp)

    val converter = new ImagePlusFrameConverter()

    val frame = converter.convert(imp)

    frame.imageDepth should be(Frame.DEPTH_UBYTE)
    frame.imageWidth should be(13)
    frame.imageHeight should be(17)
    frame.imageChannels should be(3)
  }

}
