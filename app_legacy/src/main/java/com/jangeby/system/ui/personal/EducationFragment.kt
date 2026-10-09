package com.jangeby.system.ui.personal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.jangeby.system.databinding.FragmentEducationBinding
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.EducationModel
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager

class EducationFragment : Fragment() {
    private var _binding: FragmentEducationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEducationBinding.inflate(inflater, container, false)
        return binding.root
    }

    private lateinit var educationAdapter: EducationAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        educationAdapter = EducationAdapter(emptyList(), { item ->
            showEducationForm(item)
        }, { item ->
            deleteEducation(item.id)
        })
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = educationAdapter
        }
        
        binding.swipeRefresh.setOnRefreshListener {
            loadData()
        }
        
        binding.fabAdd.setOnClickListener {
            showEducationForm()
        }
        
        loadData()
    }
    
    private fun loadData() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService(requireContext()).getEducation()
                if (response.success && response.data != null) {
                    educationAdapter.updateData(response.data)
                }
            } catch (e: Exception) {
                // Ignore for now
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
    
    private fun showEducationForm(itemToEdit: EducationModel? = null) {
        val dialogView = layoutInflater.inflate(com.jangeby.system.R.layout.dialog_form_education, null)
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundResource(android.R.color.transparent)
        
        val etDegree = dialogView.findViewById<android.widget.AutoCompleteTextView>(com.jangeby.system.R.id.etDegree)
        val etInstitution = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.jangeby.system.R.id.etInstitution)
        val etMajor = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.jangeby.system.R.id.etMajor)
        val etYear = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.jangeby.system.R.id.etYear)
        
        val degrees = arrayOf("SD", "SMP", "SMA/SMK", "D1", "D2", "D3", "D4", "S1", "S2", "S3")
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, degrees)
        etDegree.setAdapter(adapter)

        if (itemToEdit != null) {
            etInstitution.setText(itemToEdit.institution_name)
            etDegree.setText(itemToEdit.level, false)
            etMajor.setText(itemToEdit.major)
            etYear.setText(itemToEdit.end_year?.toString())
        }

        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(com.jangeby.system.R.id.btnCancel)
        val btnSave = dialogView.findViewById<com.google.android.material.button.MaterialButton>(com.jangeby.system.R.id.btnSave)
        
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener { 
            val institution = etInstitution.text.toString()
            val degree = etDegree.text.toString()
            val major = etMajor.text.toString()
            val year = etYear.text.toString()
            
            val req = mapOf<String, Any>(
                "institution_name" to institution,
                "level" to degree,
                "major" to major,
                "end_year" to year
            )
            
            lifecycleScope.launch {
                try {
                    val response = if (itemToEdit != null) {
                        ApiClient.getService(requireContext()).updateEducation(itemToEdit.id, req)
                    } else {
                        ApiClient.getService(requireContext()).storeEducation(req)
                    }
                    
                    if (response.success) {
                        Toast.makeText(context, "Data berhasil disimpan", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadData()
                    } else {
                        Toast.makeText(context, response.message ?: "Gagal menyimpan", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        dialog.show()
    }

    private fun deleteEducation(id: Int) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Hapus")
            .setMessage("Yakin ingin menghapus data ini?")
            .setPositiveButton("Hapus") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val response = ApiClient.getService(requireContext()).deleteEducation(id)
                        if (response.success) {
                            Toast.makeText(context, "Berhasil dihapus", Toast.LENGTH_SHORT).show()
                            loadData()
                        } else {
                            Toast.makeText(context, response.message ?: "Gagal menghapus", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
