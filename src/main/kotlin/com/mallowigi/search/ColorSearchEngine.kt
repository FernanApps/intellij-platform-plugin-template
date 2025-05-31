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

package com.mallowigi.search

import com.mallowigi.search.parsers.ColorParserFactory
import com.mallowigi.visitors.ColorVisitor
import java.awt.Color
import java.util.regex.Pattern

object ColorSearchEngine {

  /**
   * Try to parse a color using the provided formats
   *
   * @param text text to parse
   * @param visitor a Language Visitor to provide additional formats (ex:
   *     Color() for Java/Kotlin)
   */
  fun getColor(text: String, visitor: ColorVisitor): Color? = try {
    val normalizedText = text.replace("\"".toRegex(), "").replace("'".toRegex(), "")
    ColorParserFactory.getParser(normalizedText, visitor).parseColor(normalizedText)
  } catch (e: RuntimeException) {
    null
  }

}

