package com.pfitzer.tootsie.presentation.icons

/*
MIT License

Copyright (c) Tailwind Labs, Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

*/
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val CustomIcons.Link: ImageVector
    get() {
        if (_link != null) return _link!!

        _link = ImageVector.Builder(
            name = "link",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(19.902f, 4.098f)
                arcToRelative(3.75f, 3.75f, 0f, false, false, -5.304f, 0f)
                lineToRelative(-4.5f, 4.5f)
                arcToRelative(3.75f, 3.75f, 0f, false, false, 1.035f, 6.037f)
                arcToRelative(0.75f, 0.75f, 0f, false, true, -0.646f, 1.353f)
                arcToRelative(5.25f, 5.25f, 0f, false, true, -1.449f, -8.45f)
                lineToRelative(4.5f, -4.5f)
                arcToRelative(5.25f, 5.25f, 0f, true, true, 7.424f, 7.424f)
                lineToRelative(-1.757f, 1.757f)
                arcToRelative(0.75f, 0.75f, 0f, true, true, -1.06f, -1.06f)
                lineToRelative(1.757f, -1.757f)
                arcToRelative(3.75f, 3.75f, 0f, false, false, 0f, -5.304f)
                close()
                moveToRelative(-7.389f, 4.267f)
                arcToRelative(0.75f, 0.75f, 0f, false, true, 1f, -0.353f)
                arcToRelative(5.25f, 5.25f, 0f, false, true, 1.449f, 8.45f)
                lineToRelative(-4.5f, 4.5f)
                arcToRelative(5.25f, 5.25f, 0f, true, true, -7.424f, -7.424f)
                lineToRelative(1.757f, -1.757f)
                arcToRelative(0.75f, 0.75f, 0f, true, true, 1.06f, 1.06f)
                lineToRelative(-1.757f, 1.757f)
                arcToRelative(3.75f, 3.75f, 0f, true, false, 5.304f, 5.304f)
                lineToRelative(4.5f, -4.5f)
                arcToRelative(3.75f, 3.75f, 0f, false, false, -1.035f, -6.037f)
                arcToRelative(0.75f, 0.75f, 0f, false, true, -0.354f, -1f)
                close()
            }
        }.build()

        return _link!!
    }

private var _link: ImageVector? = null
