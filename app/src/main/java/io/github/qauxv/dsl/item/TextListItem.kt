package io.github.qauxv.dsl.item

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.qauxv.dsl.cell.TitleValueCell

class TextListItem(
        private val title: String,
        private val summary: String? = null,
        private val value: String? = null,
        private val onClick: View.OnClickListener? = null
) : DslTMsgListItemInflatable, TMsgListItem {

    class ViewHolder(cell: TitleValueCell) : RecyclerView.ViewHolder(cell)

    override val isEnabled = true
    override val isVoidBackground = false
    override val isClickable = true
    override val isLongClickable = false

    override fun createViewHolder(context: Context, parent: ViewGroup) =
            ViewHolder(TitleValueCell(context))

    private val mCellOnClickListener = View.OnClickListener {
        onItemClick(it, -1, -1, -1)
    }

    override fun bindView(viewHolder: RecyclerView.ViewHolder, position: Int, context: Context) {
        val cell = viewHolder.itemView as TitleValueCell
        cell.setOnClickListener(mCellOnClickListener)
        cell.isHasSwitch = false
        cell.title = title
        cell.summary = summary
        cell.value = value
    }

    override fun onItemClick(v: View, position: Int, x: Int, y: Int) {
        onClick?.onClick(v)
    }
}
