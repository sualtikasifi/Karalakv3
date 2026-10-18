package com.sualtikasifi.cizimhafiza.presentation.reports.console

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** **bold**, *italic* and `code` inside one line. */
private fun inline(line: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < line.length) {
        when {
            line.startsWith("**", i) -> {
                val end = line.indexOf("**", i + 2)
                if (end < 0) { append(line.substring(i)); i = line.length } else {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(line.substring(i + 2, end)) }
                    i = end + 2
                }
            }
            line[i] == '`' -> {
                val end = line.indexOf('`', i + 1)
                if (end < 0) { append(line.substring(i)); i = line.length } else {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Console.Grid)) { append(line.substring(i + 1, end)) }
                    i = end + 1
                }
            }
            line[i] == '*' && i + 1 < line.length && line[i + 1] != ' ' -> {
                val end = line.indexOf('*', i + 1)
                if (end < 0) { append(line[i]); i++ } else {
                    withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) { append(line.substring(i + 1, end)) }
                    i = end + 1
                }
            }
            else -> { append(line[i]); i++ }
        }
    }
}

/**
 * The assistant's answers: headings, bullets, numbered lists, tables (as aligned monospace) and
 * inline emphasis. Selectable, so any part can be copied.
 */
@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    SelectionContainer(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            text.lines().forEach { raw ->
                val line = raw.trimEnd()
                val t = line.trimStart()
                val indent = ((line.length - t.length) / 2).coerceAtMost(3)
                when {
                    t.isEmpty() -> Spacer(Modifier.padding(top = 2.dp))
                    t.startsWith("### ") -> Text(inline(t.removePrefix("### ")), style = Console.Subtitle, modifier = Modifier.padding(top = 6.dp))
                    t.startsWith("## ") -> Text(inline(t.removePrefix("## ")), style = Console.Title, modifier = Modifier.padding(top = 10.dp))
                    t.startsWith("# ") -> Text(inline(t.removePrefix("# ")), style = Console.Title.copy(fontSize = 19.sp), modifier = Modifier.padding(top = 10.dp))
                    t == "---" || t == "***" -> HorizontalDivider(color = Console.Border, modifier = Modifier.padding(vertical = 6.dp))
                    t.startsWith("|") -> Text(t, style = Console.Small.copy(fontFamily = FontFamily.Monospace, color = Console.Ink, fontSize = 11.sp))
                    t.startsWith("- ") || t.startsWith("* ") || t.startsWith("• ") -> Row(Modifier.fillMaxWidth().padding(start = (indent * 14).dp)) {
                        Text("•", style = Console.Body.copy(color = Console.Accent))
                        Spacer(Modifier.width(8.dp))
                        Text(inline(t.drop(2)), style = Console.Body)
                    }
                    Regex("^\\d+[.)] ").containsMatchIn(t) -> {
                        val num = t.substringBefore(' ')
                        Row(Modifier.fillMaxWidth().padding(start = (indent * 14).dp)) {
                            Text(num, style = Console.Body.copy(color = Console.Accent, fontWeight = FontWeight.SemiBold))
                            Spacer(Modifier.width(6.dp))
                            Text(inline(t.substringAfter(' ')), style = Console.Body)
                        }
                    }
                    t.startsWith("> ") -> Text(inline(t.removePrefix("> ")), style = Console.Body.copy(color = Console.InkSecondary), modifier = Modifier.padding(start = 8.dp))
                    else -> Text(inline(t), style = Console.Body)
                }
            }
        }
    }
}
