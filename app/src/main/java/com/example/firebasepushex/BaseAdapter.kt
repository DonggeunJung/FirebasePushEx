package com.example.firebasepushex

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BaseAdapter : RecyclerView.Adapter<BaseAdapter.BaseVH>() {
    protected var list: List<*>? = null
    protected var itemLayout = -1

    fun setLayout(itemLayout: Int, itemEvent: ItemEvent?) {
        this.itemLayout = itemLayout
        this.itemEvent = itemEvent
    }

    @JvmName("setList1")
    fun setList(list: List<*>?) {
        this.list = list
        notifyDataSetChanged()
    }

    fun getData(index: Int): Any {
        return list!![index]!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseAdapter.BaseVH {
        val inflater = LayoutInflater.from(parent.context)
        val itemView: View = inflater.inflate(itemLayout, parent, false)
        return BaseVH(itemView)
    }

    override fun onBindViewHolder(holder: BaseVH, position: Int) {
        val itemView = setClickable(holder, position)
        if (itemView == null || itemEvent == null) return
        itemEvent!!.onBindViewHolder(itemView, position, getData(position))
    }

    override fun getItemCount(): Int {
        return list?.size ?: 0
    }

    protected fun setClickable(holder: BaseAdapter.BaseVH, position: Int): View {
        return holder.itemView.also{ setClickable(it, position) }
    }

    protected fun setClickable(v: View, position: Int) {
        v.tag= position
        v.setOnClickListener{
            itemEvent?.onClickItem(it.tag as Int)
        }
    }

    inner class BaseVH(itemView: View) : RecyclerView.ViewHolder(itemView)

    interface ItemEvent {
        fun onClickItem(index: Int)
        fun onBindViewHolder(v: View, index: Int, data: Any)
    }

    var itemEvent: ItemEvent? = null

    companion object {
        fun makeInstance(rv: RecyclerView, layoutItem: Int, itemEvent: ItemEvent): BaseAdapter {
            val layoutManager = LinearLayoutManager(itemEvent as Context)
            rv.layoutManager = layoutManager
            val dividerItemDecoration = DividerItemDecoration(rv.context, layoutManager.orientation)
            rv.addItemDecoration(dividerItemDecoration)
            return BaseAdapter().also{
                it.setLayout(layoutItem, itemEvent)
                rv.adapter=it
            }
        }
    }
}