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
