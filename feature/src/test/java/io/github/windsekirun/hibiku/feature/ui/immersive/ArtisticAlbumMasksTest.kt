package io.github.windsekirun.hibiku.feature.ui.immersive

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtisticAlbumMasksTest {

    private val density = Density(1f)
    private val layoutDirection = LayoutDirection.Ltr
    private val testSize = Size(300f, 300f)

    @Test
    fun immersiveShapeStyles_containAllFourRequiredStyles() {
        val styles = ImmersiveShapeStyle.entries
        assertEquals(4, styles.size)
        assertTrue(styles.contains(ImmersiveShapeStyle.FIGURE_8))
        assertTrue(styles.contains(ImmersiveShapeStyle.SQUIRCLE))
        assertTrue(styles.contains(ImmersiveShapeStyle.VINYL))
        assertTrue(styles.contains(ImmersiveShapeStyle.SCALLOP))
    }

    @Test
    fun getShapeForStyle_mapsCorrectShapes() {
        assertTrue(getShapeForStyle(ImmersiveShapeStyle.FIGURE_8) is Figure8Shape)
        assertTrue(getShapeForStyle(ImmersiveShapeStyle.SQUIRCLE) is SquircleShape)
        assertEquals(CircleShape, getShapeForStyle(ImmersiveShapeStyle.VINYL))
        assertTrue(getShapeForStyle(ImmersiveShapeStyle.SCALLOP) is ScallopShape)
    }

    @Test
    fun squircleShape_createsValidOutline() {
        val shape = SquircleShape(28)
        val outline = shape.createOutline(testSize, layoutDirection, density)
        assertNotNull(outline)
    }

    @Test
    fun calculateScallopSegments_generatesExact12ConnectedSegments() {
        val cx = 150f
        val cy = 150f
        val innerRadius = 120f
        val outerRadius = 150f

        val segments = calculateScallopSegments(
            points = 12,
            cx = cx,
            cy = cy,
            innerRadius = innerRadius,
            outerRadius = outerRadius
        )

        assertEquals("Should have exactly 12 segments for 12 points", 12, segments.size)

        // Verify continuity: segment i end should match segment i+1 start
        for (i in 0 until segments.size - 1) {
            assertEquals(
                "Segment $i endX should match segment ${i + 1} startX",
                segments[i].endX,
                segments[i + 1].startX,
                0.001f
            )
            assertEquals(
                "Segment $i endY should match segment ${i + 1} startY",
                segments[i].endY,
                segments[i + 1].startY,
                0.001f
            )
        }

        // Verify loop closure: last segment end should match first segment start
        assertEquals(
            "Last segment endX should match first segment startX",
            segments.last().endX,
            segments.first().startX,
            0.001f
        )
        assertEquals(
            "Last segment endY should match first segment startY",
            segments.last().endY,
            segments.first().startY,
            0.001f
        )
    }

    @Test
    fun calculateScallopSegments_handlesZeroOrNegativePoints() {
        val emptySegments = calculateScallopSegments(0, 100f, 100f, 80f, 100f)
        assertTrue(emptySegments.isEmpty())

        val negativeSegments = calculateScallopSegments(-5, 100f, 100f, 80f, 100f)
        assertTrue(negativeSegments.isEmpty())
    }
}
