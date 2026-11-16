package com.uhufor.inspectionsample.contact.compose

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.uhufor.inspectionsample.R
import com.uhufor.inspectionsample.ui.theme.InspectionSampleTheme

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        val (profileImage, heartIcon, nameText, ageText, addressText, emailText) = createRefs()
        val backgroundSamples = createRef()
        val profileBottom = createBottomBarrier(profileImage, emailText)

        Image(
            painter = painterResource(id = R.drawable.ic_profile_placeholder),
            contentDescription = "Profile Image",
            modifier = Modifier
                .size(100.dp)
                .constrainAs(profileImage) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                },
            contentScale = ContentScale.Crop
        )

        Image(
            painter = painterResource(id = R.drawable.ic_heart),
            contentDescription = "Heart Icon",
            modifier = Modifier
                .size(30.dp)
                .constrainAs(heartIcon) {
                    bottom.linkTo(profileImage.bottom)
                    end.linkTo(profileImage.end)
                }
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    Toast.makeText(context, "Heart Clicked (Compose)", Toast.LENGTH_SHORT).show()
                }
        )

        Text(
            text = "John Doe",
            fontSize = 20.sp,
            color = Color(0xFFFC86BB),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.constrainAs(nameText) {
                top.linkTo(profileImage.top)
                start.linkTo(profileImage.end, margin = 16.dp)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            }
        )

        Text(
            text = "Age: 35",
            color = MaterialTheme.colorScheme.onSecondary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.constrainAs(ageText) {
                top.linkTo(nameText.bottom, margin = 8.dp)
                start.linkTo(nameText.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            }
        )

        Text(
            text = "Address: 123 Main St, Anytown USA",
            color = MaterialTheme.colorScheme.onSecondary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.constrainAs(addressText) {
                top.linkTo(ageText.bottom, margin = 8.dp)
                start.linkTo(nameText.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            }
        )

        Text(
            text = "Email: john.doe@example.com",
            color = MaterialTheme.colorScheme.onSecondary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.constrainAs(emailText) {
                top.linkTo(addressText.bottom, margin = 8.dp)
                start.linkTo(nameText.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            }
        )

        BackgroundColorSamples(
            modifier = Modifier.constrainAs(backgroundSamples) {
                top.linkTo(profileBottom, margin = 16.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            }
        )
    }
}

@Composable
private fun BackgroundColorSamples(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.compose_background_samples_title),
            style = MaterialTheme.typography.titleSmall,
        )
        // Text already exposes semantics: its background needs no inspection Modifier.
        Text(
            text = stringResource(R.string.compose_background_solid_text),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
                .background(Color(0xFF1565C0), RoundedCornerShape(8.dp))
                .border(2.dp, Color(0xFF0D47A1), RoundedCornerShape(8.dp))
                .graphicsLayer(alpha = 0.9f)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        Text(
            text = stringResource(R.string.compose_background_alpha_text),
            color = Color(0xFF3E2723),
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.fillMaxWidth()
                .background(Color(0x80FF9800), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        val boxLabel = stringResource(R.string.compose_background_box)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // A bare Box has no semantics in Compose 1.6.8; expose only this sample swatch.
            Box(
                Modifier.size(48.dp)
                    .background(Color(0xFF2E7D32), RoundedCornerShape(8.dp))
                    .semantics { contentDescription = boxLabel }
            )
            Text(
                text = boxLabel,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultProfilePreview() {
    InspectionSampleTheme {
        ProfileScreen()
    }
}
