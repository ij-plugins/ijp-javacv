/*
 * Image/J Plugins
 * Copyright (C) 2002-2021 Jarek Sacha
 * Author's email: jpsacha at gmail dot com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Latest release available at http://sourceforge.net/projects/ij-plugins/
 */

package net.sf.ij_plugins.javacv

import ij.process._
import org.bytedeco.javacv.{Frame, FrameConverter}

import java.nio.{ByteBuffer, FloatBuffer, ShortBuffer}

object ImageProcessorFrameConverter {

  private[javacv] def copyPixels(ips: Array[ImageProcessor], frame: Frame): Unit = {
    require(ips.nonEmpty)

    if (frame.image.length != 1) {
      throw new UnsupportedOperationException(s"Expecting frame.image.length == 1, got ${frame.image.length}")
    }

    ips.head match {
      case _: ByteProcessor =>
        val srcPixels = ips.map(_.getPixels.asInstanceOf[Array[Byte]])
        copyPixelsByte(srcPixels, frame)

      case _: ShortProcessor =>
        val srcPixels = ips.map(_.getPixels.asInstanceOf[Array[Short]])
        copyPixelsShort(srcPixels, frame)

      case _: FloatProcessor =>
        val srcPixels = ips.map(_.getPixels.asInstanceOf[Array[Float]])
        copyPixelsFloat(srcPixels, frame)

      case _: ColorProcessor =>
        // Only single slice color images are currently supported
        val srcPixels = ips.head.getPixels.asInstanceOf[Array[Int]]
        copyPixelsRGB(srcPixels, frame)

      case _ =>
        throw new UnsupportedOperationException(s"Unsupported ImageProcessor type: ${ips.head.getClass}")
    }
  }


  private[javacv] def copyPixelsByte(srcPixels: Array[Array[Byte]],
                                     dstFrame: Frame): Unit = {
    val dstBuffer = dstFrame.image(0).position(0).asInstanceOf[ByteBuffer]
    val dstPixels = new Array[Byte](dstFrame.imageStride * dstFrame.imageHeight)
    for (i <- 0 until dstFrame.imageHeight) {
      copyStride(i, dstFrame.imageWidth, dstFrame.imageChannels, dstFrame.imageStride, srcPixels, dstPixels)
    }
    //    dstBuffer.position(i * imageStride)
    dstBuffer.position(0)
    dstBuffer.put(dstPixels)
  }


  private[javacv] def copyPixelsByte(srcFrame: Frame): Array[Array[Byte]] = {

    val dstPixels = Array.ofDim[Byte](srcFrame.imageChannels, srcFrame.imageWidth * srcFrame.imageHeight)

    val srcPixels = new Array[Byte](srcFrame.imageStride * srcFrame.imageHeight)
    val srcBuffer = srcFrame.image(0).position(0).asInstanceOf[ByteBuffer]
    srcBuffer.get(srcPixels)
    for (y <- 0 until srcFrame.imageHeight) {
      copyFromStride(y, srcFrame.imageWidth, srcFrame.imageChannels, srcFrame.imageStride, srcPixels, dstPixels)
    }

    dstPixels
  }


  private[javacv] def copyPixelsShort(srcPixels: Array[Array[Short]],
                                      dstFrame: Frame): Unit = {
    val dstBuffer = dstFrame.image(0).position(0).asInstanceOf[ShortBuffer]
    val dstPixels = new Array[Short](dstFrame.imageStride * dstFrame.imageHeight)
    for (y <- 0 until dstFrame.imageHeight) {
      copyStride(y, dstFrame.imageWidth, dstFrame.imageChannels, dstFrame.imageStride, srcPixels, dstPixels)
    }
    //    dstBuffer.position(i * imageStride)
    dstBuffer.position(0)
    dstBuffer.put(dstPixels)
  }


  private[javacv] def copyPixelsShort(srcFrame: Frame): Array[Array[Short]] = {

    val dstPixels = Array.ofDim[Short](srcFrame.imageChannels, srcFrame.imageWidth * srcFrame.imageHeight)

    val srcPixels = new Array[Short](srcFrame.imageStride * srcFrame.imageHeight)
    val srcBuffer = srcFrame.image(0).position(0).asInstanceOf[ShortBuffer]
    srcBuffer.get(srcPixels)
    for (y <- 0 until srcFrame.imageHeight) {
      copyFromStride(y, srcFrame.imageWidth, srcFrame.imageChannels, srcFrame.imageStride, srcPixels, dstPixels)
    }

    dstPixels
  }


  private[javacv] def copyPixelsFloat(srcPixels: Array[Array[Float]],
                                      dstFrame: Frame): Unit = {
    val dstBuffer = dstFrame.image(0).position(0).asInstanceOf[FloatBuffer]
    val dstPixels = new Array[Float](dstFrame.imageStride * dstFrame.imageHeight)
    for (y <- 0 until dstFrame.imageHeight) {
      copyStride(y, dstFrame.imageWidth, dstFrame.imageChannels, dstFrame.imageStride, srcPixels, dstPixels)
    }
    //    dstBuffer.position(i * imageStride)
    dstBuffer.position(0)
    dstBuffer.put(dstPixels)
  }


  private[javacv] def copyPixelsFloat(srcFrame: Frame): Array[Array[Float]] = {

    val dstPixels = Array.ofDim[Float](srcFrame.imageChannels, srcFrame.imageWidth * srcFrame.imageHeight)

    val srcPixels = new Array[Float](srcFrame.imageStride * srcFrame.imageHeight)
    val srcBuffer = srcFrame.image(0).position(0).asInstanceOf[FloatBuffer]
    srcBuffer.get(srcPixels)
    for (y <- 0 until srcFrame.imageHeight) {
      copyFromStride(y, srcFrame.imageWidth, srcFrame.imageChannels, srcFrame.imageStride, srcPixels, dstPixels)
    }

    dstPixels
  }


  private def copyStride[T](y: Int,
                            width: Int, channels: Int, imageStride: Int,
                            srcPixels: Array[Array[T]],
                            dstPixels: Array[T]): Unit = {
    val src_offset_y = y * width
    val dst_offset_y = y * imageStride
    if (channels == 1) {
      // We can use a bit of speedup using system copy
      System.arraycopy(srcPixels.head, src_offset_y, dstPixels, dst_offset_y, width)
    } else {
      for (x <- 0 until width) {
        val dst_offset_x = dst_offset_y + x * channels
        for (c <- 0 until channels) {
          dstPixels(dst_offset_x + c) = srcPixels(c)(src_offset_y + x)
        }
      }
    }
  }


  private def copyFromStride[T](y: Int,
                                width: Int, channels: Int, imageStride: Int,
                                srcPixels: Array[T],
                                dstPixels: Array[Array[T]]): Unit = {
    val src_offset_y = y * imageStride
    val dst_offset_y = y * width
    if (channels == 1) {
      // We can use a bit of speedup using system copy
      System.arraycopy(srcPixels, src_offset_y, dstPixels.head, dst_offset_y, width)
    } else {
      for (x <- 0 until width) {
        val src_offset_x = src_offset_y + x * channels
        for (c <- 0 until channels) {
          dstPixels(c)(dst_offset_y + x) = srcPixels(src_offset_x + c)
        }
      }
    }
  }


  private def copyPixelsRGB(pixels: Array[Int], frame: Frame): Unit = {
    val dstBuffer = frame.image(0).position(0).asInstanceOf[ByteBuffer]

    val rowPixels = new Array[Byte](frame.imageWidth * frame.imageChannels)
    for (y <- 0 until frame.imageHeight) {
      val yOffset = y * frame.imageWidth
      for (x <- 0 until frame.imageWidth) {
        val c: Int = pixels(yOffset + x)
        //            val a = ((c & 0xff000000) >> 24).toByte
        val r = ((c & 0xff0000) >> 16).toByte
        val g = ((c & 0xff00) >> 8).toByte
        val b = (c & 0xff).toByte
        rowPixels(x * frame.imageChannels) = b
        rowPixels(x * frame.imageChannels + 1) = g
        rowPixels(x * frame.imageChannels + 2) = r
        //            rowPixels(x * channels + 3) = a
      }
      dstBuffer.position(y * frame.imageStride)
      dstBuffer.put(rowPixels)
    }
  }


  private def copyPixelsRGB(frame: Frame): Array[Int] = {
    val srcBuffer = frame.image(0).position(0).asInstanceOf[ByteBuffer]
    val dstPixels = new Array[Int](frame.imageWidth * frame.imageHeight)

    val rowPixels = new Array[Byte](frame.imageWidth * frame.imageChannels)
    for (y <- 0 until frame.imageHeight) {
      srcBuffer.position(y * frame.imageStride)
      srcBuffer.get(rowPixels)
      val yOffset = y * frame.imageWidth
      for (x <- 0 until frame.imageWidth) {
        val b = rowPixels(x * frame.imageChannels)
        val g = rowPixels(x * frame.imageChannels + 1)
        val r = rowPixels(x * frame.imageChannels + 2)
        dstPixels(yOffset + x) = (r << 16) + (g << 8) + b
      }
    }
    dstPixels
  }


  private def toByteProcessor(frame: Frame): ByteProcessor = {
    require(frame.imageDepth == Frame.DEPTH_UBYTE)
    require(frame.imageChannels == 1)

    val dstPixels = copyPixelsByte(frame)
    new ByteProcessor(frame.imageWidth, frame.imageHeight, dstPixels.head)
  }


  private def toShortProcessor(frame: Frame): ShortProcessor = {
    require(frame.imageDepth == Frame.DEPTH_USHORT)
    require(frame.imageChannels == 1)

    val dstPixels = copyPixelsShort(frame)
    new ShortProcessor(frame.imageWidth, frame.imageHeight, dstPixels.head, null)
  }


  private def toFloatProcessor(frame: Frame): FloatProcessor = {
    require(frame.imageDepth == Frame.DEPTH_FLOAT)
    require(frame.imageChannels == 1)

    val dstPixels = copyPixelsFloat(frame)
    new FloatProcessor(frame.imageWidth, frame.imageHeight, dstPixels.head)
  }


  private[javacv] def toColorProcessor(frame: Frame): ColorProcessor = {
    require(frame.imageDepth == Frame.DEPTH_UBYTE)
    require(frame.imageChannels == 3)

    val dstPixels = copyPixelsRGB(frame)
    new ColorProcessor(frame.imageWidth, frame.imageHeight, dstPixels)
  }

}

/**
 * Converts between ImageProcessor and Frame.
 */
class ImageProcessorFrameConverter extends FrameConverter[ImageProcessor] {

  import ImageProcessorFrameConverter._

  override def convert(ip: ImageProcessor): Frame = toFrame(ip)

  override def convert(frame: Frame): ImageProcessor = toImageProcessor(frame)

  private def toFrame(ip: ImageProcessor): Frame = {
    if (ip == null) {
      return null
    }

    val (imageDepth, channels) = ip match {
      case _: ByteProcessor => (Frame.DEPTH_UBYTE, 1)
      case _: ShortProcessor => (Frame.DEPTH_USHORT, 1)
      case _: FloatProcessor => (Frame.DEPTH_FLOAT, 1)
      case _: ColorProcessor => (Frame.DEPTH_UBYTE, 3)
      case _ => throw new UnsupportedOperationException(s"Unsupported ImageProcessor type: ${ip.getClass}")
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
    copyPixels(Array(ip), frame)

    frame
  }

  private def toImageProcessor(frame: Frame): ImageProcessor = {
    if (frame == null || frame.image == null) {
      return null
    }

    frame.imageDepth match {
      case Frame.DEPTH_UBYTE =>
        frame.imageChannels match {
          case 1 => toByteProcessor(frame)
          case 3 => toColorProcessor(frame)
          case n => throw new UnsupportedOperationException(s"Unsupported image channels $n for DEPTH_USHORT")
        }

      case Frame.DEPTH_USHORT =>
        if (frame.imageChannels == 1) {
          toShortProcessor(frame)
        } else {
          throw new IllegalArgumentException(s"'Short' image can only have 1 channel, got ${frame.imageChannels}")
        }
      case Frame.DEPTH_FLOAT =>
        if (frame.imageChannels == 1) {
          toFloatProcessor(frame)
        } else {
          throw new IllegalArgumentException(s"'Float' image can only have 1 channel, got ${frame.imageChannels}")
        }

      case d => throw new UnsupportedOperationException(s"Unsupported image depth: $d")
    }

  }
}
