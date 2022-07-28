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

package net.sf.ij_plugins.javacv

import ij.gui.Roi
import ij.plugin.PlugIn
import ij.process.ImageProcessor
import ij.{IJ, ImagePlus}
import net.sf.ij_plugins.javacv.util.OpenCVUtils.toMat8U
import org.bytedeco.opencv.global.opencv_imgproc.*
import org.bytedeco.opencv.opencv_core.{Mat, Rect}

class GrabCutPlugIn extends PlugIn {

  private val Title = "Grab Cut Segmentation"

  // TODO: Option to set number of iterations
  // TODO: Dialog with preview
  // TODO: Option se send ROI back to original image (or send back an overlay + select color of the ROI or
  //       auto select most distant to color in the image?)
  // TODO: Option se send ROI back to ROI Manager
  // TODO: Support mask selection
  // TODO: Show debug data from GrubCut

  override def run(arg: String): Unit = {
    Option(IJ.getImage) match {
      case Some(imp) =>
        Option(imp.getRoi) match {
          case Some(roi) =>
            val dst = run(imp.getProcessor, roi)
            new ImagePlus(s"${imp.getShortTitle}+GrabCut", dst).show()
          case None =>
            IJ.error(Title, "ROI enclosing the object is required.")
        }
      case None =>
        IJ.noImage()
    }
  }

  private def run(ip: ImageProcessor, roi: Roi): ImageProcessor = {

    val image = IJOpenCVConverters.toMat(ip)

    // Define bounding rectangle, pixels outside this rectangle will be labeled as background.
//    val rectangle = new Rect(10, 100, 380, 180)
    val bounds    = roi.getBounds
    val rectangle = new Rect(bounds.x, bounds.y, bounds.width, bounds.height)

    val result    = new Mat()
    val iterCount = 5
    val mode      = GC_INIT_WITH_RECT

    // Need to allocate arrays for temporary data
    val bgdModel = new Mat()
    val fgdModel = new Mat()

    // GrabCut segmentation
    grabCut(image, result, rectangle, bgdModel, fgdModel, iterCount, mode)

    // Prepare image for display: extract foreground
    threshold(result, result, GC_PR_FGD - 0.5, GC_PR_FGD + 0.5, THRESH_BINARY)

    val r2 = toMat8U(result)

    val dst = IJOpenCVConverters.toImageProcessor(r2)
    dst
  }
}
