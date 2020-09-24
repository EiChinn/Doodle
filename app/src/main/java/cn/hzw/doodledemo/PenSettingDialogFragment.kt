package cn.hzw.doodledemo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import cn.hzw.doodle.core.IDoodle
import cn.hzw.doodledemo.databinding.PopupPaintSettingBinding
import org.greenrobot.eventbus.EventBus

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val CURRENT_SIZE = "currentSize"
private const val CURRENT_LINE_TYPE = "currentLineType"

/**
 * A simple [Fragment] subclass.
 * Use the [PenSettingDialogFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PenSettingDialogFragment : DialogFragment() {
    // TODO: Rename and change types of parameters
    private var currentSize: Float = 1.0f
    private var currentLineType: Int = 0

    private var _binding: PopupPaintSettingBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentSize = it.getFloat(CURRENT_SIZE)
            currentLineType = it.getInt(CURRENT_LINE_TYPE)
        }
    }

    override fun onStart() {
        super.onStart()
        val screenWidth = 1000
        dialog?.window?.setLayout(screenWidth, ViewGroup.MarginLayoutParams.WRAP_CONTENT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        _binding = PopupPaintSettingBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.paintSizeSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                           fromUser: Boolean) {
                binding.paintSizeText.text = progress.toString()
            }
        })
        binding.paintSizeSeekbar.progress = currentSize.toInt()

        if (currentLineType == IDoodle.LineType.SOLID_LINE.ordinal) {
            binding.lineTypeGroup.check(R.id.solid_line_rb)
        } else {
            binding.lineTypeGroup.check(R.id.dotted_line_rb)
        }

        binding.confirmBtn.setOnClickListener {
            val newPaintSize = binding.paintSizeSeekbar.progress.toFloat()
            val newPaintLineType = if (binding.solidLineRb.isChecked) IDoodle.LineType.SOLID_LINE else IDoodle.LineType.DOTTED_LINE
            if (newPaintSize != currentSize || newPaintLineType.ordinal != currentLineType) {
                EventBus.getDefault().post(ChangePenAttrEvent(newPaintSize, newPaintLineType))
            }
            dismiss()
        }
        binding.cancelBtn.setOnClickListener { dismiss() }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param currentPaintSize
         * @param currentPaintLineType
         * @return A new instance of fragment PenSettingDialogFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(currentPaintSize: Float, currentPaintLineType: Int) =
                PenSettingDialogFragment().apply {
                    arguments = Bundle().apply {
                        putFloat(CURRENT_SIZE, currentPaintSize)
                        putInt(CURRENT_LINE_TYPE, currentPaintLineType)
                    }
                }
    }
}