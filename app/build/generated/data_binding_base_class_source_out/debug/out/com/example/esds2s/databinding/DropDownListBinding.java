// Generated by view binder compiler. Do not edit!
package com.example.esds2s.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.esds2s.R;
import com.google.android.material.textfield.TextInputLayout;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class DropDownListBinding implements ViewBinding {
  @NonNull
  private final TextInputLayout rootView;

  @NonNull
  public final TextInputLayout DropDownListLanguage;

  @NonNull
  public final AutoCompleteTextView autoCompleteTextViewLanguage;

  private DropDownListBinding(@NonNull TextInputLayout rootView,
      @NonNull TextInputLayout DropDownListLanguage,
      @NonNull AutoCompleteTextView autoCompleteTextViewLanguage) {
    this.rootView = rootView;
    this.DropDownListLanguage = DropDownListLanguage;
    this.autoCompleteTextViewLanguage = autoCompleteTextViewLanguage;
  }

  @Override
  @NonNull
  public TextInputLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static DropDownListBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static DropDownListBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.drop_down_list, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static DropDownListBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      TextInputLayout DropDownListLanguage = (TextInputLayout) rootView;

      id = R.id.autoCompleteTextViewLanguage;
      AutoCompleteTextView autoCompleteTextViewLanguage = ViewBindings.findChildViewById(rootView, id);
      if (autoCompleteTextViewLanguage == null) {
        break missingId;
      }

      return new DropDownListBinding((TextInputLayout) rootView, DropDownListLanguage,
          autoCompleteTextViewLanguage);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
