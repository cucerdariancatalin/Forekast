package com.nebula.forekast.fragment

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.DatePicker
import java.util.*

/**
 * Created by horus on 6/27/17.
 */
class DatePickerFragment : DialogFragment(), DatePickerDialog.OnDateSetListener {


    private var mListener: OnDateSelectedListener? = null

    companion object {

        fun newInstance(): DatePickerFragment {
            val fragment = DatePickerFragment()
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use the current date as the default date in the picker
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        // Create a new instance of DatePickerDialog and return it
        return DatePickerDialog(activity, this, year, month, day)
    }

    override fun onDateSet(p0: DatePicker?, year: Int, month: Int, day: Int) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        mListener!!.onDateSelected(cal.time)

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnDateSelectedListener) {
            mListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnDateSelectedListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    interface OnDateSelectedListener {
        // TODO: Update argument type and name
        fun onDateSelected(date: Date)
    }

}