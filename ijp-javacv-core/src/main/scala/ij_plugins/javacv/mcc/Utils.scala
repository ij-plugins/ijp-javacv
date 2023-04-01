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

import org.bytedeco.javacpp.indexer.FloatIndexer
import org.bytedeco.javacpp.{FloatPointer, IntPointer, PointerPointer}
import org.bytedeco.opencv.global.opencv_core.{CV_16U, CV_8U, merge, split}
import org.bytedeco.opencv.global.opencv_imgproc.calcHist
import org.bytedeco.opencv.opencv_core.{Mat, MatVector, Scalar}

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

}
