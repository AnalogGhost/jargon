package com.hackerapps.jargon.tools.screenshots

import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RadialGradientPaint
import java.awt.RenderingHints
import java.awt.geom.GeneralPath
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

// Green-phosphor CRT terminal palette. BACKGROUND is a near-black with a faint green cast;
// PHOSPHOR is the bright scan-line green used for the caption, cursor, and frame; DIM_GREEN is
// the app's brand TerminalGreen (#1B5E20), used for the glow and the shell prompt.
private val BACKGROUND = Color(0x06, 0x0A, 0x06)
private val PHOSPHOR = Color(0x4A, 0xE2, 0x4A)
private val DIM_GREEN = Color(0x1B, 0x5E, 0x20)
private const val WATERMARK_ALPHA = 0.06f
private const val BLOOM_ALPHA = 0.30f
private const val SCANLINE_ALPHA = 0.10f
private const val SCANLINE_PITCH = 3

// Wide enough that the glow, scanlines, and watermark have visible canvas to sit on — at a thin
// margin the frame covers almost the whole image and the effects render mostly underneath it.
private const val SIDE_MARGIN = 220
private const val CAPTION_BAND_HEIGHT = 420
private const val FRAME_BORDER = 6
private const val CAPTION_FONT_SIZE = 108f
private const val CAPTION_LINE_SPACING = 1.05f
private const val PROMPT = "> "
private const val CURSOR = "█"

// VT323 is a bundled DEC VT-terminal bitmap face (SIL OFL, see resources/fonts/VT323-OFL.txt) —
// it carries the green-phosphor "old hacker" look far better than a generic mono. Falls back to
// whatever monospaced face the JDK can find if the resource is somehow missing.
private val CAPTION_FONT: Font by lazy {
    val stream = object {}.javaClass.getResourceAsStream("/fonts/VT323-Regular.ttf")
    if (stream != null) {
        stream.use { Font.createFont(Font.TRUETYPE_FONT, it) }
    } else {
        val fallbacks = listOf("DejaVu Sans Mono", "Liberation Mono", "Noto Sans Mono", Font.MONOSPACED)
        Font(fallbacks.first { Font(it, Font.PLAIN, 12).family.equals(it, ignoreCase = true) || it == Font.MONOSPACED }, Font.BOLD, 12)
    }
}

fun main() {
    val repoRoot = File(".").canonicalFile
    val fastlaneDir = File(repoRoot, "fastlane")
    val sourceMetadataDir = File(fastlaneDir, "metadata/android")
    val outputMetadataDir = File(fastlaneDir, "metadata/android-play")
    val captions = parseCaptions(File(fastlaneDir, "screenshot_captions.yml"))

    val locales = sourceMetadataDir.listFiles { f -> f.isDirectory }
        ?.map { it.name }
        ?.sorted()
        ?: error("No locales found under $sourceMetadataDir")

    var composed = 0
    for (locale in locales) {
        val inputDir = File(sourceMetadataDir, "$locale/images/phoneScreenshots")
        if (!inputDir.isDirectory) continue

        val outputDir = File(outputMetadataDir, "$locale/images/phoneScreenshots")
        outputDir.mkdirs()

        inputDir.listFiles { f -> f.extension.equals("png", ignoreCase = true) }
            ?.sortedBy { it.name }
            ?.forEach { rawFile ->
                val screenName = rawFile.nameWithoutExtension
                val caption = captions[screenName]?.get(locale)
                    ?: error("No caption for '$screenName' in locale '$locale' (fastlane/screenshot_captions.yml)")

                val raw = ImageIO.read(rawFile)
                val composedImage = composite(raw, caption)
                ImageIO.write(composedImage, "png", File(outputDir, rawFile.name))
                composed++
            }

        copyIfExists(File(sourceMetadataDir, "$locale/images/featureGraphic.png"), File(outputMetadataDir, "$locale/images/featureGraphic.png"))
        copyIfExists(File(sourceMetadataDir, "$locale/images/featureGraphic.svg"), File(outputMetadataDir, "$locale/images/featureGraphic.svg"))
        copyIfExists(File(sourceMetadataDir, "$locale/images/icon.png"), File(outputMetadataDir, "$locale/images/icon.png"))
        copyIfExists(File(sourceMetadataDir, "$locale/images/icon.svg"), File(outputMetadataDir, "$locale/images/icon.svg"))
    }

    println("Composed $composed screenshot(s) into $outputMetadataDir")
}

private fun copyIfExists(source: File, destination: File) {
    if (!source.isFile) return
    destination.parentFile.mkdirs()
    source.copyTo(destination, overwrite = true)
}

/**
 * Parses the narrow two-level structure used by fastlane/screenshot_captions.yml:
 *   <screen-name>:
 *     <locale>: "<caption text>"
 * This is not a general YAML parser — it only understands that one shape.
 */
private fun parseCaptions(file: File): Map<String, Map<String, String>> {
    if (!file.isFile) error("Missing caption file: $file")

    val screenKeyRegex = Regex("""^(\S+):\s*$""")
    val localeLineRegex = Regex("""^\s{2}([\w-]+):\s*"(.*)"\s*$""")

    val result = mutableMapOf<String, MutableMap<String, String>>()
    var currentScreen: String? = null

    file.readLines().forEach { rawLine ->
        val line = rawLine.substringBefore("\n")
        if (line.isBlank() || line.trimStart().startsWith("#")) return@forEach

        screenKeyRegex.matchEntire(line)?.let { match ->
            currentScreen = match.groupValues[1]
            result.getOrPut(currentScreen!!) { mutableMapOf() }
            return@forEach
        }

        localeLineRegex.matchEntire(line)?.let { match ->
            val screen = currentScreen ?: error("Caption locale line found before any screen key: $line")
            result.getOrPut(screen) { mutableMapOf() }[match.groupValues[1]] = match.groupValues[2]
        }
    }

    return result
}

private fun composite(raw: BufferedImage, caption: String): BufferedImage {
    val canvasWidth = raw.width + SIDE_MARGIN * 2
    val canvasHeight = raw.height + SIDE_MARGIN * 2 + CAPTION_BAND_HEIGHT

    // TYPE_INT_RGB, not ARGB: Play rejects screenshots with an alpha channel ("24-bit PNG, no
    // alpha"). The whole canvas is painted over an opaque BACKGROUND fill, so there is nothing to
    // be transparent — the low-alpha bloom/watermark/scanline passes still composite fine onto it.
    val canvas = BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB)
    val g = canvas.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)

    g.color = BACKGROUND
    g.fillRect(0, 0, canvasWidth, canvasHeight)

    // Phosphor glow, centered on the frame's top edge so the brightest part sits in the visible
    // caption band / side margins rather than hidden under the opaque screenshot below it.
    val glowCenter = Point2D.Float(canvasWidth * 0.5f, (CAPTION_BAND_HEIGHT + SIDE_MARGIN).toFloat())
    val glowRadius = canvasWidth * 0.9f
    g.paint = RadialGradientPaint(glowCenter, glowRadius, floatArrayOf(0f, 1f), arrayOf(DIM_GREEN, BACKGROUND))
    g.fillRect(0, 0, canvasWidth, canvasHeight)
    g.paint = null

    drawWatermarkPrompt(g, canvasWidth, canvasHeight)

    drawCaption(g, caption, canvasWidth)

    val frameX = SIDE_MARGIN - FRAME_BORDER
    val frameY = CAPTION_BAND_HEIGHT + SIDE_MARGIN - FRAME_BORDER
    val frameWidth = raw.width + FRAME_BORDER * 2
    val frameHeight = raw.height + FRAME_BORDER * 2

    // Sharp-cornered phosphor border with a soft outer glow — a CRT/DOS box, not a rounded card.
    g.stroke = BasicStroke(FRAME_BORDER * 3f)
    g.color = withAlpha(PHOSPHOR, 60)
    g.drawRect(frameX, frameY, frameWidth, frameHeight)
    g.stroke = BasicStroke(FRAME_BORDER.toFloat())
    g.color = PHOSPHOR
    g.drawRect(frameX, frameY, frameWidth, frameHeight)

    g.drawImage(raw, SIDE_MARGIN, CAPTION_BAND_HEIGHT + SIDE_MARGIN, null)

    drawScanlines(g, canvasWidth, canvasHeight)

    g.dispose()
    return canvas
}

/**
 * Traces the terminal-prompt glyph from the launcher icon (the ">" chevron plus the "_" cursor
 * underscore, in icon.svg's 0-108 coordinate space) as a large, low-alpha watermark, so the
 * screenshots share the app's visual motif instead of just a flat accent color.
 */
private fun drawWatermarkPrompt(g: Graphics2D, canvasWidth: Int, canvasHeight: Int) {
    val oldTransform = g.transform
    val oldComposite = g.composite
    val oldColor = g.color
    val oldStroke = g.stroke

    g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, WATERMARK_ALPHA)
    g.color = PHOSPHOR

    // Glyph bounding box is roughly x:[38,86] y:[32,76] (center ~62,54). Anchor that center near
    // the bottom-right margin so the glyph mostly sits on visible canvas and bleeds off the edge.
    val glyphScale = SIDE_MARGIN * 3.4 / 54.0
    val targetX = canvasWidth - 140.0
    val targetY = canvasHeight - 140.0
    g.translate((targetX - 62.0 * glyphScale).toInt(), (targetY - 54.0 * glyphScale).toInt())
    g.scale(glyphScale, glyphScale)
    g.stroke = BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

    g.draw(GeneralPath().apply {
        moveTo(38.0, 32.0); lineTo(60.0, 54.0); lineTo(38.0, 76.0)
    })
    g.draw(GeneralPath().apply {
        moveTo(68.0, 76.0); lineTo(86.0, 76.0)
    })

    g.transform = oldTransform
    g.composite = oldComposite
    g.color = oldColor
    g.stroke = oldStroke
}

/**
 * Renders the caption as a shell line — a dim-green "> " prompt, the phosphor-green caption text,
 * and a trailing block cursor — left-aligned in the caption band, with an offset low-alpha bloom
 * pass behind the crisp glyphs to fake CRT phosphor spread.
 */
private fun drawCaption(g: Graphics2D, caption: String, canvasWidth: Int) {
    g.font = CAPTION_FONT.deriveFont(CAPTION_FONT_SIZE)
    val metrics = g.fontMetrics
    val maxTextWidth = canvasWidth - SIDE_MARGIN * 2

    val lines = wrapText(caption, maxTextWidth) { text -> metrics.stringWidth(PROMPT + text) }
    val lineHeight = metrics.height * CAPTION_LINE_SPACING
    val blockHeight = lineHeight * lines.size
    var y = (CAPTION_BAND_HEIGHT - blockHeight) / 2f + metrics.ascent

    for ((index, line) in lines.withIndex()) {
        val text = if (index == 0) PROMPT + line else "  $line"
        val rendered = if (index == lines.lastIndex) "$text$CURSOR" else text
        drawGlowText(g, rendered, SIDE_MARGIN, y.toInt())
        y += lineHeight
    }
}

private fun drawGlowText(g: Graphics2D, text: String, x: Int, y: Int) {
    val oldComposite = g.composite
    g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, BLOOM_ALPHA)
    g.color = PHOSPHOR
    for (dx in -2..2 step 2) {
        for (dy in -2..2 step 2) {
            if (dx == 0 && dy == 0) continue
            g.drawString(text, x + dx, y + dy)
        }
    }
    g.composite = oldComposite

    // Crisp pass: dim-green prompt/indent, phosphor for the rest.
    val splitAt = if (text.startsWith(PROMPT)) PROMPT.length else 0
    if (splitAt > 0) {
        g.color = DIM_GREEN.brighter()
        g.drawString(text.substring(0, splitAt), x, y)
        val prefixWidth = g.fontMetrics.stringWidth(text.substring(0, splitAt))
        g.color = PHOSPHOR
        g.drawString(text.substring(splitAt), x + prefixWidth, y)
    } else {
        g.color = PHOSPHOR
        g.drawString(text, x, y)
    }
}

private fun drawScanlines(g: Graphics2D, canvasWidth: Int, canvasHeight: Int) {
    val oldColor = g.color
    val oldStroke = g.stroke
    g.color = withAlpha(Color.BLACK, (SCANLINE_ALPHA * 255).toInt())
    g.stroke = BasicStroke(1f)
    var y = 0
    while (y < canvasHeight) {
        g.drawLine(0, y, canvasWidth, y)
        y += SCANLINE_PITCH
    }
    g.color = oldColor
    g.stroke = oldStroke
}

private fun withAlpha(color: Color, alpha: Int): Color = Color(color.red, color.green, color.blue, alpha)

private fun wrapText(text: String, maxWidth: Int, widthOf: (String) -> Int): List<String> {
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var current = StringBuilder()

    for (word in words) {
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (widthOf(candidate) <= maxWidth || current.isEmpty()) {
            current = StringBuilder(candidate)
        } else {
            lines += current.toString()
            current = StringBuilder(word)
        }
    }
    if (current.isNotEmpty()) lines += current.toString()
    return lines
}
