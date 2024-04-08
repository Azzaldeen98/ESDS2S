// Generated by view binder compiler. Do not edit!
package com.example.esds2s.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.esds2s.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class FragmentMainHomeBinding implements ViewBinding {
  @NonNull
  private final FrameLayout rootView;

  @NonNull
  public final RecyclerView RecyclerViewChatsList;

  @NonNull
  public final LinearLayout backResult;

  @NonNull
  public final Button btnMainCreateChat;

  @NonNull
  public final LinearLayout mainContinerChatButtons;

  private FragmentMainHomeBinding(@NonNull FrameLayout rootView,
      @NonNull RecyclerView RecyclerViewChatsList, @NonNull LinearLayout backResult,
      @NonNull Button btnMainCreateChat, @NonNull LinearLayout mainContinerChatButtons) {
    this.rootView = rootView;
    this.RecyclerViewChatsList = RecyclerViewChatsList;
    this.backResult = backResult;
    this.btnMainCreateChat = btnMainCreateChat;
    this.mainContinerChatButtons = mainContinerChatButtons;
  }

  @Override
  @NonNull
  public FrameLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static FragmentMainHomeBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static FragmentMainHomeBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.fragment_main_home, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static FragmentMainHomeBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.RecyclerViewChatsList;
      RecyclerView RecyclerViewChatsList = ViewBindings.findChildViewById(rootView, id);
      if (RecyclerViewChatsList == null) {
        break missingId;
      }

      id = R.id.backResult;
      LinearLayout backResult = ViewBindings.findChildViewById(rootView, id);
      if (backResult == null) {
        break missingId;
      }

      id = R.id.btn_main_create_chat;
      Button btnMainCreateChat = ViewBindings.findChildViewById(rootView, id);
      if (btnMainCreateChat == null) {
        break missingId;
      }

      id = R.id.main_continer_chat_buttons;
      LinearLayout mainContinerChatButtons = ViewBindings.findChildViewById(rootView, id);
      if (mainContinerChatButtons == null) {
        break missingId;
      }

      return new FragmentMainHomeBinding((FrameLayout) rootView, RecyclerViewChatsList, backResult,
          btnMainCreateChat, mainContinerChatButtons);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
