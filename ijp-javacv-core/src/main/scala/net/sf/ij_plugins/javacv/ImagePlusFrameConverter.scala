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

import ij.process.ImageProcessor
import ij.{ImagePlus, ImageStack}
import net.sf.ij_plugins.javacv.ImageProcessorFrameConverter._
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
        case _ => toGRAY8(frame)
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
