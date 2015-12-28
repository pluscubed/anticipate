package com.pluscubed.anticipate;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.pluscubed.anticipate.transitions.FabDialogMorphSetup;

public class AddAppDialogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_add_perapp);

        View viewById = findViewById(R.id.root);
        FabDialogMorphSetup.setupSharedElementTransitions(this, viewById, getResources().getDimensionPixelSize(R.dimen.dialog_corners));
    }

}
