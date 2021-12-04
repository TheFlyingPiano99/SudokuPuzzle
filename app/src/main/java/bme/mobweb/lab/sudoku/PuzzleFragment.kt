package bme.mobweb.lab.sudoku

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.navigation.fragment.findNavController
import bme.mobweb.lab.sudoku.customView.PuzzleView
import bme.mobweb.lab.sudoku.databinding.FragmentPuzzleBinding
import bme.mobweb.lab.sudoku.model.Puzzle
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
    private var isForeground = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)         // Important to handle navbar events!
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context !is PuzzleHolder) {
            throw RuntimeException("Context doesn't implement PuzzleHolder!")
        }
        handler = context
        handler.setInvalidateViewFunction { invalidateView() }
        handler.setShakeTable { shakeTable() }
        handler.setSpinTableFunction { spinTable() }
        handler.setDoNumberMagicAnimationFunction { doNumberMagicAnimation() }
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
        }
        binding.selectPuzzleButton.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
        binding.initNewPuzzleButton.setOnClickListener {
            handler.initNewPuzzle()
        }
        binding.hintButton.setOnClickListener {
            handler.giveHint()
        }

    }

    override fun onResume() {
        super.onResume()
        isForeground = true
        handler.continueSolving()
    }

    override fun onPause() {
        super.onPause()
        isForeground = false
        handler.breakSolving()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface PuzzleHolder {
        fun solveCurrentPuzzle()
        fun giveHint()
        fun getCurrentPuzzle(): Puzzle?
        fun clearCurrentPuzzle()
        fun getNotifiedAboutSelection(row: Int, column: Int, view: View)
        fun setInvalidateViewFunction(f : () -> Unit)
        fun setSpinTableFunction(f : () -> Unit)
        fun setDoNumberMagicAnimationFunction(f : () -> Unit)
        fun setShakeTable(f : () -> Unit)
        fun initNewPuzzle()
        fun continueSolving()
        fun breakSolving()
    }

    override fun getPuzzle() : Puzzle? {
        return handler.getCurrentPuzzle()
    }

    override fun onSelection(row: Int, column: Int) {
        handler.getNotifiedAboutSelection(row, column, binding.puzzleView)
    }

    private fun invalidateView() {
        if (isForeground) {
            binding.puzzleView.invalidate()
        }
    }

    private fun shakeTable() {
        if (isForeground) {
            val animation = SpringAnimation(binding.puzzleView, DynamicAnimation.TRANSLATION_X, 0f)
            animation.setStartValue(50f)
            animation.spring.dampingRatio = SpringForce.DAMPING_RATIO_HIGH_BOUNCY
            animation.spring.stiffness = SpringForce.STIFFNESS_HIGH
            animation.start()
        }
    }

    private fun spinTable() {
        if (isForeground) {
            val animation = SpringAnimation(binding.puzzleView, DynamicAnimation.ROTATION, 0f)
            animation.setStartValue(180f)
            animation.spring.dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
            animation.spring.stiffness = SpringForce.STIFFNESS_LOW
            animation.start()
        }
    }

    private fun doNumberMagicAnimation() {
        if (isForeground) {
            val animationX = SpringAnimation(binding.puzzleView, DynamicAnimation.SCALE_X, 1f)
            animationX.setStartValue(0.1f)
            val animationY = SpringAnimation(binding.puzzleView, DynamicAnimation.SCALE_Y, 1f)
            animationY.setStartValue(0.1f)
            animationX.start()
            animationY.start()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings ->
            {
                findNavController().navigate(R.id.action_FirstFragment_to_settingsFragment)
                true
            }
            R.id.action_help ->
            {
                findNavController().navigate(R.id.action_FirstFragment_to_helpFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}