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

import ij.gui.NonBlockingGenericDialog
import ij.plugin.PlugIn
import ij.{IJ, ImagePlus}
import ij_plugins.javacv.IJOpenCVConverters
import ij_plugins.javacv.util.OpenCVUtils
import org.bytedeco.opencv.opencv_core.Mat

import java.awt.event.ActionEvent
import java.awt.image.BufferedImage

class MatGCTesterPlugin extends PlugIn {

  private var imageOption: Option[ImagePlus]  = None
  private var matOption: Option[Mat]          = None
  private var biOption: Option[BufferedImage] = None
  private val Title                           = "Mat() GC Tester"

  override def run(arg: String): Unit = {

    imageOption = Option(IJ.getImage)
    imageOption match {
      case Some(v) => v
      case None =>
        IJ.noImage()
        return
    }

    val gd = createDialog()

    gd.showDialog()
  }

  def createDialog(): NonBlockingGenericDialog = {
    val gd = new NonBlockingGenericDialog(Title)

    import ij.IJ

    import java.awt.Button
    import java.awt.event.ActionListener
    // Create custom button// Create custom button

    val createMatButton = new Button("Create Mat")
    createMatButton.addActionListener(new ActionListener() {
      override def actionPerformed(e: ActionEvent): Unit = {
        IJ.log(s"You clicked the button: ${createMatButton.getLabel}")
        imageOption.foreach { im =>
          matOption = Option(IJOpenCVConverters.toMat(im))
        }
      }
    })

    val createBIButton = new Button("Create Buffered Image") {
      addActionListener(new ActionListener() {
        override def actionPerformed(e: ActionEvent): Unit = {
          IJ.log(s"You clicked the button: ${getLabel}")
          matOption.foreach { mat =>
            biOption = Option(OpenCVUtils.toBufferedImage(mat))
          }
        }
      })
    }

    val displayMatButton = new Button("Display Mat")
    displayMatButton.addActionListener(new ActionListener() {
      override def actionPerformed(e: ActionEvent): Unit = {
        IJ.log(s"You clicked the button: ${displayMatButton.getLabel}")
        matOption.foreach { mat =>
          val ip = IJOpenCVConverters.toImageProcessor(mat)
          new ImagePlus("from Mat", ip).show()
        }
      }
    })

    val displayBIButton = new Button("Display BI") {
      addActionListener(new ActionListener() {
        override def actionPerformed(e: ActionEvent): Unit = {
          IJ.log(s"You clicked the button: ${getLabel}")
          biOption.foreach { bi =>
            new ImagePlus("from Mat", bi).show()
          }
        }
      })
    }

    gd.add(createMatButton)
    gd.add(displayMatButton)
    gd.add(createBIButton)
    gd.add(displayBIButton)

    gd
  }
}
