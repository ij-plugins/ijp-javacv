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

import ij.gui.Roi
import ij.process.{ByteProcessor, ColorProcessor}
import ij.{IJ, ImagePlus}
import org.bytedeco.opencv.global.opencv_imgproc.*
import org.bytedeco.opencv.opencv_core.{Mat, Rect}

import java.awt.Rectangle

/**
 * Provides interaction with OpenCV implementation of GrubCut algorithm,
 * hiding any native memory and resources management.
 *
 * @param ip input image to be segmented.
 */
class GrubCutInteraction(ip: ColorProcessor) extends AutoCloseable {

  /** Instance is 'open' is resouces are still allocated, before `close()` was called. */
  private var isOpen = true

  private val image = IJOpenCVConverters.toMat(ip)

  private val iterCount = 5
  private var result    = new Mat()
  // Need to allocate arrays for temporary data
  private val bgdModel = new Mat()
  private val fgdModel = new Mat()

  def initialRun(rect: Rectangle): Unit = {
    validate()

    val rectangle = new Rect(rect.x, rect.y, rect.width, rect.height)

    // GrabCut segmentation
    grabCut(image, result, rectangle, bgdModel, fgdModel, iterCount, GC_INIT_WITH_RECT)

    if (IJ.debugMode) new ImagePlus("Initial Result", IJOpenCVConverters.toImageProcessor(result)).show()
  }

  def update(): Unit = {
    validate()

    if (IJ.debugMode) new ImagePlus("Input", IJOpenCVConverters.toImageProcessor(result)).show()

    grabCut(image, result, new Rect(), bgdModel, fgdModel, iterCount, GC_INIT_WITH_MASK)

    if (IJ.debugMode) new ImagePlus("Result", IJOpenCVConverters.toImageProcessor(result)).show()
  }

  def addToForeground(roi: Roi): Unit = {
    validate()

    val resultIP = IJOpenCVConverters.toImageProcessor(result)
    resultIP.setValue(GC_FGD)
    resultIP.fill(roi)

    result.close()
    result = IJOpenCVConverters.toMat(resultIP)
  }

  def addToBackground(roi: Roi): Unit = {
    validate()

    val resultIP = IJOpenCVConverters.toImageProcessor(result)
    resultIP.setValue(GC_BGD)
    resultIP.fill(roi)

    result.close()
    result = IJOpenCVConverters.toMat(resultIP)
  }

  def backgroundRoi: Roi         = extractROI(GC_BGD)
  def foregroundRoi: Roi         = extractROI(GC_FGD)
  def probableBackgroundRoi: Roi = extractROI(GC_PR_BGD)
  def probableForegroundRoi: Roi = extractROI(GC_PR_FGD)

  override def close(): Unit = {
    isOpen = false
    image.close()
    result.close()
    bgdModel.close()
    fgdModel.close()
  }

  private def extractROI(level: Int): Roi = {
    val ip = IJOpenCVConverters.toImageProcessor(result).asInstanceOf[ByteProcessor]
    IJUtils.toRoi(ip, level, level)
  }

  private def validate(): Unit = {
    if (!isOpen) {
      throw new IllegalStateException(s"This instance of 'GrubCutInteraction' was already closed.")
    }
  }
}
