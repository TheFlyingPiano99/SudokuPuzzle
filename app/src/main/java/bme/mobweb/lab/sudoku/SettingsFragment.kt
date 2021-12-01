package bme.mobweb.lab.sudoku

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceFragmentCompat
import bme.mobweb.lab.sudoku.databinding.FragmentPuzzleBinding
import bme.mobweb.lab.sudoku.databinding.FragmentSettingsBinding
import bme.mobweb.lab.sudoku.model.Settings
import java.lang.RuntimeException

class SettingsFragment : Fragment() {
    private var _binding : FragmentSettingsBinding? = null
    private lateinit var listener : SettingsListener

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context !is SettingsListener) {
            throw RuntimeException("Context doesn't implement SettingsListener!")
        }
        listener = context
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val settings = listener.getSettings()
        binding.darkModeSwitch.isChecked = settings.darkTheme
        binding.darkModeSwitch.setOnCheckedChangeListener {
            _, newValue -> listener.onDarkSettingChanged(newValue)
        }
        binding.hintSwitch.isChecked = settings.hints
        binding.hintSwitch.setOnCheckedChangeListener {
            _, newValue -> listener.onHintSettingChanged(newValue)
        }
        binding.deleteAllPuzzlesButton.setOnClickListener {
            listener.onDeletePuzzles()
        }
    }

    interface SettingsListener {
        fun getSettings() : Settings
        fun onHintSettingChanged(newValue : Boolean)
        fun onDarkSettingChanged(newValue : Boolean)
        fun onDeletePuzzles()
    }

}