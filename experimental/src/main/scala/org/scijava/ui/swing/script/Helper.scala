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

package org.scijava.ui.swing.script

import org.scijava.Context
import org.scijava.script.{ScriptLanguage, ScriptService}
import org.scijava.ui.DefaultUIService
import org.scijava.ui.swing.script.TextEditor

import java.awt.event.{WindowAdapter, WindowEvent}
import javax.swing.{SwingUtilities, WindowConstants}

object Helper {
  def launch(language: String, exitOnClose: Boolean): Unit = {
    val context          = new Context()
    val defaultUIService = context.getService(classOf[DefaultUIService])
    defaultUIService.showUI()
    val editor        = new TextEditor(context)
    val scriptService = context.getService(classOf[ScriptService])
    val lang          = scriptService.getLanguageByName(language)
    if (lang == null) throw new IllegalArgumentException("Script language '" + language + "' not found")
    editor.setLanguage(lang)
    editor.addWindowListener(new WindowAdapter() {
      override def windowClosed(e: WindowEvent): Unit = {
        defaultUIService.dispose()
        SwingUtilities.invokeLater(() => { context.dispose() })
      }
    })
    if (exitOnClose) editor.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    editor.setVisible(true)
//    Main.launch(language)
  }
}
