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

package ij_plugins.javacv.experimental

import ij.plugin.PlugIn
import org.scijava.Context
import org.scijava.script.{ScriptLanguage, ScriptService}
import org.scijava.ui.DefaultUIService
import org.scijava.ui.swing.script.{Helper, Main, ScriptEditor, TextEditor}

import java.awt.event.{WindowAdapter, WindowEvent}
import javax.swing.{SwingUtilities, WindowConstants}

object ScriptEditorRunner {
//  private lazy val context = new org.scijava.Context()
  def main(args: Array[String]): Unit = {
    Helper.launch("Groovy", true)
//    Main.launch("Groovy")
  }
}

class ScriptEditorRunner extends PlugIn {

  import org.scijava.util.VersionUtils

  VersionUtils.getVersion(classOf[VersionUtils])

//  import ScriptEditorRunner.*
  override def run(arg: String): Unit = {
    Helper.launch("Groovy", false)
//    Main.launch("Groovy")
  }
}
