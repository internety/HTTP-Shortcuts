package ch.rmy.android.http_shortcuts.extensions

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.utils.CrashReporting
import ch.rmy.android.http_shortcuts.utils.Destroyable
import ch.rmy.android.http_shortcuts.utils.Destroyer
import ch.rmy.android.http_shortcuts.utils.showIfPossible
import com.afollestad.materialdialogs.MaterialDialog
import io.reactivex.Completable
import io.reactivex.disposables.Disposable
import org.jdeferred2.Deferred
import org.jdeferred2.DoneFilter
import org.jdeferred2.FailFilter
import org.jdeferred2.ProgressFilter
import org.jdeferred2.Promise
import org.jdeferred2.impl.DeferredObject

fun Fragment.showMessageDialog(@StringRes stringRes: Int) {
    MaterialDialog.Builder(context!!)
        .content(stringRes)
        .positiveText(R.string.dialog_ok)
        .showIfPossible()
}

@ColorInt
fun color(context: Context, @ColorRes colorRes: Int): Int = ContextCompat.getColor(context, colorRes)

fun drawable(context: Context, @DrawableRes drawableRes: Int): Drawable? = ContextCompat.getDrawable(context, drawableRes)

fun Activity.dimen(@DimenRes dimenRes: Int) = dimen(this, dimenRes)
fun dimen(context: Context, @DimenRes dimenRes: Int) = context.resources.getDimensionPixelSize(dimenRes)

inline fun consume(f: () -> Unit): Boolean {
    f()
    return true
}

inline fun <T> T.mapIf(predicate: Boolean, block: (T) -> T): T = if (predicate) block(this) else this

inline fun <T, U> T.mapFor(iterable: Iterable<U>, block: (T, U) -> T): T {
    val iterator = iterable.iterator()
    var item = this
    while (iterator.hasNext()) {
        item = block.invoke(item, iterator.next())
    }
    return item
}

fun Any.logException(e: Throwable) {
    if (CrashReporting.enabled) {
        CrashReporting.logException(e)
    } else {
        Log.e(this.javaClass.simpleName, "An error occurred", e)
    }
}

fun Context.showToast(message: String, long: Boolean = false) {
    Toast.makeText(this, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

fun Context.showToast(@StringRes message: Int, long: Boolean = false) {
    Toast.makeText(this, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

fun <T, U, F, P> Promise<T, F, P>.filter(filter: (T) -> U) = this.then(DoneFilter<T, U> { result -> filter(result) }, null as FailFilter<F, F>?, null as ProgressFilter<P, P>?)!!

fun <T, U, V> Deferred<T, U, V>.rejectSafely(reject: U): Deferred<T, U, V> =
    if (isPending) {
        reject(reject)
    } else {
        this
    }

fun <T, U, V> Deferred<T, U, V>.resolveSafely(resolve: T): Deferred<T, U, V> =
    if (isPending) {
        resolve(resolve)
    } else {
        this
    }

/*
fun CurlCommand.applyToShortcut(shortcut: Shortcut) {
    shortcut.url = url
    shortcut.method = method
    shortcut.bodyContent = data
    shortcut.requestBodyType = Shortcut.REQUEST_BODY_TYPE_CUSTOM_TEXT
    shortcut.username = username
    shortcut.password = password
    if (!username.isNullOrEmpty() || !password.isNullOrEmpty()) {
        shortcut.authentication = Shortcut.AUTHENTICATION_BASIC
    }
    if (timeout != 0) {
        shortcut.timeout = timeout
    }
    for ((key, value) in headers) {
        if (key.equals(HttpHeaders.CONTENT_TYPE, ignoreCase = true)) {
            shortcut.contentType = value
        } else {
            shortcut.headers.add(Header.createNew(key, value))
        }
    }
}*/

fun <T> Array<T>.findIndex(item: T) =
    indices.firstOrNull { this[it] == item } ?: 0

fun Disposable.toDestroyable() = object : Destroyable {
    override fun destroy() {
        dispose()
    }
}

fun Disposable.attachTo(destroyer: Destroyer) {
    destroyer.own { dispose() }
}

fun Completable.toPromise(): Promise<Unit, Throwable, Unit> {
    val deferred = DeferredObject<Unit, Throwable, Unit>()
    subscribe({
        deferred.resolveSafely(Unit)
    }, {
        deferred.rejectSafely(it)
    })
    return deferred.promise()
}