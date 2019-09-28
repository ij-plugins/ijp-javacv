/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package net.sf.ijplugins.javacv

import java.nio.{ByteBuffer, FloatBuffer, ShortBuffer}

import ij.process._
import org.bytedeco.javacv.{Frame, FrameConverter}

/**
 * * Converts between ImageProcessor and Frame.
 */
class ImageProcessorFrameConverter extends FrameConverter[ImageProcessor] {

  override def convert(ip: ImageProcessor): Frame = toFrame(ip)

  override def convert(frame: Frame): ImageProcessor = toImagePlus(frame)

  private def toFrame(ip: ImageProcessor): Frame = {
    if (ip == null) {
      return null
    }

    val (imageDepth, channels) = ip match {
      case _: ByteProcessor => (Frame.DEPTH_UBYTE, 1)
      case _: ShortProcessor => (Frame.DEPTH_USHORT, 1)
      case _: FloatProcessor => (Frame.DEPTH_FLOAT, 1)
      case _: ColorProcessor => (Frame.DEPTH_UBYTE, 1)
      case _ => ???
    }

    val width = ip.getWidth
    val height = ip.getHeight

    if (frame == null ||
      frame.imageWidth != width ||
      frame.imageHeight != height ||
      frame.imageChannels != channels) {
      // Reallocate frame, its type or size changed
      frame = new Frame(width, height, imageDepth, channels)
    }

    // Copy pixels
    // assume matching strides
    val buffer = frame.image(0).position(0)

    ip match {
      case _: ByteProcessor =>
        val b = buffer.asInstanceOf[ByteBuffer]
        val ipPixels = ip.getPixels.asInstanceOf[Array[Byte]]
        val rowPixels = new Array[Byte](width)
        b.position(0)
        for (i <- 0 until height) {
          b.position(i * frame.imageStride)
          System.arraycopy(ipPixels, i * width, rowPixels, 0, rowPixels.length)
          b.put(rowPixels)
        }
      case _: ShortProcessor =>
        val b = buffer.asInstanceOf[ShortBuffer]
        val ipPixels = ip.getPixels.asInstanceOf[Array[Short]]
        val rowPixels = new Array[Short](width)
        b.position(0)
        for (i <- 0 until height) {
          b.position(i * frame.imageStride)
          System.arraycopy(ipPixels, i * width, rowPixels, 0, rowPixels.length)
          b.put(rowPixels)
        }
      case _: FloatProcessor =>
        val b = buffer.asInstanceOf[FloatBuffer]
        val ipPixels = ip.getPixels.asInstanceOf[Array[Float]]
        val rowPixels = new Array[Float](width)
        b.position(0)
        for (i <- 0 until height) {
          b.position(i * frame.imageStride)
          System.arraycopy(ipPixels, i * width, rowPixels, 0, rowPixels.length)
          b.put(rowPixels)
        }
      case _: ColorProcessor =>
        ???
      case _ => ???
    }

    frame
  }

  private def toImagePlus(frame: Frame): ImageProcessor = {
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
