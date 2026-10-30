package com.mandu.liquiddialer.telecom

import android.view.Surface

/** Surfaces ko provider (VideoCall) se alag rakhta hai: provider call ke baad me available ho sakta
 *  hai ya replace ho sakta hai. bind par current surfaces turant attach hote hain; surface baad me
 *  bane to bound provider ko turant push hota hai. Call khatam par unbind sab saaf karta hai. */
object VideoLink {
    var display: Surface? = null; private set
    var preview: Surface? = null; private set
    private var push: ((Surface?, Surface?) -> Unit)? = null

    @Synchronized fun setDisplay(s: Surface?) { display = s; try { push?.invoke(display, preview) } catch (_: Throwable) { } }
    @Synchronized fun setPreview(s: Surface?) { preview = s; try { push?.invoke(display, preview) } catch (_: Throwable) { } }

    /** Provider available/replaced: abhi ke surfaces turant is par attach karo. */
    @Synchronized fun bind(p: (Surface?, Surface?) -> Unit) { push = p; try { p(display, preview) } catch (_: Throwable) { } }

    /** Call gayi: provider ko null surfaces push karo, refs saaf. Surface release uska TextureView karega. */
    @Synchronized fun unbind() {
        val p = push; push = null
        try { p?.invoke(null, null) } catch (_: Throwable) { }
        display = null; preview = null
    }
}
