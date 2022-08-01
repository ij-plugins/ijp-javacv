/*
 * Copyright (c) 2011-2022 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package net.sf.ij_plugins.javacv

import ij.gui.Roi
import ij.process.{ByteProcessor, ImageProcessor}
import ij.{IJ, ImagePlus}
import org.bytedeco.opencv.global.opencv_imgproc.*
import org.bytedeco.opencv.opencv_core.{Mat, Rect}

import java.awt.Rectangle

class GrubCutInteraction(ip: ImageProcessor) {

  private val image = IJOpenCVConverters.toMat(ip)

  private val iterCount = 5
  private var result    = new Mat()
  // Need to allocate arrays for temporary data
  private val bgdModel = new Mat()
  private val fgdModel = new Mat()

  def initialRun(rect: Rectangle): Unit = {

    val rectangle = new Rect(rect.x, rect.y, rect.width, rect.height)

    // GrabCut segmentation
    grabCut(image, result, rectangle, bgdModel, fgdModel, iterCount, GC_INIT_WITH_RECT)

    if (IJ.debugMode) new ImagePlus("Initial Result", IJOpenCVConverters.toImageProcessor(result)).show()
  }

  def update(): Unit = {

    if (IJ.debugMode) new ImagePlus("Input", IJOpenCVConverters.toImageProcessor(result)).show()

    grabCut(image, result, new Rect(), bgdModel, fgdModel, iterCount, GC_INIT_WITH_MASK)

    if (IJ.debugMode) new ImagePlus("Result", IJOpenCVConverters.toImageProcessor(result)).show()
  }

  def addToForeground(roi: Roi): Unit = {
    val resultIP = IJOpenCVConverters.toImageProcessor(result)
    resultIP.setValue(GC_FGD)
    resultIP.fill(roi)

    result = IJOpenCVConverters.toMat(resultIP)
  }

  def addToBackground(roi: Roi): Unit = {
    val resultIP = IJOpenCVConverters.toImageProcessor(result)
    resultIP.setValue(GC_BGD)
    resultIP.fill(roi)

    result = IJOpenCVConverters.toMat(resultIP)
  }

  def backgroundRoi: Roi         = extractROI(GC_BGD)
  def foregroundRoi: Roi         = extractROI(GC_FGD)
  def probableBackgroundRoi: Roi = extractROI(GC_PR_BGD)
  def probableForegroundRoi: Roi = extractROI(GC_PR_FGD)

  private def extractROI(level: Int): Roi = {
    val ip = IJOpenCVConverters.toImageProcessor(result).asInstanceOf[ByteProcessor]
    IJUtils.toRoi(ip, level, level)
  }
}
