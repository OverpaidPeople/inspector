package com.uhufor.inspector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uhufor.inspector.R
import com.uhufor.inspector.ui.compose.dvdp
import com.uhufor.inspector.ui.compose.dvsp

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    previewColor: Int? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dvdp)
            .padding(vertical = 1.dvdp),
    ) {
        Text(
            text = label,
            fontSize = 9.dvsp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.4f)
                .background(
                    colorResource(R.color.inspector_label_bg),
                    shape = RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp)
                )
                .padding(horizontal = 2.dvdp)
        )
        Spacer(modifier = Modifier.width(2.dvdp))
        if (previewColor != null) {
            Canvas(
                Modifier.size(10.dvdp).align(Alignment.CenterVertically)
                    .border(0.5.dp, Color.Gray)
                    .semantics { contentDescription = value }
            ) {
                // Checkerboard keeps transparent ARGB colors distinguishable from white.
                drawRect(Color.White)
                val half = size.width / 2
                drawRect(Color.LightGray, size = Size(half, half))
                drawRect(Color.LightGray, topLeft = Offset(half, half), size = Size(half, half))
                drawRect(Color(previewColor))
            }
            Spacer(modifier = Modifier.width(2.dvdp))
        }
        Text(
            text = value,
            fontSize = 9.dvsp,
            modifier = Modifier
                .fillMaxHeight()
                .weight(weight = 1f, fill = true)
                .background(
                    colorResource(R.color.inspector_value_bg),
                    shape = RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp)
                )
                .padding(horizontal = 2.dvdp)
                .basicMarquee(iterations = 5)
        )
    }
}
