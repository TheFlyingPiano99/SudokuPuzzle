package bme.mobweb.lab.sudoku.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import bme.mobweb.lab.sudoku.databinding.PuzzleListBinding
import bme.mobweb.lab.sudoku.model.Puzzle
import java.lang.StringBuilder

class PuzzleAdapter(private val listener: PuzzleListItemClickListener) :
    RecyclerView.Adapter<PuzzleAdapter.PuzzleViewHolder>() {

    private val items = mutableListOf<Puzzle>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PuzzleViewHolder(
        PuzzleListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: PuzzleViewHolder, position: Int) {
        val item = items[position]

        holder.binding.timeCreatedTextView.text = StringBuilder("Created:\n").append(item.timeCreated.toString()).toString()
        holder.binding.deleteButton.setOnClickListener { _ ->
            val n = items.indexOf(item)
            items.remove(item)
            listener.onItemRemoved(item)
            notifyItemRemoved(n)
        }
        holder.binding.selectButton.setOnClickListener {
            listener.onItemSelectClicked(item)
        }
        /*
        holder.binding.ivIcon.setImageResource(getImageResource(puzzle.category))
        holder.binding.cbIsBought.isChecked = puzzle.isBought
        holder.binding.tvDescription.text = puzzle.description
        holder.binding.tvCategory.text = puzzle.category.name
        holder.binding.tvPrice.text = "${puzzle.estimatedPrice} Ft"
        */

        /*
            holder.binding.cbIsBought.setOnCheckedChangeListener { buttonView, isChecked ->
                puzzle.isBought = isChecked
                listener.onItemChanged(puzzle)
            }
            holder.binding.ibRemove.setOnClickListener {
                    buttonView ->
                val n = items.indexOf(puzzle)
                items.remove(puzzle)
                listener.onItemRemoved(puzzle)
                notifyItemRemoved(n)
            }
         */
    }

    /*
    @DrawableRes()
    private fun getImageResource(category: ShoppingItem.Category): Int {
        return when (category) {
            Puzzle.Category.FOOD -> R.drawable.groceries
            Puzzle.Category.ELECTRONIC -> R.drawable.lightning
            Puzzle.Category.BOOK -> R.drawable.open_book
        }
    }
    * */

    override fun getItemCount(): Int = items.size

    interface PuzzleListItemClickListener {
        fun onItemChanged(item : Puzzle)
        fun onItemRemoved(item : Puzzle)
        fun onItemSelectClicked(item : Puzzle)
    }

    inner class PuzzleViewHolder(val binding: PuzzleListBinding) : RecyclerView.ViewHolder(binding.root)

    fun addItem(item: Puzzle) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun update(puzzles: List<Puzzle>) {
        items.clear()
        items.addAll(puzzles)
        notifyDataSetChanged()
    }
}