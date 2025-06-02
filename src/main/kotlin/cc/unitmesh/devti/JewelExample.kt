package cc.unitmesh.devti

import androidx.compose.runtime.Composable
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Button
import org.jetbrains.jewel.ui.component.Text

@Composable
fun JewelExample() {
    JewelTheme {
        Button(onClick = {}) {
            Text("Hello Jewel")
        }
    }
}
