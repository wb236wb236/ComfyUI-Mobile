package com.example.demo;

import android.content.ContentValues;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.example.demo.manager.HistoryStore;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public class PhotoViewDialogFragment extends DialogFragment {

    private static final String ARG_URL = "arg_url";
    private static final String ARG_LOCAL_PATH = "arg_local_path";

    public static PhotoViewDialogFragment newInstance(HistoryStore.HistoryEntry entry) {
        PhotoViewDialogFragment fragment = new PhotoViewDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, entry.url);
        args.putString(ARG_LOCAL_PATH, entry.localPath);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.dialog_fullscreen_image, container, false);
        PhotoView photoView = root.findViewById(R.id.fullscreenPhotoView);

        String localPath = getArguments() != null ? getArguments().getString(ARG_LOCAL_PATH) : null;
        String url = getArguments() != null ? getArguments().getString(ARG_URL) : null;

        final File localFile;
        if (localPath != null && !localPath.isEmpty()) {
            File f = new File(localPath);
            localFile = f.exists() ? f : null;
        } else {
            localFile = null;
        }

        if (localFile != null) {
            Glide.with(this).load(localFile).into(photoView);
        } else if (url != null && !url.isEmpty()) {
            Glide.with(this).load(url).into(photoView);
        }

        photoView.setOnClickListener(v -> dismiss());
        photoView.setOnLongClickListener(v -> {
            if (localFile != null) {
                saveToGallery(localFile);
            } else {
                Toast.makeText(getContext(), "仅本地图片可保存", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        photoView.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);

        return root;
    }

    private void saveToGallery(File imageFile) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, System.currentTimeMillis() + ".png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            Uri uri = requireActivity().getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                Toast.makeText(getContext(), "保存失败", Toast.LENGTH_SHORT).show();
                return;
            }
            try (InputStream is = new java.io.FileInputStream(imageFile);
                 OutputStream os = requireActivity().getContentResolver().openOutputStream(uri)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
            }
            Toast.makeText(getContext(), "已保存到相册", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "保存失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(android.R.style.Theme_Black_NoTitleBar, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            getDialog().getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.BLACK));
        }
    }
}
