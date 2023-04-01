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

package ij_plugins.javacv.mcc

import ij_plugins.javacv.plugins.mcc.ChartType
import org.bytedeco.opencv.global.opencv_mcc
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*

class ChartTypeTest extends AnyFlatSpec {
  behavior of "ChartType"

  it should "match OpenCV constants" in {
    // Enumeratum does not allow to use non-literals in values, cannot use OpenCV constants to define enums directly.
    // We have to verify that hand coded values match OpenCV constants
    ChartType.MCC24.value should be(opencv_mcc.MCC24)
    ChartType.SG140.value should be(opencv_mcc.SG140)
    ChartType.VINYL18.value should be(opencv_mcc.VINYL18)
  }
}
