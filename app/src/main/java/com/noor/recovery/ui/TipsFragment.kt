package com.noor.recovery.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.noor.recovery.databinding.FragmentTipsBinding
import com.noor.recovery.ui.adapter.TipAdapter

class TipsFragment : Fragment() {

    private var _binding: FragmentTipsBinding? = null
    private val binding get() = _binding!!

    private val tips = listOf(
        "حدّد محفزاتك — اعرف ما الذي يشعل الرغبة لديك وتجنّبه في البداية.",
        "استبدل العادة السيئة بعادة صحية: رياضة، قراءة، أو أي هواية تمتصّ طاقتك.",
        "لا تواجه الرغبة وحدك — تحدث مع شخص تثق به أو اكتب مشاعرك.",
        "الرغبة تدوم ١٥-٢٠ دقيقة ثم تخفت — ابقَ مشغولاً خلالها.",
        "احتفل بكل إنجاز صغير — يوم، أسبوع، شهر — فكل خطوة تستحق التقدير.",
        "اطلب المساعدة المتخصصة عند الحاجة — لا عيب في ذلك، بل هو شجاعة.",
        "إن انتكستَ فلا تستسلم — الطريق ليس مستقيماً دائماً، المهم العودة.",
        "النوم الجيد والتغذية الصحية يقوّيان إرادتك بشكل ملحوظ.",
        "مارس تمارين التنفس العميق كلما شعرت بالرغبة الشديدة.",
        "تخيّل نفسك بعد سنة وأنت قد نجحت — هذه الصورة قوة حقيقية."
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTipsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvTips.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTips.adapter = TipAdapter(tips)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
