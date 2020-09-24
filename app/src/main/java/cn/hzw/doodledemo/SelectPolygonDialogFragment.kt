package cn.hzw.doodledemo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import cn.hzw.doodle.DoodleShape
import cn.hzw.doodledemo.databinding.PopupSelectPolygonBinding
import org.greenrobot.eventbus.EventBus

/**
 * A simple [Fragment] subclass.
 * Use the [PenSettingDialogFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SelectPolygonDialogFragment : DialogFragment() {

    private var _binding: PopupSelectPolygonBinding? = null
    private val binding get() = _binding!!

    override fun onStart() {
        super.onStart()
        val screenWidth = 1000
        dialog?.window?.setLayout(screenWidth, ViewGroup.MarginLayoutParams.WRAP_CONTENT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        _binding = PopupSelectPolygonBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.confirmBtn.setOnClickListener {
            val selectedPolygon = when (binding.polygonGroup.checkedRadioButtonId) {
                R.id.pentagon_rb -> DoodleShape.PENTAGON
                R.id.hexagon_rb -> DoodleShape.HEXAGON
                else -> DoodleShape.TRIANGLE
            }
            EventBus.getDefault().post(SelectPolygonEvent(selectedPolygon))
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
         * @return A new instance of fragment SelectPolygonDialogFragment.
         */
        @JvmStatic
        fun newInstance() = SelectPolygonDialogFragment()
    }
}