/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package net.sf.ijplugins.javacv

import java.nio.{ByteBuffer, FloatBuffer, ShortBuffer}

import ij.ImagePlus
import ij.process.ColorProcessor
import org.bytedeco.javacv.{Frame, FrameConverter}

/**
 * Converts between ImagePlus and Frame.
 */
class ImagePlusFrameConverter extends FrameConverter[ImagePlus] {

  override def convert(imp: ImagePlus): Frame = toFrame(imp)

  override def convert(frame: Frame): ImagePlus = toImagePlus(frame)

  private def toFrame(imp: ImagePlus): Frame = {
    if (imp == null) {
      return null
    }

    val (imageDepth, channels) = imp.getType match {
      case ImagePlus.GRAY8 => (Frame.DEPTH_UBYTE, 1)
      case ImagePlus.GRAY16 => (Frame.DEPTH_USHORT, 1)
      case ImagePlus.GRAY32 => (Frame.DEPTH_FLOAT, 1)
      case ImagePlus.COLOR_256 => (Frame.DEPTH_UBYTE, 1)
      case ImagePlus.COLOR_RGB => (Frame.DEPTH_UBYTE, 3)
      case _ => ???
    }

    val width = imp.getWidth
    val height = imp.getHeight

    if (frame == null ||
      frame.imageWidth != width ||
      frame.imageHeight != imp.getHeight ||
      frame.imageChannels != channels) {
      // Reallocate frame, its type or size changed
      frame = new Frame(width, height, imageDepth, channels)
    }

    // Copy pixels
    // assume matching strides
    val buffer = frame.image(0).position(0)

    val ip = imp.getProcessor
    imp.getType match {
      case ImagePlus.GRAY8 | ImagePlus.COLOR_256 =>
        val b = buffer.asInstanceOf[ByteBuffer]
        val ipPixels = ip.getPixels.asInstanceOf[Array[Byte]]
        copyPixels(ipPixels, b, width, height, frame.imageStride, 0)
      case ImagePlus.GRAY16 =>
        val b = buffer.asInstanceOf[ShortBuffer]
        val ipPixels = ip.getPixels.asInstanceOf[Array[Short]]
        val rowPixels = new Array[Short](width)
        b.position(0)
        for (i <- 0 until height) {
          b.position(i * frame.imageStride)
          System.arraycopy(ipPixels, i * width, rowPixels, 0, rowPixels.length)
          b.put(rowPixels)
        }
      case ImagePlus.GRAY32 =>
        val b = buffer.asInstanceOf[FloatBuffer]
        val ipPixels = ip.getPixels.asInstanceOf[Array[Float]]
        val rowPixels = new Array[Float](width)
        b.position(0)
        for (i <- 0 until height) {
          b.position(i * frame.imageStride)
          System.arraycopy(ipPixels, i * width, rowPixels, 0, rowPixels.length)
          b.put(rowPixels)
        }
      case ImagePlus.COLOR_RGB =>
        val b = buffer.asInstanceOf[ByteBuffer]
        val cp = ip.asInstanceOf[ColorProcessor]
        val pixels = cp.getPixels.asInstanceOf[Array[Int]]
        for (y <- 0 until height) {
          val rowPixels = new Array[Byte](width * channels)
          val yOffset = y * width
          for (x <- 0 until width) {
            val c: Int = pixels(yOffset + x)
            //            val a = ((c & 0xff000000) >> 24).toByte
            val r = ((c & 0xff0000) >> 16).toByte
            val g = ((c & 0xff00) >> 8).toByte
            val b = (c & 0xff).toByte
            rowPixels(x * channels) = b
            rowPixels(x * channels + 1) = g
            rowPixels(x * channels + 2) = r
            //            rowPixels(x * channels + 3) = a
          }
          b.position(y * frame.imageStride)
          b.put(rowPixels)
        }
      case _ => ???
    }

    frame
  }


  def copyPixels(srcPixels: Array[Byte], destBuffer: ByteBuffer,
                 width: Int, height: Int, destStride: Int, destOffset: Int = 0) = {
    val rowPixels = new Array[Byte](width)
    destBuffer.position(destOffset)
    for (i <- 0 until height) {
      destBuffer.position(i * frame.imageStride)
      System.arraycopy(srcPixels, i * width, rowPixels, 0, rowPixels.length)
      destBuffer.put(rowPixels)
    }

  }

  private def toImagePlus(frame: Frame): ImagePlus = {
    if (frame == null || frame.image == null) {
      return null
    }

    var alpha: Boolean = false
    var offsets: Array[Int] = null
    frame.imageChannels match {
      case 1 =>
        alpha = false
        offsets = Array[Int](0)
      case 3 =>
        alpha = false
        offsets = Array[Int](2, 1, 0)
      case 4 =>
        alpha = true
        offsets = Array[Int](0, 1, 2, 3)
      case _ =>
        throw new RuntimeException("Unsupported `frame.imageChannels` = " + frame.imageChannels)
    }

    //    var cm: ColorModel = null
    //    var wr: WritableRaster = null
    //    if (frame.imageDepth == Frame.DEPTH_UBYTE || frame.imageDepth == Frame.DEPTH_BYTE) {
    //      cm = new ComponentColorModel(cs, alpha, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE)
    //      wr = Raster.createWritableRaster(new ComponentSampleModel(DataBuffer.TYPE_BYTE, frame.imageWidth, frame.imageHeight, frame.imageChannels, frame.imageStride, offsets), null)
    //    }
    //    else if (frame.imageDepth == Frame.DEPTH_USHORT) {
    //      cm = new ComponentColorModel(cs, alpha, false, Transparency.OPAQUE, DataBuffer.TYPE_USHORT)
    //      wr = Raster.createWritableRaster(new ComponentSampleModel(DataBuffer.TYPE_USHORT, frame.imageWidth, frame.imageHeight, frame.imageChannels, frame.imageStride, offsets), null)
    //    }
    //    else if (frame.imageDepth == Frame.DEPTH_SHORT) {
    //      cm = new ComponentColorModel(cs, alpha, false, Transparency.OPAQUE, DataBuffer.TYPE_SHORT)
    //      wr = Raster.createWritableRaster(new ComponentSampleModel(DataBuffer.TYPE_SHORT, frame.imageWidth, frame.imageHeight, frame.imageChannels, frame.imageStride, offsets), null)
    //    }
    //    else if (frame.imageDepth == Frame.DEPTH_INT) {
    //      cm = new ComponentColorModel(cs, alpha, false, Transparency.OPAQUE, DataBuffer.TYPE_INT)
    //      wr = Raster.createWritableRaster(new ComponentSampleModel(DataBuffer.TYPE_INT, frame.imageWidth, frame.imageHeight, frame.imageChannels, frame.imageStride, offsets), null)
    //    }
    //    else if (frame.imageDepth == Frame.DEPTH_FLOAT) {
    //      cm = new ComponentColorModel(cs, alpha, false, Transparency.OPAQUE, DataBuffer.TYPE_FLOAT)
    //      wr = Raster.createWritableRaster(new ComponentSampleModel(DataBuffer.TYPE_FLOAT, frame.imageWidth, frame.imageHeight, frame.imageChannels, frame.imageStride, offsets), null)
    //    }
    //    else if (frame.imageDepth == Frame.DEPTH_DOUBLE) {
    //      cm = new ComponentColorModel(cs, alpha, false, Transparency.OPAQUE, DataBuffer.TYPE_DOUBLE)
    //      wr = Raster.createWritableRaster(new ComponentSampleModel(DataBuffer.TYPE_DOUBLE, frame.imageWidth, frame.imageHeight, frame.imageChannels, frame.imageStride, offsets), null)
    //    }
    //    else {
    //      assert(false)
    //    }
    //
    //    bufferedImage = new BufferedImage(cm, wr, false, null)

    ???
  }
}
