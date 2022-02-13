package io.github.qauxv.dsl.item

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.RecyclerView
import io.github.qauxv.base.ISwitchCellAgent
import io.github.qauxv.dsl.cell.TitleValueCell

class TextSwitchItem(
        private val title: String,
        private val summary: String?,
        private val switchAgent: ISwitchCellAgent
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

    private val mOnSwitchChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        switchAgent.isChecked = isChecked
    }

    override fun bindView(viewHolder: RecyclerView.ViewHolder, position: Int, context: Context) {
        val cell = viewHolder.itemView as TitleValueCell
        cell.isHasSwitch = true
        // reset listener to avoid things messy
        cell.switchView.setOnCheckedChangeListener(null)
        cell.switchView.isChecked = switchAgent.isChecked
        cell.switchView.isEnabled = switchAgent.isCheckable
        cell.switchView.setOnCheckedChangeListener(mOnSwitchChangeListener)
        cell.setOnClickListener(mCellOnClickListener)
        cell.title = title
        cell.summary = summary
    }

    override fun onItemClick(v: View, position: Int, x: Int, y: Int) {
        val cell = v as TitleValueCell
        val switchView = cell.switchView
        if (switchView.isEnabled) {
            switchView.toggle()
        }
    }
}
