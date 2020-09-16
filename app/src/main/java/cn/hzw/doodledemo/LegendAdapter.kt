package cn.hzw.doodledemo

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class LegendAdapter(data: MutableList<Legend>) : BaseQuickAdapter<Legend, BaseViewHolder>(R.layout.item_legend, data) {
    override fun convert(holder: BaseViewHolder, item: Legend) {
        holder.setText(R.id.legend_tv, item.symbol)

    }
}