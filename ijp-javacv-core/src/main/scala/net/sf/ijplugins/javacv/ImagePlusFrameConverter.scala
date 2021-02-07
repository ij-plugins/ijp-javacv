/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package net.sf.ijplugins.javacv

import ij.process.ImageProcessor
import ij.{ImagePlus, ImageStack}
import net.sf.ijplugins.javacv.ImageProcessorFrameConverter._
import org.bytedeco.javacv.{Frame, FrameConverter}

/**
 * Converts between ImageJ's ImagePlus and JavaCV Frame.
 */
class ImagePlusFrameConverter extends FrameConverter[ImagePlus] {
  // TODO: Add option to assume that 3/4 channel images are color

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
        if (imp.getStackSize == 1) {
          (Frame.DEPTH_UBYTE, 3)
        } else {
          throw new UnsupportedOperationException(s"RGB ImagePlus supported only with stack size == 1, " +
            s"got: ${imp.getStackSize}")
        }
      case t =>
        throw new UnsupportedOperationException(s"Unsupported ImagePlus type: $t")
    }

    val width = imp.getWidth
    val height = imp.getHeight

    if (frame == null ||
      frame.imageWidth != width ||
      frame.imageHeight != height ||
      frame.imageChannels != channels) {
      // Reallocate frame, its type or size changed
      frame = new Frame(width, height, imageDepth, channels)
    }

    // Copy pixels
    val ips: Array[ImageProcessor] = {
      val stack = imp.getStack
      for (k <- (1 to stack.getSize).toArray) yield stack.getProcessor(k)
    }

    copyPixels(ips, frame)

    frame
  }


  private def toImagePlus(frame: Frame): ImagePlus = {
    if (frame == null || frame.image == null) {
      return null
    }

    frame.imageDepth match {
      case Frame.DEPTH_UBYTE => frame.imageChannels match {
        case 3 => toRGB(frame)
        case n => toGRAY8(frame)
      }
      case Frame.DEPTH_USHORT => toGRAY16(frame)
      case Frame.DEPTH_FLOAT => toGRAY32(frame)
      case imageDepth => throw new UnsupportedOperationException(s"Unsupported image depth: $imageDepth")
    }

  }


  private def toGRAY8(frame: Frame): ImagePlus = {
    require(frame.imageDepth == Frame.DEPTH_UBYTE)

    val dstPixels = copyPixelsByte(frame)

    toImagePlus(frame.imageWidth, frame.imageHeight, dstPixels)
  }


  private def toGRAY16(frame: Frame): ImagePlus = {
    require(frame.imageDepth == Frame.DEPTH_USHORT)

    val dstPixels = copyPixelsShort(frame)

    toImagePlus(frame.imageWidth, frame.imageHeight, dstPixels)
  }


  private def toGRAY32(frame: Frame): ImagePlus = {
    require(frame.imageDepth == Frame.DEPTH_FLOAT)

    val dstPixels = copyPixelsFloat(frame)

    toImagePlus(frame.imageWidth, frame.imageHeight, dstPixels)
  }


  private def toRGB(frame: Frame): ImagePlus = {
    val cp = toColorProcessor(frame)
    new ImagePlus("", cp)
  }


  private def toImagePlus[T](width: Int, height: Int, pixels: Array[Array[T]]): ImagePlus = {
    val stack = new ImageStack(width, height, pixels.length)
    for (i <- pixels.indices) {
      stack.setPixels(pixels(i), i + 1)
    }

    new ImagePlus("", stack)
  }
}
