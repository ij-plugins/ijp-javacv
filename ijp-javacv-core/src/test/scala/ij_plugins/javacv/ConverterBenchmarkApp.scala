/*
 * Image/J Plugins
 * Copyright (C) 2002-2022 Jarek Sacha
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

package ij_plugins.javacv

import ij.process.ImageProcessor
import ij_plugins.javacv.util.OpenCVUtils
import IJOpenCVConverters.toImageProcessor
import org.bytedeco.javacv.{Frame, Java2DFrameConverter, OpenCVFrameConverter}
import org.bytedeco.opencv.global.opencv_core.*
import org.bytedeco.opencv.opencv_core.{Mat, Scalar}

import scala.util.Using

object ConverterBenchmarkApp extends App {

  val mat = new Mat(9000, 9000, CV_8UC3, new Scalar(10, 100, 150, 0))
  //    val mat = new Mat(9000, 9000, CV_8U, new Scalar(10));

  OpenCVUtils.printInfo(mat, "Mat to be converted to ImageProcessor")

  for (_ <- 1 to 10) {
    val tb0 = System.currentTimeMillis()
    val ipB = convertUsingJava2DFrame(mat)
    val tb1 = System.currentTimeMillis()
    assert(ipB != null)

    val ti0 = System.currentTimeMillis()
    val ipI = convertUsingIJFrame(mat)
    val ti1 = System.currentTimeMillis()
    assert(ipI != null)

    val tf0 = System.currentTimeMillis()
    Using(covertToFrame(mat)) { frame =>
      assert(frame != null)
    }.get
    val tf1 = System.currentTimeMillis()

    println(
      s"Conversion time: through BufferedImage: ${tb1 - tb0}ms, direct: ${ti1 - ti0}ms, just frame ${tf1 - tf0}ms"
    )
  }

  def covertToFrame(mat: Mat): Frame = {
    Using(new OpenCVFrameConverter.ToMat()) { converter =>
      converter.convert(mat)
    }.get
  }

  def convertUsingJava2DFrame(mat: Mat): ImageProcessor = {
    Using.Manager { use =>
      val openCVConverter = use(new OpenCVFrameConverter.ToMat())
      val frame           = use(openCVConverter.convert(mat))
      val java2DConverter = use(new Java2DFrameConverter())
      val bi              = java2DConverter.convert(frame)
      toImageProcessor(bi)
    }.get
  }

  def convertUsingIJFrame(mat: Mat): ImageProcessor = {
    Using.Manager { use =>
      val converter = use(new OpenCVFrameConverter.ToMat())
      val frame     = use(converter.convert(mat))
      new ImageProcessorFrameConverter().convert(frame)
    }.get
  }

}
