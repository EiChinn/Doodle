package cn.hzw.doodledemo

import cn.hzw.doodle.core.IDoodle

data class ChangePenAttrEvent(
        val paintSize: Float,
        val paintLineType: IDoodle.LineType
)