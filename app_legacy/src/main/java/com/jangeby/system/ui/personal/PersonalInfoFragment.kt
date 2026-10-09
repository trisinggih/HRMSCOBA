package com.jangeby.system.ui.personal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.jangeby.system.databinding.FragmentPersonalInfoBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class PersonalInfoFragment : Fragment() {

    private var _binding: FragmentPersonalInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPersonalInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.swipeRefresh.setOnRefreshListener {
            loadData()
        }
        
        binding.btnEdit.setOnClickListener {
            val intent = android.content.Intent(requireContext(), com.jangeby.system.ui.profile.ProfileActivity::class.java)
            startActivity(intent)
        }
        
        loadData()
    }

    private fun loadData() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val res = com.jangeby.system.data.remote.ApiClient.getService(requireContext()).getProfile()
                if (res.success && res.data != null) {
                    val profile = res.data
                    binding.tvName.text = profile.name ?: "-"
                    binding.tvPosition.text = profile.position ?: "-"
                    binding.tvEmployeeId.text = profile.employeeId ?: "-"
                    binding.tvEmail.text = profile.email ?: "-"
                    binding.tvPhone.text = profile.phone ?: "-"
                    binding.tvCompany.text = profile.company ?: "-"
                    binding.tvAddress.text = profile.address ?: "-"
                    
                    if (!profile.photoUrl.isNullOrEmpty()) {
                        com.bumptech.glide.Glide.with(this@PersonalInfoFragment)
                            .load(profile.photoUrl)
                            .placeholder(com.jangeby.system.R.drawable.ic_person)
                            .error(com.jangeby.system.R.drawable.ic_person)
                            .into(binding.ivProfile)
                    }
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(requireContext(), "Gagal memuat profil", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
