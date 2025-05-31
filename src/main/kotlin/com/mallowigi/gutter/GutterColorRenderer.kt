/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2022 Elior "Mallowigi" Boukhobza
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 *
 */
package com.mallowigi.gutter

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.ui.ColorChooserService
import com.intellij.ui.ColorUtil
import com.intellij.ui.picker.ColorListener
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.ColorIcon
import com.intellij.util.ui.EmptyIcon
import com.mallowigi.ColorHighlighterBundle.message
import com.mallowigi.gutter.actions.*
import java.awt.Color
import java.awt.datatransfer.StringSelection
import java.util.*
import javax.swing.Icon

class GutterColorRenderer(private val color: Color?, private val element: PsiElement?) : GutterIconRenderer() {
  override fun getIcon(): Icon = when {
    color != null -> {
      EditorColorsManager.getInstance().globalScheme.defaultForeground
        .let { ColorIcon(ICON_SIZE, ICON_SIZE, ICON_SIZE - 2, ICON_SIZE - 2, color, it, 3) }
        .let { JBUIScale.scaleIcon(it) }
    }

    else -> JBUIScale.scaleIcon(EmptyIcon.create(ICON_SIZE))
  }

  override fun getTooltipText(): String = message("choose.color")

  override fun getPopupMenuActions(): ActionGroup {
    return DefaultActionGroup(
      CopyAndroidArgb(color),
      CopyAndroidRgb(color),
      CopyHexAction(color),
      CopyRgbAction(color),
      CopyRgbaAction(color),
      CopyHslAction(color),
      CopyHslaAction(color),
      CopyJavaColorResource(color),
      CopyKotlinColorResource(color),
      CopyJavaRgb(color),
      CopyJavaRgba(color),
      CopyKotlinRgb(color),
      CopyKotlinRgba(color),
      CopyNetRgb(color),
      CopyNetArgb(color),
      CopyNSColorHsb(color),
      CopyNSColorHsba(color),
      CopyUIColorHsb(color),
      CopyUIColorHsba(color),
      CopySwiftHsb(color),
      CopySwiftHsba(color)
    )
  }

  /*override fun getClickAction(): @NonNls AnAction {
    return object : AnAction(message("choose.color1")) {
      override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val currentColor = color ?: return
        val newColor = ColorChooserService.instance.showDialog(
          editor.project, editor.component, message("replace.color"), currentColor, true
        )
        copyColor(currentColor, newColor)
      }


      private fun copyColor(currentColor: Color, newColor: Color?) {
        if (newColor == null || newColor == currentColor) return

        CopyPasteManager.getInstance().setContents(StringSelection(ColorUtil.toHex(newColor, false)))
      }
    }
  }*/

  private var currentHighlighter: RangeHighlighter? = null

  override fun getClickAction(): AnAction {
    return object : AnAction(message("choose.color1")) {
      override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val currentColor = color ?: return
        val project = editor.project ?: return
        val element = this@GutterColorRenderer.element ?: return

        // Mostrar popup de selección de color
        ColorChooserService.instance.showPopup(
          project = project,
          currentColor = currentColor,
          editor = editor,
          listener = { newColor, _ ->
            if (newColor != null && newColor != currentColor) {
              //highlightColorTemporary(editor, element, newColor)
              //replaceColorInCode(editor, element, currentColor, color)
            }
          },
          showAlpha = true,
          /*popupCloseListener = object : com.intellij.ui.picker.ColorPickerPopupCloseListener {
            override fun closed(color: Color?, canceled: Boolean) {
              removeCurrentHighlighter(editor)
              if (!canceled && color != null && color != currentColor) {
                replaceColorInCode(editor, element, currentColor, color)
              }
            }

            override fun onPopupClosed() {
              TODO("Not yet implemented")
            }
          }*/
        )

        fun highlightColorTemporary(editor: Editor, element: PsiElement, color: Color) {
          removeCurrentHighlighter(editor)

          val highlighter = editor.markupModel.addRangeHighlighter(
            element.textRange.startOffset,
            element.textRange.endOffset,
            com.intellij.openapi.editor.markup.HighlighterLayer.SELECTION - 1,
            com.intellij.openapi.editor.markup.TextAttributes().apply {
              backgroundColor = color
            },
            com.intellij.openapi.editor.markup.HighlighterTargetArea.EXACT_RANGE
          )

          currentHighlighter = highlighter
        }

        fun removeCurrentHighlighter(editor: Editor) {
          currentHighlighter?.let {
            editor.markupModel.removeHighlighter(it)
            currentHighlighter = null
          }
        }

        fun replaceColorInCode(editor: Editor, element: PsiElement, oldColor: Color, newColor: Color) {
          val project = editor.project ?: return
          val document = editor.document

          val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return

          val argb = (newColor.alpha shl 24) or
                  (newColor.red shl 16) or
                  (newColor.green shl 8) or
                  (newColor.blue)

          val unsigned = argb.toUInt()
          val newText = "0x${unsigned.toString(16).uppercase()}"

          val factory = JavaPsiFacade.getInstance(project).elementFactory
          val newElement = factory.createExpressionFromText(newText, element)

          WriteCommandAction.runWriteCommandAction(project, "Update Color", null, {
            element.replace(newElement)
          }, psiFile)
        }

      }

      private fun highlightColorTemporary(editor: Editor, element: PsiElement, color: Color) {
        removeCurrentHighlighter(editor)

        val highlighter = editor.markupModel.addRangeHighlighter(
          element.textRange.startOffset,
          element.textRange.endOffset,
          com.intellij.openapi.editor.markup.HighlighterLayer.SELECTION - 1,
          com.intellij.openapi.editor.markup.TextAttributes().apply {
            backgroundColor = color
          },
          com.intellij.openapi.editor.markup.HighlighterTargetArea.EXACT_RANGE
        )

        currentHighlighter = highlighter
      }

      private fun removeCurrentHighlighter(editor: Editor) {
        currentHighlighter?.let {
          editor.markupModel.removeHighlighter(it)
          currentHighlighter = null
        }
      }

      private fun replaceColorInCode(editor: Editor, element: PsiElement, oldColor: Color, newColor: Color) {
        val project = editor.project ?: return
        val document = editor.document

        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return

        val argb = (newColor.alpha shl 24) or
                (newColor.red shl 16) or
                (newColor.green shl 8) or
                (newColor.blue)

        val unsigned = argb.toUInt()
        val newText = "0x${unsigned.toString(16).uppercase()}"

        val factory = JavaPsiFacade.getInstance(project).elementFactory
        val newElement = factory.createExpressionFromText(newText, element)

        WriteCommandAction.runWriteCommandAction(project, "Update Color", null, {
          element.replace(newElement)
        }, psiFile)
      }
    }
  }


  /*
  override fun getClickAction(): AnAction {
    return object : AnAction(message("choose.color1")) {
      override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val currentColor = color ?: return
        element ?: return

        ColorChooserService.instance.showPopup(
          project = editor.project,
          currentColor = currentColor,
          editor = editor,
          listener = { newColor, source ->
            newColor?.takeIf { it != currentColor }?.let { updatedColor ->
              replaceColorInCode(editor, element, color, updatedColor)
            }
          },
          showAlpha = true
        )
      }

      private fun copyColor(currentColor: Color, newColor: Color?) {
        if (newColor == null || newColor == currentColor) return
        CopyPasteManager.getInstance().setContents(StringSelection(ColorUtil.toHex(newColor, false)))
      }

      private fun replaceColorInCode(editor: Editor, element: PsiElement, oldColor: Color, newColor: Color) {
        val project = editor.project ?: return
        val document = editor.document

        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return

        val argb = (newColor.alpha shl 24) or
                (newColor.red shl 16) or
                (newColor.green shl 8) or
                (newColor.blue)

        val unsigned = argb.toUInt()
        val newText = "0x${unsigned.toString(16).uppercase()}"


        val factory = JavaPsiFacade.getInstance(project).elementFactory
        val newElement = factory.createExpressionFromText(newText, element)

        WriteCommandAction.runWriteCommandAction(project, "Update Color", null, {
          element.replace(newElement)
          //DaemonCodeAnalyzer.getInstance(project).restart(psiFile)



        }, psiFile)
      }
    }
  }

  */


  override fun isNavigateAction(): Boolean = true

  override fun equals(other: Any?): Boolean {
    return when {
      this === other -> true
      other == null || javaClass != other.javaClass -> false
      else -> {
        val renderer = other as GutterColorRenderer
        color == renderer.color
      }
    }
  }

  override fun hashCode(): Int = Objects.hash(color)

  override fun getAlignment(): Alignment = Alignment.RIGHT

  companion object {
    private const val ICON_SIZE = 12
  }
}
