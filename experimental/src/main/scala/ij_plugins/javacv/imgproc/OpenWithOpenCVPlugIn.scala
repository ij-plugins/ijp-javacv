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

import ij.io.OpenDialog
import ij.plugin.PlugIn
import ij_plugins.javacv.IJOpenCVConverters
import ij_plugins.javacv.util.OpenCVUtils
import org.bytedeco.opencv.global.opencv_core.merge
import org.bytedeco.opencv.global.opencv_imgcodecs.{IMREAD_UNCHANGED, imreadmulti}
import org.bytedeco.opencv.opencv_core.{Mat, MatVector}

import java.io.{File, IOException}

class OpenWithOpenCVPlugIn extends PlugIn {
  override def run(arg: String): Unit = {
    val od = new OpenDialog("Open image with OpenCV codecs")

    val path = od.getPath

    val file = new File(path)

    // Read input image
    val matV = new MatVector()
    val ok   = imreadmulti(file.getAbsolutePath, matV, IMREAD_UNCHANGED)
    if (!ok) {
      throw new IOException("Couldn't load image: " + file.getAbsolutePath)
    }

    val mat = new Mat()
    merge(matV, mat)

    OpenCVUtils.printInfo(mat)

    val imp = IJOpenCVConverters.toImagePlus(mat)
    imp.setTitle(file.getName)
    imp.show()
  }
}
