package bme.mobweb.lab.sudoku

import android.content.Context
import android.os.Bundle
import android.text.Editable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import bme.mobweb.lab.sudoku.customView.PuzzleView
import bme.mobweb.lab.sudoku.databinding.FragmentPuzzleBinding
import bme.mobweb.lab.sudoku.model.Solver
import com.google.android.material.snackbar.Snackbar
import java.lang.RuntimeException

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class PuzzleFragment : Fragment(), PuzzleView.PuzzleDataProvider {

    private companion object {
        private const val TAG = "PuzzleFragment"
    }
    private var _binding: FragmentPuzzleBinding? = null
    private lateinit var handler : PuzzleHolder

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context !is PuzzleHolder) {
            throw RuntimeException("Context doesn't implement PuzzleHolder!")
        }
        handler = context
        handler.setInvalidateViewFunction { invalidateView() }
    }

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentPuzzleBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.puzzleView.dataProvider = this
        binding.solveButton.setOnClickListener {
            handler.solveCurrentPuzzle()
            //findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        binding.initNewPuzzleButton.setOnClickListener {
                view ->
            handler.initNewPuzzle()
            Snackbar.make(view, "New puzzle created.", Snackbar.LENGTH_SHORT).show()
            invalidateView()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface PuzzleHolder {
        fun solveCurrentPuzzle()
        fun initNewPuzzle()
        fun getFieldOfCurrentPuzzle(row: Int, column: Int): Int
        fun setFieldOfCurrentPuzzle(row: Int, column: Int, value: Int)
        fun getValidityOfFieldOfCurrentPuzzle(row: Int, column: Int): Boolean
        fun clearCurrentPuzzle()
        fun getNotifiedAboutSelection(row: Int, column: Int, view: View)
        fun setInvalidateViewFunction(f : () -> Unit)
    }

    override fun getValueAtLocation(row: Int, column: Int) : Int {
        return handler.getFieldOfCurrentPuzzle(row, column)
    }

    override fun getValidityAtLocation(row: Int, column: Int): Boolean {
        return handler.getValidityOfFieldOfCurrentPuzzle(row, column)
    }

    override fun getNotifiedAboutSelection(row: Int, column: Int) {
        handler.getNotifiedAboutSelection(row, column, binding.puzzleView)
    }

    private fun invalidateView() {
        binding.puzzleView.invalidate()
    }

}