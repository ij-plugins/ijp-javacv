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

package ij_plugins.javacv.imgproc

import ij.gui.{NonBlockingGenericDialog, Overlay, Roi}
import ij.plugin.PlugIn
import ij.process.ColorProcessor
import ij.{IJ, ImagePlus}
import ij_plugins.javacv.imgproc.GrubCutInteraction
import ij_plugins.javacv.util.{IJPUtils, IJUtils}

import java.awt.Color

class GrabCutPlugIn extends PlugIn {

  private val Title = "Grab Cut Segmentation"

  private var imp: Option[ImagePlus] = None

  private var gci: Option[GrubCutInteraction] = None

  private var frgROIAddition: Option[Roi] = None
  private var bkgROIAddition: Option[Roi] = None

  private val bkgColor: Color      = Color.BLUE
  private val bkgTransparency: Int = 128

  // TODO: Option to set number of iterations
  override def run(arg: String): Unit = {
    imp = Option(IJ.getImage)
    imp match {
      case Some(imp) =>
        imp.getProcessor match {
          case cp: ColorProcessor =>
            Option(imp.getRoi) match {
              case Some(roi) =>
                interact(cp.convertToColorProcessor(), roi)
              case None =>
                IJ.error(Title, "ROI enclosing the object is required.")
            }
          case _ =>
            IJ.error(Title, "GrubCut requires color image.")
        }
      case None =>
        IJ.noImage()
    }
  }

  private def interact(ip: ColorProcessor, roi: Roi): Unit = {
    IJ.showStatus(s"$Title: Starting...")
    IJ.showProgress(0.1)

    gci = Option(new GrubCutInteraction(ip))

    IJ.showStatus(s"$Title: Running initial segmentation...")
    IJ.showProgress(0.2)
    gci.foreach(_.initialRun(roi.getBounds))
    IJ.showProgress(0.9)
    updateDisplay()
    IJ.showProgress(1.01)

    val message =
      """
        | <html>
        | Segments foreground object.<br>
        | Requires rectangular ROI to perform initial segmentation.<br>
        | After initial segmentation you can mark additional ROIs as elements of the <br>
        | foreground or background.<br>
        | Clicking "OK" with create final selection.<br>
        | </html>
        |""".stripMargin

    val dialog = new NonBlockingGenericDialog(Title)
    dialog.addPanel(IJPUtils.createInfoPanel(Title, message))
    dialog.addButton(
      "Add to Foreground",
      (_) => {
        imp.flatMap(im => Option(im.getRoi)) match {
          case Some(r) =>
            frgROIAddition = IJUtils.add(frgROIAddition, r)
            updateDisplay()
          case None =>
            IJ.error(Title, "Selection required")
        }
      }
    )

    dialog.addButton(
      "Add to Background",
      (_) => {
        imp.flatMap(im => Option(im.getRoi)) match {
          case Some(r) =>
            bkgROIAddition = IJUtils.add(bkgROIAddition, r)
            updateDisplay()
          case None =>
            IJ.error(Title, "Selection required")
        }
      }
    )

    dialog.addButton(
      "Update Preview",
      (_) => {
        IJ.showStatus(s"$Title: Updating GrubCut...")

        // Add BKG and FRG modifications
        IJ.showProgress(0.01)

        frgROIAddition.foreach(roi => gci.foreach(_.addToForeground(roi)))
        frgROIAddition = None
        IJ.showProgress(0.05)

        bkgROIAddition.foreach(roi => gci.foreach(_.addToBackground(roi)))
        bkgROIAddition = None
        IJ.showProgress(0.1)

        IJ.showStatus(s"$Title: Running GrubCut...")
        gci.foreach(_.update())
        IJ.showProgress(0.9)

        updateDisplay()
        IJ.showProgress(1.01)
        IJ.showStatus("")
      }
    )

    dialog.showDialog()

    if (dialog.wasOKed()) {
      showFinalResult()
      gci.foreach(_.close())
      gci = None
    } else {
      // Restore original ROI
      imp.foreach(_.setOverlay(null))
      imp.foreach(_.setRoi(roi))
    }
  }

  private def updateDisplay(): Unit = {

    val bkgOverlay = new Overlay()

    IJ.showStatus(s"$Title: Displaying results")
    gci.foreach { gc =>
      IJUtils
        .add(gc.backgroundRoi, gc.probableBackgroundRoi)
        .foreach { roi =>
          roi.setStrokeColor(bkgColor)
          roi.setStrokeWidth(2)
          bkgOverlay.add(roi, "All background outline")

          addFill(bkgOverlay, roi, bkgColor, bkgTransparency, "All background fill")
        }
    }

    frgROIAddition.foreach { r =>
      r.setStrokeColor(Color.GREEN)
      bkgOverlay.add(r, "Foreground addition")
      addFill(bkgOverlay, r, r.getStrokeColor, bkgTransparency, "Foreground addition fill")
    }
    bkgROIAddition.foreach { r =>
      r.setStrokeColor(Color.RED)
      bkgOverlay.add(r, "Background addition")
      addFill(bkgOverlay, r, r.getStrokeColor, bkgTransparency, "Background addition fill")
    }

    imp.foreach(_.setRoi(null.asInstanceOf[Roi]))
    imp.foreach(_.setOverlay(bkgOverlay))
    IJ.showStatus("")
  }

  private def addFill(overlay: Overlay, roi: Roi, color: Color, transparency: Int, name: String): Unit = {
    val roiFill = roi.clone().asInstanceOf[Roi]
    roiFill.setFillColor(new Color(color.getRed, color.getGreen, color.getBlue, transparency))

    overlay.add(roiFill, name)
  }

  private def showFinalResult(): Unit = {
    IJ.showStatus(s"$Title: Display final results")

    gci.foreach { gc =>
      IJUtils.add(gc.foregroundRoi, gc.probableForegroundRoi) match {
        case Some(roi) => imp.foreach(_.setRoi(roi))
        case None      => imp.foreach(_.setRoi(null.asInstanceOf[Roi]))
      }
    }

    imp.foreach(_.setOverlay(null))

    IJ.showStatus(s"$Title: Result")
  }
}
