package com.mallowigi.search

enum class ColorRegexes(val text: String) {
  KT_COLOR("""Color\(\s*0x([A-Fa-f0-9]{8})\s*\)""")
}