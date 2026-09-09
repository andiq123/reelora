package tv.reelora.app

/** A new surface must see its own initial press before accepting a release. */
internal class RemotePressGate {
    private var pressedKey: Int? = null
    private var pressedAt = 0L

    fun consume(keyCode: Int, down: Boolean, repeatCount: Int, downTime: Long, ready: Boolean = true): Boolean {
        if (down) {
            if (repeatCount != 0 || !ready) return true
            pressedKey = keyCode
            pressedAt = downTime
            return false
        }
        val accepted = ready && pressedKey == keyCode && pressedAt == downTime
        pressedKey = null
        return !accepted
    }
}
