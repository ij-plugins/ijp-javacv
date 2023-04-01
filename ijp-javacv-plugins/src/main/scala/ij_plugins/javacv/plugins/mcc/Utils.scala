/*
 * Image/J Plugins
 * Copyright (C) 2002-2023 Jarek Sacha
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

package ij_plugins.javacv.plugins.mcc

import ij.ImagePlus
import ij.process.*

import scala.util.control.NonFatal

object Utils {

  /**
   * @param src images to validate
   * @param length expected number of images
   * @tparam T  image processor type
   * @throws java.lang.IllegalArgumentException   if the images in the array are not of the same dimension.
   * @see ij_plugins.color.util.ImageJUtils#validateSameDimensions
   */
  @inline
  private def validateSameDimensions[T <: ImageProcessor](src: Array[T], length: Int): Unit = {
    require(src != null, "Input cannot be null.")
    require(src.length == length, "Input array has to have " + length + " elements.")
    require(!src.contains(null.asInstanceOf[T]), "Input array cannot have null entries.")
    val width  = src(0).getWidth
    val height = src(0).getHeight
    require(
      src.forall(width == _.getWidth),
      "All input images have to have the same width: " + src.map(_.getWidth).mkString(",")
    )
    require(
      src.forall(height == _.getHeight),
      "All input images have to have the same height: " + src.map(_.getHeight).mkString(",")
    )
  }

  /**
   * @param src
   * images to validate
   * @param length
   * expected number of images
   * @tparam T
   * image processor type
   * @throws java.lang.IllegalArgumentException
   * if the images in the array are not of the same dimension.
   * @see ij_plugins.color.util.ImageJUtils#validateSameDimensions
   */
  @inline
  private def validateSameTypeAndDimensions[T <: ImageProcessor](src: Array[T], length: Int): Unit = {
    validateSameDimensions(src, length)
    if (length > 1) {
      val t = src(0).getClass
      require(src.tail.forall(_.getClass == t), "All input images must be of the same type.")
    }
  }

  /**
   * Merges RGB bands into a ColorProcessor.
   *
   * @param src ByteProcessor for red, green, and blue band.
   * @return merged bands
   * @see #splitRGB
   * @see ij_plugins.color.util.ImageJUtils#mergeRGB
   */
  private def mergeRGB(src: Array[ByteProcessor]): ColorProcessor = {
    validateSameTypeAndDimensions(src, 3)

    val width  = src(0).getWidth
    val height = src(0).getHeight
    val dest   = new ColorProcessor(width, height)
    dest.setRGB(
      src(0).getPixels.asInstanceOf[Array[Byte]],
      src(1).getPixels.asInstanceOf[Array[Byte]],
      src(2).getPixels.asInstanceOf[Array[Byte]]
    )
    dest
  }

  /**
   * Merges RGB bands into a ColorProcessor.
   *
   * @param src `ImageProcessor`s for red, green, and blue band. `ImageProcessor`s cannot be `ColorProcessors`
   * @return   merged bands
   * @see #splitRGB
   * @see ij_plugins.color.util.ImageJUtils#mergeRGB
   */
  private def mergeRGB(src: Array[ImageProcessor], doScaling: Boolean): ColorProcessor = {
    validateSameTypeAndDimensions(src, 3)
    require(!src(0).isInstanceOf[ColorProcessor], "Cannot convert stack of ColorProcessor to a ColorProcessor")

    mergeRGB(Array.range(0, 3).map(src(_).convertToByte(doScaling).asInstanceOf[ByteProcessor]))
  }

  /**
   * Convert input image to RGB24 color image. In input is stack of grey image they will be scaled fir 0-256 range.
   * @param imp contains one color image (4b-bit or indexed 8-bit) or a stack of 3 gray images interpreted as RGB bands.
   * @return RGB24 color imageRGB24 color image
   */
  def toColorProcessor(imp: ImagePlus): Either[String, ColorProcessor] = {
    val stackSize = imp.getStackSize
    imp.getType match {
      case ImagePlus.COLOR_256 | ImagePlus.COLOR_RGB if stackSize == 1 =>
        Right(imp.getProcessor.convertToColorProcessor())
      case ImagePlus.GRAY8 | ImagePlus.GRAY16 | ImagePlus.GRAY32 if stackSize == 3 =>
        val stack = imp.getStack
        val bands = (1 to stack.size()).map(stack.getProcessor).toArray
        try {
          Right(mergeRGB(bands, doScaling = true))
        } catch {
          case NonFatal(ex) =>
            Left(s"Failed to create RGB24 image. ${ex.getMessage}")
        }
      case _ =>
        Left("Required Color image or stack of 3 gray images.\n" +
               s"Got image type ${imp.getType} and ${stackSize} slices")
    }
  }

}
