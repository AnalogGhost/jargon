package com.hackerapps.jargon.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

private const val TERM_TAG = "term"

private fun buildLinkedText(text: String, terms: List<String>, linkColor: Color): AnnotatedString {
    if (terms.isEmpty() || text.isEmpty()) return AnnotatedString(text)

    val sortedTerms = terms.filter { it.isNotBlank() }.sortedByDescending { it.length }
    if (sortedTerms.isEmpty()) return AnnotatedString(text)

    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val match = sortedTerms.firstOrNull { term ->
                text.regionMatches(i, term, 0, term.length, ignoreCase = true)
            }
            if (match != null) {
                pushStringAnnotation(TERM_TAG, match)
                withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                    append(text.substring(i, i + match.length))
                }
                pop()
                i += match.length
            } else {
                append(text[i])
                i += 1
            }
        }
    }
}

@Composable
fun LinkedText(
    text: String,
    terms: List<String>,
    onTermClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(text, terms, linkColor) { buildLinkedText(text, terms, linkColor) }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotated,
        style = style,
        modifier = modifier.pointerInput(annotated) {
            detectTapGestures { offset ->
                val result = layoutResult ?: return@detectTapGestures
                val position = result.getOffsetForPosition(offset)
                annotated.getStringAnnotations(TERM_TAG, position, position)
                    .firstOrNull()
                    ?.let { onTermClick(it.item) }
            }
        },
        onTextLayout = { layoutResult = it }
    )
}
