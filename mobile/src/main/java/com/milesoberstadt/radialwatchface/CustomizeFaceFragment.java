package com.milesoberstadt.radialwatchface;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by miles on 6/19/15.
 */

public class CustomizeFaceFragment extends Fragment {
    public static String ARG_NAV_INDEX = "arg_nav_index";

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view;

        int navIndex = savedInstanceState.getInt(ARG_NAV_INDEX);
        if (navIndex == -1){
            view = inflater.inflate(R.layout.fragment_colors, container, false);
        }
        else{
            view = inflater.inflate(R.layout.fragment_themes, container, false);
        }

        return view;
    }

}
