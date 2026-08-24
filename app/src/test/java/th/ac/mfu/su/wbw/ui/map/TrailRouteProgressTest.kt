package th.ac.mfu.su.wbw.ui.map

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The route-progress geometry, which is the one part of the walk that cannot be checked by
 * looking at the screen — the alternative is walking eight kilometres with a debugger.
 *
 * The fixtures are synthetic rather than the real GPX. A straight line and a loop of known
 * dimensions make the expected answers arithmetic rather than "whatever the track happens
 * to do near point 412", and every property under test here — monotonicity, the off-route
 * cutoff, the loop disambiguation — is about the algorithm, not about the event's trail.
 */
class TrailRouteProgressTest {

    /** Metres per degree of latitude, matching [TrailRoute]'s own constant. */
    private val mPerDeg = 111_320.0

    /** A due-north line from the equator/prime meridian, [metres] long, sampled every 10 m. */
    private fun straightRoute(metres: Double): TrailRoute {
        val step = 10.0
        val n = (metres / step).toInt() + 1
        val points = (0 until n).map { LatLng(it * step / mPerDeg, 0.0) }
        return TrailRoute(points, metres.toInt())
    }

    /** Offsets a position [north] metres up the line and [east] metres to its side. */
    private fun at(north: Double, east: Double, route: TrailRoute): LatLng {
        val lat = north / mPerDeg
        // cos(lat) is ~1 this close to the equator, so a metre east is a metre east.
        return LatLng(lat, east / mPerDeg)
    }

    @Test
    fun `first fix acquires anywhere on the route`() {
        val route = straightRoute(1_000.0)
        // Negative means "no previous position" — a walker may well start halfway round.
        val p = route.progressFrom(-1.0, at(400.0, 0.0, route).latitude, 0.0)
        assertNotNull(p)
        assertEquals(400.0, p!!, 15.0)
    }

    @Test
    fun `progress advances as the walker moves along the line`() {
        val route = straightRoute(1_000.0)
        var m = -1.0
        for (target in listOf(100.0, 250.0, 600.0, 900.0)) {
            m = route.progressFrom(m, at(target, 0.0, route).latitude, 0.0)!!
            assertEquals(target, m, 15.0)
        }
    }

    @Test
    fun `a position off the trail reports nothing rather than a wrong number`() {
        val route = straightRoute(1_000.0)
        // 300 m to the side is well beyond the 120 m off-route tolerance.
        val p = route.progressFrom(400.0, at(400.0, 300.0, route).latitude, 300.0 / mPerDeg)
        assertNull(p)
    }

    @Test
    fun `jitter backwards does not drag progress down`() {
        val route = straightRoute(1_000.0)
        val settled = route.progressFrom(-1.0, at(500.0, 0.0, route).latitude, 0.0)!!
        // A fix a few metres behind — ordinary GPS noise, not somebody turning round.
        val after = route.progressFrom(settled, at(492.0, 0.0, route).latitude, 0.0)!!
        assertTrue("progress went backwards: $settled -> $after", after >= settled)
    }

    /**
     * The case the windowed search exists for.
     *
     * On a loop the finish passes close to the start, so a walker near the end is
     * genuinely near both the 0 km mark and the 8 km mark. A plain nearest-point search
     * picks whichever is a hair closer and resets a nearly-finished walk to nothing.
     */
    @Test
    fun `near the end of a loop, progress does not snap back to the start`() {
        // A square loop 400 m on a side that returns to within 20 m of its own start.
        val side = 400.0
        val pts = mutableListOf<LatLng>()
        var d = 0.0
        while (d <= side) { pts += LatLng(d / mPerDeg, 0.0); d += 10.0 }          // north
        d = 0.0
        while (d <= side) { pts += LatLng(side / mPerDeg, d / mPerDeg); d += 10.0 } // east
        d = side
        while (d >= 0.0) { pts += LatLng(d / mPerDeg, side / mPerDeg); d -= 10.0 }  // south
        d = side
        while (d >= 20.0) { pts += LatLng(0.0, d / mPerDeg); d -= 10.0 }            // back west
        val route = TrailRoute(pts, 1_580)

        val nearEnd = route.lengthMetres - 30.0
        // A fix at the very end of the loop, which sits 20 m from the route's own start.
        val last = pts.last()
        val m = route.progressFrom(nearEnd, last.latitude, last.longitude)
        assertNotNull(m)
        assertTrue("snapped back to the start: $m of ${route.lengthMetres}", m!! > route.lengthMetres - 100.0)
    }

    @Test
    fun `splitAt cuts the route into two halves that meet`() {
        val route = straightRoute(1_000.0)
        val (walked, remaining) = route.splitAt(400.0)
        assertTrue(walked.size >= 2)
        assertTrue(remaining.size >= 2)
        // The cut point is shared, so the drawn lines join rather than leaving a gap.
        assertEquals(walked.last().latitude, remaining.first().latitude, 1e-9)
        assertEquals(walked.last().longitude, remaining.first().longitude, 1e-9)
        assertEquals(400.0, walked.last().latitude * mPerDeg, 15.0)
    }

    @Test
    fun `splitAt clamps at both ends instead of throwing`() {
        val route = straightRoute(1_000.0)
        val (noneWalked, allLeft) = route.splitAt(-50.0)
        assertTrue(allLeft.size >= 2)
        assertEquals(0.0, noneWalked.last().latitude * mPerDeg, 1.0)

        val (allWalked, noneLeft) = route.splitAt(5_000.0)
        assertTrue(allWalked.size >= 2)
        assertEquals(1_000.0, allWalked.last().latitude * mPerDeg, 15.0)
        assertTrue(noneLeft.size <= 2)
    }
}
