/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package net.sf.ijplugins.javacv

import java.nio.{ByteBuffer, FloatBuffer, ShortBuffer}

import ij.ImagePlus
import ij.process.{ColorProcessor, ImageProcessor}
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
      case ImagePlus.GRAY8 => (Frame.DEPTH_UBYTE, imp.getStackSize)
      case ImagePlus.GRAY16 => (Frame.DEPTH_USHORT, imp.getStackSize)
      case ImagePlus.GRAY32 => (Frame.DEPTH_FLOAT, imp.getStackSize)
      case ImagePlus.COLOR_256 => (Frame.DEPTH_UBYTE, imp.getStackSize)
      case ImagePlus.COLOR_RGB =>
        if (imp.getStackSize != 1) {
          throw new UnsupportedOperationException(s"RGB ImagePlus supported only with stack size == 1, " +
            s"got: ${imp.getStackSize}")
        } else {
          (Frame.DEPTH_UBYTE, 3)
        }
      case t =>
        throw new UnsupportedOperationException(s"Unsupported ImagePlus type: $t")
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

    val ips: Array[ImageProcessor] = {
      val stack = imp.getStack
      for (k <- (1 to stack.getSize).toArray) yield stack.getProcessor(k)
    }

    imp.getType match {

      case ImagePlus.GRAY8
           | ImagePlus.COLOR_256 => // Byte
        val srcPixels = ips.map(_.getPixels.asInstanceOf[Array[Byte]])
        val dstPixels = new Array[Byte](frame.imageStride * height)
        copyPixels(width, height, channels, frame.imageStride, srcPixels, dstPixels)

        val dstBuffer = buffer.asInstanceOf[ByteBuffer]
        dstBuffer.position(0)
        dstBuffer.put(dstPixels)

      case ImagePlus.GRAY16 => // Short
        val srcPixels = ips.map(_.getPixels.asInstanceOf[Array[Short]])
        val dstPixels = new Array[Short](frame.imageStride * height)
        copyPixels(width, height, channels, frame.imageStride, srcPixels, dstPixels)

        val dstBuffer = buffer.asInstanceOf[ShortBuffer]
        dstBuffer.position(0)
        dstBuffer.put(dstPixels)

      case ImagePlus.GRAY32 => // Float
        val srcPixels = ips.map(_.getPixels.asInstanceOf[Array[Float]])
        val dstPixels = new Array[Float](frame.imageStride * height)
        copyPixels(width, height, channels, frame.imageStride, srcPixels, dstPixels)

        val dstBuffer = buffer.asInstanceOf[FloatBuffer]
        dstBuffer.position(0)
        dstBuffer.put(dstPixels)

      case ImagePlus.COLOR_RGB =>
        val dstBuffer = buffer.asInstanceOf[ByteBuffer]
        // Only single slice color images are currently supported
        val cp = ips(0).asInstanceOf[ColorProcessor]
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
          dstBuffer.position(y * frame.imageStride)
          dstBuffer.put(rowPixels)
        }
      case t =>
        throw new UnsupportedOperationException(s"Unsupported ImagePlus type: $t")
    }

    frame
  }


  private def copyPixels[T](width: Int,
                            height: Int,
                            channels: Int,
                            imageStride: Int,
                            srcPixels: Array[Array[T]],
                            dstPixels: Array[T]): Unit = {
    for (i <- 0 until height) {
      val src_offset_i = i * width
      val dst_offset_i = i * imageStride
      if (channels == 1) {
        // We can use a bit of speedup using system copy
        System.arraycopy(srcPixels, src_offset_i, dstPixels, dst_offset_i, width)
      } else {
        for (j <- 0 until width) {
          val dst_offset_j = dst_offset_i + j * channels
          for (k <- 0 until channels) {
            dstPixels(dst_offset_j + k) = srcPixels(k)(src_offset_i + j)
          }
        }
      }
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
