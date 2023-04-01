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

package ij_plugins.javacv.mcc

import ij.ImagePlus
import ij.gui.{PolygonRoi, Roi}
import ij.process.{ByteProcessor, ColorProcessor, ImageProcessor}
import org.bytedeco.javacpp.indexer.FloatIndexer
import org.bytedeco.javacpp.{FloatPointer, IntPointer, PointerPointer}
import org.bytedeco.opencv.global.opencv_core.{CV_16U, CV_8U, merge, split}
import org.bytedeco.opencv.global.opencv_imgproc.calcHist
import org.bytedeco.opencv.opencv_core.{Mat, MatVector, Point2fVector, Scalar}

import scala.util.control.NonFatal

object Utils {

  /**
   * Create `Scalar` from 3 values. `Scalar` constructor accepts 1, 2, or 4 values.
   * This function creates 4 value variant setting the last value to 0.
   * This is useful when creating a `Scalar` representing a color.
   */
  def scalar(v0: Double, v1: Double, v2: Double): Scalar = new Scalar(v0, v1, v2, 0)

  /**
   * Compute min and max values of `mat` ignoring fraction of `p` pixels with extreme low values
   * and fraction of `p` pixels with extreme high values.
   *
   * This is intended to deal with outlier values.
   *
   * @param mat input
   * @param p percentile values in a min and max tail. `p` is in range `[0, 0.5)`
   * @return tuple (min, max)
   */
  def percentileMinMax(mat: Mat, p: Double): (Int, Int) = {
    require(p >= 0)
    require(p < 0.5)
    val nbBins = mat.depth() match {
      case CV_8U  => 256
      case CV_16U => 256 * 256
      case _      => throw new IllegalArgumentException(s"Unsupported image depth: ${mat.depth()}")
    }

    val hist = histogram(mat, nbBins)
//    printInfo(hist, "hist")

    val er = hist.createIndexer().asInstanceOf[FloatIndexer]
    val a  = new Array[Float](er.sizes()(0).toInt)
    for (i <- a.indices) a(i) = er.get(i)

    val nbPixels = mat.size(0) * mat.size(1)
    var sum      = 0.0
    val minPerc  = p * nbPixels * mat.channels()
    val maxPerc  = (1 - p) * nbPixels * mat.channels()
    var minV     = 0
    var maxV     = 0
    for (i <- a.indices) {
      sum += a(i)
      if (sum <= minPerc) minV = i
      if (sum < maxPerc) maxV = i
    }

    (minV, maxV)
  }

  /**
   * Compute histogram of `image` accumulating counts on all bands
   * @param image input image
   * @param numberOfBins number of bins in the histogram
   * @return histogram
   */
  def histogram(image: Mat, numberOfBins: Int): Mat = {
    require(image != null)

    val bands = new MatVector()
    split(image, bands)

    // Compute histogram
    val hist = new Mat()

    val intPtrChannels  = new IntPointer(1L).put(0)
    val intPtrHistSize  = new IntPointer(1L).put(numberOfBins)
    val histRange       = Array[Float](0, numberOfBins)
    val ptrPtrHistRange = new PointerPointer[FloatPointer](histRange)
    for (i <- 0 until bands.size().toInt)
      calcHist(
        bands.get(i),
        1,               // histogram of 1 image only
        intPtrChannels,  // the channel used
        new Mat(),       // no mask is used
        hist,            // the resulting histogram
        1,               // it is a 3D histogram
        intPtrHistSize,  // number of bins
        ptrPtrHistRange, // pixel value range
        true,            // uniform
        true             // no accumulation
      )
    hist
  }

  def reverseSlices(src: Mat): Mat = {
    val bandsSrc = new MatVector()
    split(src, bandsSrc)

    val n         = bandsSrc.size().toInt
    val bandsDest = new MatVector(n)

    for (i <- 0 until n) bandsDest.put(n - i - 1, bandsSrc.get(i))

    val dst = new Mat()
    merge(bandsDest, dst)

    dst
  }

  def toPolygonRoi(points: Point2fVector): PolygonRoi = {
    val nPoints: Int = points.size().toInt
    val xPoints      = new Array[Float](nPoints)
    val yPoints      = new Array[Float](nPoints)
    for (i <- 0 until nPoints) {
      val p2f = points.get(i)
      xPoints(i) = p2f.x()
      yPoints(i) = p2f.y()
    }

    new PolygonRoi(xPoints, yPoints, nPoints, Roi.POLYGON)
  }

  /**
   * Convert input image to RGB24 color image. In input is stack of grey image they will be scaled fir 0-256 range.
   *
   * @param imp contains one color image (4b-bit or indexed 8-bit) or a stack of 3 gray images interpreted as RGB bands.
   * @return RGB24 color imageRGB24 color image
   * @throws IllegalArgumentException if inout image is in a format that cannot be interpreted as a RGB color image
   */
  def toColorProcessor(imp: ImagePlus): ColorProcessor = {
    val stackSize = imp.getStackSize
    imp.getType match {
      case ImagePlus.COLOR_256 | ImagePlus.COLOR_RGB if stackSize == 1 =>
        imp.getProcessor.convertToColorProcessor()
      case ImagePlus.GRAY8 | ImagePlus.GRAY16 | ImagePlus.GRAY32 if stackSize == 3 =>
        val stack = imp.getStack
        val bands = (1 to stack.size()).map(stack.getProcessor).toArray
        try {
          mergeRGB(bands, doScaling = true)
        } catch {
          case NonFatal(ex) =>
            throw new IllegalArgumentException(s"Failed to create RGB24 image. ${ex.getMessage}")
        }
      case _ =>
        throw new IllegalArgumentException("Required Color image or stack of 3 gray images.\n" +
          s"Got image type ${imp.getType} and ${stackSize} slices")
    }
  }

  /**
   * Merges RGB bands into a ColorProcessor.
   *
   * @param src `ImageProcessor`s for red, green, and blue band. `ImageProcessor`s cannot be `ColorProcessors`
   * @return merged bands
   * @see #splitRGB
   * @see ij_plugins.color.util.ImageJUtils#mergeRGB
   */
  private def mergeRGB(src: Array[ImageProcessor], doScaling: Boolean): ColorProcessor = {
    validateSameTypeAndDimensions(src, 3)
    require(!src(0).isInstanceOf[ColorProcessor], "Cannot convert stack of ColorProcessor to a ColorProcessor")

    mergeRGB(Array.range(0, 3).map(src(_).convertToByte(doScaling).asInstanceOf[ByteProcessor]))
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
   * @param src    images to validate
   * @param length expected number of images
   * @tparam T image processor type
   * @throws java.lang.IllegalArgumentException if the images in the array are not of the same dimension.
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

}
