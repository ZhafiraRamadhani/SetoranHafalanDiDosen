package com.example.setoranhafalandosen.tampilan.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

/**
 * Composable yang aman untuk menampilkan gambar dengan optimasi otomatis
 * untuk mencegah error "Canvas: trying to draw too large bitmap"
 */
@Composable
fun SafeImage(
    data: Any,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    size: Dp = 120.dp
) {
    val context = LocalContext.current
    val sizePx = with(androidx.compose.ui.platform.LocalDensity.current) { size.toPx().toInt() }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(data)
            .size(sizePx)
            .crossfade(true)
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        contentScale = contentScale
    )
}

/**
 * Composable untuk menampilkan gambar resource dengan aman
 */
@Composable
fun SafeResourceImage(
    resourceId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    size: Dp = 120.dp
) {
    val context = LocalContext.current
    val sizePx = with(androidx.compose.ui.platform.LocalDensity.current) { size.toPx().toInt() }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(resourceId)
            .size(sizePx)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        contentScale = contentScale
    )
}

/**
 * Composable untuk menampilkan gambar dengan loading state
 */
@Composable
fun SafeImageWithLoading(
    data: Any,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    size: Dp = 120.dp,
    cornerRadius: Dp = 0.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val context = LocalContext.current
    val sizePx = with(androidx.compose.ui.platform.LocalDensity.current) { size.toPx().toInt() }

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(data)
            .size(sizePx)
            .crossfade(true)
            .build()
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(size / 3),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            is AsyncImagePainter.State.Error -> {
                // Tampilkan placeholder atau icon error
                Box(
                    modifier = Modifier
                        .size(size)
                        .background(backgroundColor)
                )
            }
            else -> {
                Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(size),
                    contentScale = contentScale
                )
            }
        }
    }
}

/**
 * Composable khusus untuk logo dengan optimasi
 */
@Composable
fun SafeLogo(
    resourceId: Int,
    contentDescription: String = "Logo",
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    SafeResourceImage(
        resourceId = resourceId,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        size = size
    )
}
