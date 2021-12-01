package bme.mobweb.lab.sudoku

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import bme.mobweb.lab.sudoku.adapter.PuzzleAdapter
import bme.mobweb.lab.sudoku.databinding.FragmentSelectBinding
import bme.mobweb.lab.sudoku.model.Puzzle
import java.lang.RuntimeException

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SelectFragment : Fragment(), PuzzleAdapter.PuzzleListItemListener {
    private lateinit var adapter : PuzzleAdapter
    private lateinit var holder : PuzzleListHolder
    private var _binding: FragmentSelectBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)         // Important to handle navbar events!
    }

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context !is PuzzleListHolder) {
            throw RuntimeException("Context doesn't implement PuzzleListHolder.")
        }
        holder = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSelectBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        adapter.update(holder.getPuzzleList())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initRecyclerView() {
        adapter = PuzzleAdapter(this)
        binding.rvMain.layoutManager = LinearLayoutManager(binding.root.context)
        binding.rvMain.adapter = adapter
        adapter.update(holder.getPuzzleList())
    }

    override fun onItemChanged(item: Puzzle) {
        TODO("Not yet implemented")
    }

    override fun onItemRemoved(item: Puzzle) {
        holder.removePuzzle(item)
    }

    override fun onItemSelectClicked(item: Puzzle) {
        holder.setSelectedPuzzle(item)
        findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings ->
            {
                findNavController().navigate(R.id.action_SecondFragment_to_settingsFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    interface PuzzleListHolder {
        fun getPuzzleList() : List<Puzzle>
        fun setSelectedPuzzle(puzzle : Puzzle)
        fun removePuzzle(puzzle : Puzzle)
        fun setUpdateAdapterFunction(f : (puzzles : List<Puzzle>) -> Unit)
    }
}