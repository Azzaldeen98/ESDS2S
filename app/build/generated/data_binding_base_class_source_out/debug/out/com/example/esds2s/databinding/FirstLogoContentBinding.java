// Generated by view binder compiler. Do not edit!
package com.example.esds2s.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.esds2s.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class FirstLogoContentBinding implements ViewBinding {
  @NonNull
  private final LinearLayout rootView;

  @NonNull
  public final LinearLayout firstLogoContent;

  @NonNull
  public final TextView splashLabel;

  @NonNull
  public final ImageView splashRobotImg;

  private FirstLogoContentBinding(@NonNull LinearLayout rootView,
      @NonNull LinearLayout firstLogoContent, @NonNull TextView splashLabel,
      @NonNull ImageView splashRobotImg) {
    this.rootView = rootView;
    this.firstLogoContent = firstLogoContent;
    this.splashLabel = splashLabel;
    this.splashRobotImg = splashRobotImg;
  }

  @Override
  @NonNull
  public LinearLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static FirstLogoContentBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static FirstLogoContentBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.first_logo_content, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static FirstLogoContentBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      LinearLayout firstLogoContent = (LinearLayout) rootView;

      id = R.id.splash_label;
      TextView splashLabel = ViewBindings.findChildViewById(rootView, id);
      if (splashLabel == null) {
        break missingId;
      }

      id = R.id.splash_robot_img;
      ImageView splashRobotImg = ViewBindings.findChildViewById(rootView, id);
      if (splashRobotImg == null) {
        break missingId;
      }

      return new FirstLogoContentBinding((LinearLayout) rootView, firstLogoContent, splashLabel,
          splashRobotImg);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}