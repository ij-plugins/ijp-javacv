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

package mcc

import ij.IJ
import ij.plugin.filter.RankFilters
import ij_plugins.javacv.IJOpenCVConverters
import ij_plugins.javacv.mcc.Utils.{percentileMinMax, reverseSlices, scalar}
import ij_plugins.javacv.util.OpenCVUtils
import ij_plugins.javacv.util.OpenCVUtils.printInfo
import org.bytedeco.javacpp.indexer.FloatIndexer
import org.bytedeco.javacpp.{DoublePointer, FloatPointer, IntPointer, PointerPointer}
import org.bytedeco.opencv.global.opencv_core.*
import org.bytedeco.opencv.global.opencv_imgproc.*
import org.bytedeco.opencv.global.opencv_mcc.MCC24
import org.bytedeco.opencv.opencv_core.{Mat, MatVector, Point2fVector, Scalar}
import org.bytedeco.opencv.opencv_mcc.{CCheckerDetector, CCheckerDraw}

import java.io.File
import java.nio.IntBuffer

@main
def runIJ(fileName: String): Unit =

  val file = new File(fileName)
//  val file = new File("data/IMG_0903_025p-crop.png")
  val imp1 = IJ.openImage(file.getCanonicalPath)
  imp1.show()

  val mat1 = {
    val m = IJOpenCVConverters.toMat(imp1)
    require(m.channels() == 3)
    m.depth() match {
      // JavaCV frame converter does not reverse channels in 16-bit-per-color images to BGR expected by OpenCV
      case CV_16U => reverseSlices(m)
      case CV_8U  => m
      case _      => throw new IllegalArgumentException(s"Unsupported image depth: ${m.depth()}")
    }
  }

  val (minV, maxV) = percentileMinMax(mat1, 0.01)

  println(s"minV: $minV")
  println(s"maxV: $maxV")

  val mat2  = new Mat()
  val alpha = 255d / (maxV - minV)
  val beta  = -minV * alpha
  mat1.convertTo(mat2, CV_8U, alpha, beta)
  OpenCVUtils.show(mat2, "Source Enhanced Brightness Range")

  val detector = CCheckerDetector.create()

  val hasChart = detector.process(mat2, MCC24)

  if hasChart then
    val checker = detector.getListColorChecker.get(0)
    CCheckerDraw
      .create(checker, scalar(255, 255, 255), 1)
      .draw(mat2)

    OpenCVUtils.show(mat2, "Detected ColorChecker tiles")
  else
    println("ColorChecker not detected")
