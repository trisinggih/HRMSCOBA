package com.jangeby.system.ui.personal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.jangeby.system.databinding.FragmentFamilyBinding
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.FamilyModel
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager

class FamilyFragment : Fragment() {
    private var _binding: FragmentFamilyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFamilyBinding.inflate(inflater, container, false)
        return binding.root
    }

    private lateinit var familyAdapter: FamilyAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        familyAdapter = FamilyAdapter(emptyList(), { item ->
            showFamilyForm(item)
        }, { item ->
            deleteFamily(item.id)
        })
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = familyAdapter
        }
        
        binding.swipeRefresh.setOnRefreshListener {
            loadData()
        }
        
        binding.fabAdd.setOnClickListener {
            showFamilyForm()
        }
        
        loadData()
    }
    
    private fun loadData() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService(requireContext()).getFamily()
                if (response.success && response.data != null) {
                    familyAdapter.updateData(response.data)
                }
            } catch (e: Exception) {
                // Ignore for now
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
    
    private fun showFamilyForm(itemToEdit: FamilyModel? = null) {
        val dialogView = layoutInflater.inflate(com.jangeby.system.R.layout.dialog_form_family, null)
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundResource(android.R.color.transparent)
        
        val etRelation = dialogView.findViewById<android.widget.AutoCompleteTextView>(com.jangeby.system.R.id.etRelation)
        val etName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.jangeby.system.R.id.etName)
        val etPhone = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.jangeby.system.R.id.etPhone)
        
        val relations = arrayOf("Suami/Istri", "Anak", "Orang Tua", "Lainnya")
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, relations)
        etRelation.setAdapter(adapter)

        if (itemToEdit != null) {
            etName.setText(itemToEdit.name)
            etPhone.setText(itemToEdit.phone)
            val relText = when (itemToEdit.relation) {
                "spouse" -> "Suami/Istri"
                "child" -> "Anak"
                "parent" -> "Orang Tua"
                else -> "Lainnya"
            }
            etRelation.setText(relText, false)
        }

        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(com.jangeby.system.R.id.btnCancel)
        val btnSave = dialogView.findViewById<com.google.android.material.button.MaterialButton>(com.jangeby.system.R.id.btnSave)
        
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener { 
            val name = etName.text.toString()
            val relationRaw = etRelation.text.toString()
            val phone = etPhone.text.toString()
            
            // Map to enum
            val relation = when (relationRaw) {
                "Suami/Istri" -> "spouse"
                "Anak" -> "child"
                "Orang Tua" -> "parent"
                else -> "other"
            }
            
            val req = mapOf<String, Any>(
                "name" to name,
                "relation" to relation,
                "phone" to phone,
                "gender" to "other",
                "dob" to "2000-01-01"
            )
            
            lifecycleScope.launch {
                try {
                    val response = if (itemToEdit != null) {
                        ApiClient.getService(requireContext()).updateFamily(itemToEdit.id, req)
                    } else {
                        ApiClient.getService(requireContext()).storeFamily(req)
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

    private fun deleteFamily(id: Int) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Hapus")
            .setMessage("Yakin ingin menghapus data ini?")
            .setPositiveButton("Hapus") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val response = ApiClient.getService(requireContext()).deleteFamily(id)
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
