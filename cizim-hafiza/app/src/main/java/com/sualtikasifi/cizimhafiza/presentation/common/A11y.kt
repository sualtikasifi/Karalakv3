package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/** Gives a custom clickable (a chip, an avatar, a badge) a spoken name and button role for screen readers. */
fun Modifier.a11yButton(label: String): Modifier = semantics(mergeDescendants = true) {
    contentDescription = label
    role = Role.Button
}
