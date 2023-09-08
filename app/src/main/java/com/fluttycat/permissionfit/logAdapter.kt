package com.fluttycat.permissionfit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.fluttycat.permissionfit.databinding.LogItemLayoutBinding


class LogsAdapter(private val itemClickCallback: (String) -> Unit) :
    ListAdapter<String, BaseHolder<String>>(object :
        DiffUtil.ItemCallback<String>() {

        override fun areItemsTheSame(
            oldItem: String,
            newItem: String
        ): Boolean=true

        override fun areContentsTheSame(
            oldItem: String,
            newItem: String
        ): Boolean {
            return oldItem == newItem
        }

    }) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseHolder<String> {

        return ArticleViewHolder(
            LogItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }


    override fun onBindViewHolder(holder: BaseHolder<String>, position: Int) {
        holder.bind(getItem(position)!!, position)
    }


    inner class ArticleViewHolder(
        private val binding: LogItemLayoutBinding
    ) : BaseHolder<String>(binding) {

        override fun bind(value: String, position: Int) {
            binding.root.setOnClickListener {
                itemClickCallback(value)
            }
        }
    }
}