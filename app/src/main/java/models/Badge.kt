package models

class Badge(
    private val name: String,
    private val points: String,
    private val iconRes: Int,
    private val isUnlocked: Boolean
) {
    fun getName(): String {
        return name
    }

    fun getPoints(): String {
        return points
    }

    fun getIconRes(): Int {
        return iconRes
    }

    fun isUnlocked(): Boolean {
        return isUnlocked
    }
}
