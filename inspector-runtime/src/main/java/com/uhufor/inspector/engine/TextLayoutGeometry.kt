package com.uhufor.inspector.engine

/** Keep the mapping explicit: layout bounds can differ from selection bounds due to padding. */
internal fun createTextLayoutInfo(
    width: Float,
    height: Float,
    lineCount: Int,
    firstBaseline: Float,
    lastBaseline: Float,
    map: (Float, Float) -> Pair<Float, Float>,
): TextLayoutInfo? {
    if (width <= 0 || height <= 0 || lineCount <= 0 ||
        !listOf(width, height, firstBaseline, lastBaseline).all { it.isFinite() }) return null
    fun line(x1: Float, y1: Float, x2: Float, y2: Float): GuideLine {
        val start = map(x1, y1)
        val end = map(x2, y2)
        return GuideLine(start.first, start.second, end.first, end.second)
    }
    val outline = listOf(line(0f, 0f, width, 0f), line(width, 0f, width, height),
        line(width, height, 0f, height), line(0f, height, 0f, 0f))
    // Compose 1.6.8 publicly exposes first/last baselines, not each intermediate baseline.
    val baselines = listOf(firstBaseline, lastBaseline).distinct().filter { it in 0f..height }
        .map { line(0f, it, width, it) }
    if ((outline + baselines).any {
            !listOf(it.startX, it.startY, it.endX, it.endY).all(Float::isFinite)
        }) return null
    return TextLayoutInfo(width, height, lineCount, firstBaseline, lastBaseline, outline, baselines)
}
