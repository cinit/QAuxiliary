package io.github.qauxv.dsl.item

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.qauxv.dsl.cell.TextBannerCell

class TextBannerItem(
    private val text: String,
    private val onClick: View.OnClickListener? = null,
    private val isCloseable: Boolean = true,
    private val onClose: View.OnClickListener? = null
) : DslTMsgListItemInflatable, TMsgListItem {

    class ViewHolder(cell: TextBannerCell) : RecyclerView.ViewHolder(cell)

    override val isEnabled = true
    override val isVoidBackground = false
    override val isClickable = true
    override val isLongClickable = false

    private val mOnBodyClickListener: (TextBannerCell) -> Unit = {
        onClick?.onClick(it)
    }

    private val mOnCloseClickListener: (TextBannerCell) -> Unit = {
        onClose?.onClick(it)
        it.isClosed = true
    }

    override fun createViewHolder(context: Context, parent: ViewGroup) =
        ViewHolder(TextBannerCell(context))

    override fun bindView(viewHolder: RecyclerView.ViewHolder, position: Int, context: Context) {
        val cell = viewHolder.itemView as TextBannerCell
        cell.onBodyClickListener = mOnBodyClickListener
        cell.onCloseClickListener = mOnCloseClickListener
        cell.isCloseButtonVisible = isCloseable
        cell.text = text
        cell.isClickable = true
        cell.isFocusable = true
        cell.setOnClickListener {
            cell.handleClick(cell.lastDownX, cell.lastDownY)
        }
    }

    override fun onItemClick(v: View, position: Int, x: Int, y: Int) {
        onClick?.onClick(v)
    }

}
