/*
 * Copyright (c) 2011-2022 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package net.sf.ij_plugins.javacv

import ij.gui.{NonBlockingGenericDialog, Roi, RoiListener}
import ij.plugin.PlugIn
import ij.{IJ, ImagePlus}

class RegionSelectionPlugin extends PlugIn with RoiListener {

  private val Title = "Region Selection Demo"

  private var impOption: Option[ImagePlus] = None
  private var roi1: Option[Roi]            = None
  private var roi2: Option[Roi]            = None

  private var roiSelectionID: Int = -1

  override def run(arg: String): Unit = {
    impOption = Option(IJ.getImage)

    if (impOption.isEmpty) {
      IJ.noImage()
      return
    }

    Roi.addRoiListener(this)

    println("Prepare ROI 1")
    roiSelectionID = 1
    val d1 = new NonBlockingGenericDialog(Title)
    d1.addMessage("Select ROI 1")
    d1.showDialog()

    println("Prepare ROI 2")
    roiSelectionID = 2
    val d2 = new NonBlockingGenericDialog(Title)
    d2.addMessage("Select ROI 2")
    d2.showDialog()

//    roiSelectionID = 2
//    IJ.showMessage(Title, "Select ROI 2")
//

    Roi.removeRoiListener(this)
    println(s"$Title - done")
  }

  override def roiModified(imp: ImagePlus, id: Int): Unit = {
    println(s"roiModified: $id")
    if (id == RoiListener.COMPLETED) {
      impOption.foreach { impRef =>
        if (impRef == imp) {
          Option(imp.getRoi).foreach { roi =>
            roiSelectionID match {
              case 1 =>
                roi1 = Option(roi)
                IJ.log("ROI 1 selected")

              case 2 =>
                roi2 = Option(roi)
                IJ.log("ROI 2 selected")

              case _ =>
            }
          }
        }
      }
    }
  }
}
