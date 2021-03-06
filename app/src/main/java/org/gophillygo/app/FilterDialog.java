package org.gophillygo.app;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.gophillygo.app.data.models.Filter;
import org.gophillygo.app.databinding.FilterModalBinding;
import org.jetbrains.annotations.NotNull;


public class FilterDialog extends BottomSheetDialogFragment {

    private Filter filter;
    private FilterModalBinding binding;

    public interface FilterChangeListener {
        void setFilter(Filter filter);
    }

    private static final String LOG_LABEL = "FilterDialog";
    private static final String FILTER_ARG = "Filter";

    public static FilterDialog newInstance(Filter filter) {
        FilterDialog dialog = new FilterDialog();

        Bundle args = new Bundle();
        args.putSerializable(FILTER_ARG, filter);
        dialog.setArguments(args);

        return dialog;
    }

    @Override
    public void onDismiss(@NotNull DialogInterface dialog) {
        Log.d(LOG_LABEL, "Selected " + filter.count() + " filters.");
        FilterChangeListener listener = (FilterChangeListener) getActivity();
        if (listener != null) {
            listener.setFilter(filter);
        }

        super.onDismiss(dialog);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        filter = null;
        if (getArguments() != null) {
            filter = (Filter) getArguments().getSerializable(FILTER_ARG);
        }
        if (filter == null) {
            filter = new Filter();
        }

        Dialog dialog = super.onCreateDialog(savedInstanceState);
        binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()),
                R.layout.filter_modal, null, false);
        binding.setFilter(filter);
        binding.setDialog(this);
        dialog.setContentView(binding.getRoot());

        return dialog;
    }

    public void resetFilters(View view) {
        filter.reset();
        binding.executePendingBindings();
    }
}
