package com.example.glarmto.ui.widget

import android.app.Application
import android.content.Context
import com.example.glarmto.R
import com.example.glarmto.data.util.ResourceTexts
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class GlarmToWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Looked up here (not in the composable) so it follows the language picked in the app.
        val texts = ResourceTexts(context.applicationContext as Application)
        val title = texts.get(R.string.widget_title)
        val subtitle = texts.get(R.string.widget_subtitle)
        provideContent {
            WidgetContent(title, subtitle)
        }
    }

    @Composable
    private fun WidgetContent(title: String, subtitle: String) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = TextStyle(
                    color = ColorProvider(Color(0xFFE53935)),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = subtitle,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 14.sp
                )
            )
        }
    }
}
