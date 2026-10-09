package com.jangeby.system.ui.task

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.jangeby.system.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText

class BottomSheetAddTask : BottomSheetDialogFragment() {

    private lateinit var llTasksContainer: LinearLayout
    var onSaveListener: ((List<String>) -> Unit)? = null

    override fun getTheme(): Int = com.google.android.material.R.style.Theme_Design_BottomSheetDialog
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_bottom_sheet_add_task, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        llTasksContainer = view.findViewById(R.id.llTasksContainer)
        val btnAddRow = view.findViewById<View>(R.id.btnAddRow)
        val btnSave = view.findViewById<View>(R.id.btnSave)
        
        // Add initial row
        addRow()
        
        btnAddRow.setOnClickListener {
            addRow()
        }
        
        btnSave.setOnClickListener {
            val tasks = mutableListOf<String>()
            for (i in 0 until llTasksContainer.childCount) {
                val row = llTasksContainer.getChildAt(i)
                val et = row.findViewById<TextInputEditText>(R.id.etTaskTitle)
                val text = et.text?.toString()?.trim()
                if (!text.isNullOrEmpty()) {
                    tasks.add(text)
                }
            }
            if (tasks.isNotEmpty()) {
                onSaveListener?.invoke(tasks)
                dismiss()
            } else {
                android.widget.Toast.makeText(requireContext(), "Harap isi minimal 1 tugas", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun addRow() {
        val rowView = layoutInflater.inflate(R.layout.item_bottom_sheet_task_row, llTasksContainer, false)
        val btnRemove = rowView.findViewById<View>(R.id.btnRemoveRow)
        
        btnRemove.setOnClickListener {
            llTasksContainer.removeView(rowView)
            if (llTasksContainer.childCount == 0) {
                addRow()
            }
        }
        
        llTasksContainer.addView(rowView)
    }
}
