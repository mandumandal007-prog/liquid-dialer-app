package com.mandu.liquiddialer

import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Widget night-mode shots: values-night colors load hone chahiye. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "night")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FavWidgetNightShotTest {

    private val day = FavWidgetTest()

    @Test fun widgetShot4FavoritesNight() {
        val v = day.inflateAndBind(720, listOf("Asha", "Bittu", "Chandni", "Durgeshwari"), null)
        day.saveViewShot(v, "W4-widget-4fav-night")
    }

    @Test fun widgetShotEmptyHintNight() {
        val v = day.inflateAndBind(720, emptyList(), "Favorites add karo")
        day.saveViewShot(v, "W5-widget-empty-night")
    }
}
