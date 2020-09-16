package cn.hzw.doodledemo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import cn.hzw.doodledemo.databinding.DialogLegendBinding
import org.greenrobot.eventbus.EventBus

class LegendDialogFragment : DialogFragment() {
    private var _binding: DialogLegendBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogLegendBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val legendStrArray = resources.getStringArray(R.array.LegendSymbol)
        val legends = legendStrArray.map {
            val legendProperties = it.split(":")
            Legend(legendProperties[0], legendProperties[1])
        }

        binding.legendRv.layoutManager = GridLayoutManager(requireContext(), 3)
        val adapter = LegendAdapter(legends.toMutableList())
        adapter.setOnItemClickListener { adapter, view, position ->
            val legend = adapter.data[position] as Legend
            EventBus.getDefault().post(legend)
            dismiss()
        }

        binding.legendRv.adapter = adapter

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}

