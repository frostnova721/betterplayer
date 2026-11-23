package com.jhomlala.better_player

import android.os.Build
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.mediacodec.MediaCodecDecoderException
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer

/**
 * Builds a detailed, single-string description of a [PlaybackException].
 *
 * Only the error message reaches the Dart side (as `errorDescription`), and
 * `PlaybackException.toString()` omits the cause chain, the decoder that failed
 * and the device. Those are what's needed to diagnose device-specific
 * failures reported by users, so they are all included here.
 */
@UnstableApi
internal object PlaybackErrorFormatter {

    private const val MAX_CAUSE_DEPTH = 8

    fun format(error: PlaybackException): String {
        val sb = StringBuilder("Video player had error ").append(error)
        sb.append("\n  errorCode=").append(error.errorCodeName)

        if (error is ExoPlaybackException && error.type == ExoPlaybackException.TYPE_RENDERER) {
            sb.append("\n  renderer=").append(error.rendererName)
        }

        sb.append("\n  device=").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
            .append(" (hardware=").append(Build.HARDWARE)
            .append(", sdk=").append(Build.VERSION.SDK_INT).append(')')

        var cause: Throwable? = error.cause
        var depth = 0
        while (cause != null && depth < MAX_CAUSE_DEPTH) {
            sb.append("\n  caused by: ").append(cause.javaClass.name)
                .append(": ").append(cause.message)
            appendDecoderDetails(sb, cause)
            cause = cause.cause
            depth++
        }
        return sb.toString()
    }

    private fun appendDecoderDetails(sb: StringBuilder, t: Throwable) {
        when (t) {
            is MediaCodecRenderer.DecoderInitializationException -> {
                // Walk the fallback chain so every decoder that was tried is listed.
                var e: MediaCodecRenderer.DecoderInitializationException? = t
                while (e != null) {
                    sb.append("\n    decoder init failed: ")
                        .append(e.codecInfo?.name ?: "<none found>")
                        .append(" (mime=").append(e.mimeType)
                        .append(", secure=").append(e.secureDecoderRequired)
                        .append(", diagnostic=").append(e.diagnosticInfo).append(')')
                    e = e.fallbackDecoderInitializationException
                }
            }
            is MediaCodecDecoderException -> {
                sb.append("\n    decoder failed: ").append(t.codecInfo?.name)
                    .append(" (errorCode=").append(t.errorCode)
                    .append(", diagnostic=").append(t.diagnosticInfo).append(')')
            }
        }
    }
}
