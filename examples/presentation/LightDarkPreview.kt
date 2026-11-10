package com.company.app.ui.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/** One annotation renders the preview in both the light and the dark theme. */
@Preview(name = "Light", group = "theme", showBackground = true)
@Preview(
    name = "Dark",
    group = "theme",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
annotation class LightDarkPreview
