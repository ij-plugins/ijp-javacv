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

package ij_plugins.javacv.imgproc

import ij.gui.{GenericDialog, Line, Overlay}
import ij.measure.ResultsTable
import ij.plugin.filter.{PlugInFilter, PlugInFilterRunner}
import ij.plugin.frame.RoiManager
import ij.process.ImageProcessor
import ij.{IJ, ImagePlus}
import ij_plugins.javacv.IJOpenCVConverters.*
import ij_plugins.javacv.util.{ExtendedPlugInFilterTrait, IJPUtils, OpenCVUtils}
import org.bytedeco.opencv.global.opencv_core.CV_PI
import org.bytedeco.opencv.global.opencv_imgproc.*
import org.bytedeco.opencv.opencv_core.Point
import org.bytedeco.opencv.opencv_imgproc.Vec2fVector

import java.awt.AWTEvent
import scala.math.*

object HoughLinesPlugIn {
  OpenCVUtils.initJavaCV()

  private var distanceResolutionInPixels: Int  = 1
  private var angleResolutionInDegrees: Double = 1
  private var minimumVotes: Int                = 80
  private var sendToResultsTable: Boolean      = true
}

/**
 * Finds lines in a binary image using the standard Hough transform.
 *
 * Input is a binary grey 8-bit image.
 */
class HoughLinesPlugIn extends ExtendedPlugInFilterTrait {
  _plugin =>

  import HoughLinesPlugIn.*

  override def showDialog(imp: ImagePlus, command: String, pfr: PlugInFilterRunner): Int = {

    val message = "" +
      "Finds lines in a binary image using the standard Hough transform.\n" +
      "The Canny Edge Detector is a good way to create input for this plugin."

    val dialog = new GenericDialog(Title) {
      addPanel(IJPUtils.createHeaderAWT("OpenCV Hough Lines", message))

      addNumericField("Distance_Resolution", distanceResolutionInPixels, 0, 10, "pixels")
      addNumericField("Angle_Resolution", angleResolutionInDegrees, 2, 10, "degrees")
      addNumericField("Minimum_Votes", minimumVotes, 0, 10, "")
      addCheckbox("Send to Results Table", sendToResultsTable)

      addPreviewCheckbox(pfr)
      addDialogListener(_plugin)
      this.showDialog()
    }

    if (dialog.wasCanceled) {
      return PlugInFilter.DONE
    }

    IJ.setupDialog(imp, Flags)
  }

  override protected def Flags: Int = PlugInFilter.DOES_8G

  override def dialogItemChanged(gd: GenericDialog, e: AWTEvent): Boolean = {
    val _distanceResolutionInPixels = scala.math.round(gd.getNextNumber).toInt
    if (_distanceResolutionInPixels <= 0) {
      IJ.error(Title, "`Distance Resolution` must be larger than 0.")
      return false
    }
    distanceResolutionInPixels = _distanceResolutionInPixels

    val _angleResolutionInDegrees = gd.getNextNumber
    if (_angleResolutionInDegrees <= 0) {
      IJ.error(Title, "`Angle Resolution` must be larger than 0.")
      return false
    }
    angleResolutionInDegrees = _angleResolutionInDegrees

    val _minimumVotes = scala.math.round(gd.getNextNumber).toInt
    if (_minimumVotes <= 0) {
      IJ.error(Title, "`Minimum Votes` must be larger than 0.")
      return false
    }
    minimumVotes = _minimumVotes

    sendToResultsTable = gd.getNextBoolean

    true
  }

  /**
   * Method that does actual processing of the image.
   *
   * @param ip input image.
   */
  override protected def process(ip: ImageProcessor): Unit = {
    val src = toMat(ip)

    if (IJ.debugMode) OpenCVUtils.show(src, "src")

    // Hough transform for line detection
    val lines     = new Vec2fVector(0)
    val srn       = 0.0
    val stn       = 0.0
    val min_theta = 0.0
    val max_theta = CV_PI
    try {
      HoughLines(
        src,
        lines,
        distanceResolutionInPixels,
        scala.math.toRadians(angleResolutionInDegrees),
        minimumVotes,
        srn,
        stn,
        min_theta,
        max_theta
      )
    } catch {
      case ex: RuntimeException =>
        IJ.error(Title, "Error detecting Hough Lines.\n" + ex.getMessage)
        throw ex
    }

    // Decode results as Line ROIs
    try {

      // Add lines to ROI Manager
      new RoiManager()
      val roiManager = RoiManager.getInstance()
      roiManager.reset()
      val lineRois = linesToRois(lines, ip.getWidth, ip.getHeight)
      lineRois.zipWithIndex.foreach { case (lineRoi, i) =>
        lineRoi.setName(s"Line ${i + 1}")
        roiManager.addRoi(lineRoi)
      }

      // Show lines as an overlay on the current image
      val overlay = new Overlay()
      lineRois.foreach(overlay.addElement)
      imp.setOverlay(overlay)

      // Add to a Results Table
      if (sendToResultsTable) {
        val resultsTable = new ResultsTable()
        val rhoTheta     = linesToRhoTheta(lines)
        rhoTheta.zipWithIndex.foreach { case ((rho, theta), i) =>
          resultsTable.incrementCounter()
          resultsTable.addLabel(s"Line ${i + 1}")
          resultsTable.addValue("Rho", rho)
          resultsTable.addValue("Theta", theta)
        }
        resultsTable.show(Title)
      }

    } catch {
      case t: Throwable =>
        t.printStackTrace()
        throw t
    }
  }

  override protected def Title = "Hough Lines"

  private def linesToRhoTheta(lines: Vec2fVector): Seq[(Float, Float)] = {
    for (i <- 0 until lines.size().toInt) yield {
      val line  = lines.get(i)
      val rho   = line.get(0)
      val theta = line.get(1)
      (rho, theta)
    }
  }

  private def linesToRois(lines: Vec2fVector, width: Int, height: Int): Seq[Line] = {
    for (i <- 0 until lines.size().toInt) yield {
      val line  = lines.get(i)
      val rho   = line.get(0)
      val theta = line.get(1)
      val (pt1, pt2) =
        if (theta < Pi / 4.0 || theta > 3.0 * Pi / 4.0) {
          // ~vertical line
          // point of intersection of the line with first row
          val p1 = new Point(round(rho / cos(theta)).toInt, 0)
          // point of intersection of the line with last row
          val p2 = new Point(round((rho - height * sin(theta)) / cos(theta)).toInt, height)
          (p1, p2)
        } else {
          // ~horizontal line
          // point of intersection of the line with first column
          val p1 = new Point(0, round(rho / sin(theta)).toInt)
          // point of intersection of the line with last column
          val p2 = new Point(width, round((rho - width * cos(theta)) / sin(theta)).toInt)
          (p1, p2)
        }

      new Line(pt1.x, pt1.y, pt2.x, pt2.y)
    }
  }
}
