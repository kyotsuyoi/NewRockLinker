package com.rocklinker.UI.Home;

import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.rocklinker.MainActivity;
import com.rocklinker.R;

public class HomeFragment extends Fragment {

    private MainActivity main;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);
        main = (MainActivity) getActivity();

        final TextView textView = root.findViewById(R.id.text_home);

        String ver = "Versão não encontrada";
        try {
            PackageInfo packageInfo;
            packageInfo = main.getPackageManager().getPackageInfo(main.getPackageName(), 0);
            ver = "Versão " + packageInfo.versionName + "\n Aplicação em desenvolvimento, esta área ainda não foi definida";
            textView.setText(ver);
        } catch (Exception e) {
            textView.setText(ver);
        }

        return root;
    }
}