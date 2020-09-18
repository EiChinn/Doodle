package cn.hzw.doodledemo

data class Legend(
        @JvmField var symbol: String = "",
        @JvmField var name: String = "",
        @JvmField val createTime: Long = System.currentTimeMillis()
) {


    override fun toString(): String {
        return symbol
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Legend

        if (symbol != other.symbol) return false

        return true
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }
}