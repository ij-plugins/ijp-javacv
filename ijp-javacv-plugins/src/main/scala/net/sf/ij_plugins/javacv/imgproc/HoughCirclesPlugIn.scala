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

package net.sf.ij_plugins.javacv.imgproc

import ij.gui.{GenericDialog, OvalRoi, Overlay}
import ij.plugin.filter.{PlugInFilter, PlugInFilterRunner}
import ij.plugin.frame.RoiManager
import ij.process.ImageProcessor
import ij.{IJ, ImagePlus}
import net.sf.ij_plugins.javacv.IJOpenCVConverters._
import net.sf.ij_plugins.javacv.util.{ExtendedPlugInFilterTrait, IJPUtils, OpenCVUtils}
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_imgproc._

import java.awt.AWTEvent

object HoughCirclesPlugIn {
  OpenCVUtils.initJavaCV()

  private val method: Int = HOUGH_GRADIENT
  private var dp: Double = 2
  private var minDist: Double = 33
  private var highThreshold: Double = 200
  private var votes: Double = 100
  private var minRadius: Int = 40
  private var maxRadius: Int = 90
  private var sendToResultsTable: Boolean = true
}

/**
 * Finds lines in a binary image using the standard Hough transform.
 *
 * Input is a binary grey 8-bit image.
 */
class HoughCirclesPlugIn extends ExtendedPlugInFilterTrait {
  _plugin =>

  import HoughCirclesPlugIn._

  override protected def Flags: Int = PlugInFilter.DOES_8G

  override protected def Title = "OpenCV Hough Circles"

  /**
   * Method that does actual processing of the image.
   *
   * @param ip input image.
   */
  override protected def process(ip: ImageProcessor): Unit = {
    val src = toMat(ip)

    // Hough transform for circle detection
    val circles = new Vec3fVector(0)
    try {
      HoughCircles(
        src,
        circles,
        method,
        dp,
        minDist,
        highThreshold,
        votes,
        minRadius,
        maxRadius)
    } catch {
      case ex: RuntimeException =>
        IJ.error(Title, "Error detecting Hough Circles.\n" + ex.getMessage)
        throw ex
    }

    // Decode results as circle ROIs
    try {
      new RoiManager()
      val roiManager = RoiManager.getInstance()

      if (circles.size() > 0) {
        val xyr = circlesToXYR(circles)

        // Add circles to ROI Manager
        roiManager.reset()

        val circleRois = xyr.map { case (x, y, r) => new OvalRoi(x - r, y - r, 2 * r, 2 * r) }
        circleRois.zipWithIndex.foreach { case (circleRoi, i) =>
          circleRoi.setName(s"Circle ${i + 1}")
          roiManager.addRoi(circleRoi)
        }

        // Show circles as an overlay on the current image
        val overlay = new Overlay()
        circleRois.foreach(overlay.addElement)
        imp.setOverlay(overlay)

        // TODO: Add to a Results Table
        //      if (sendToResultsTable) {
        //        val resultsTable = new ResultsTable()
        //        val rhoTheta = circlesToRhoTheta(circles)
        //        rhoTheta.zipWithIndex.foreach { case ((rho, theta), i) =>
        //          resultsTable.incrementCounter()
        //          resultsTable.addLabel(s"Circle ${i + 1}")
        //          resultsTable.addValue("Rho", rho)
        //          resultsTable.addValue("Theta", theta)
        //        }
        //        resultsTable.show(Title)
        //      }
      } else {
        imp.setOverlay(null)
        roiManager.reset()

        // TODO: clear results table
      }

    }
    catch {
      case t: Throwable =>
        t.printStackTrace()
        throw t
    }
  }

  override def showDialog(imp: ImagePlus, command: String, pfr: PlugInFilterRunner): Int = {

    val message = "" +
      "Finds circles in a grayscale image using the Hough transform."

    val dialog = new GenericDialog(Title) {
      addPanel(IJPUtils.createInfoPanel(Title, message))

      addNumericField("DP", dp, 2, 10, "")
      addNumericField("minDist", minDist, 2, 10, "pixels")
      addNumericField("highThreshold", highThreshold, 2, 10, "")
      addNumericField("votes", votes, 2, 10, "")
      addNumericField("minRadius", minRadius, 2, 10, "pixels")
      addNumericField("maxRadius", maxRadius, 2, 10, "pixels")
      addCheckbox("Send to Results Table", sendToResultsTable)

      addPreviewCheckbox(pfr)
      addDialogListener(_plugin)
      showDialog()
    }

    if (dialog.wasCanceled) {
      return PlugInFilter.DONE
    }

    IJ.setupDialog(imp, Flags)
  }

  override def dialogItemChanged(gd: GenericDialog, e: AWTEvent): Boolean = {
    //    private var method: Int = HOUGH_GRADIENT
    dp = gd.getNextNumber
    minDist = gd.getNextNumber
    highThreshold = gd.getNextNumber
    votes = gd.getNextNumber
    minRadius = scala.math.round(gd.getNextNumber).toInt
    maxRadius = scala.math.round(gd.getNextNumber).toInt

    sendToResultsTable = gd.getNextBoolean

    true
  }

  private def circlesToXYR(circles: Vec3fVector): Seq[(Float, Float, Float)] = {
    for (i <- 0 until circles.size().toInt) yield {
      val c = circles.get(i)
      val x = c.get(0)
      val y = c.get(1)
      val r = c.get(2)

      (x, y, r)
    }
  }
}
