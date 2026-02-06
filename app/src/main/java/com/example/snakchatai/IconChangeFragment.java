package com.example.snakchatai;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

public class IconChangeFragment extends Fragment {

    RadioButton rbDefault, rbIcon1, rbIcon2, rbIcon3;
    Button btnSubmit;

    CardView cardDefault, card1, card2, card3;

    public IconChangeFragment() {
        // required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.activity_icon_change, container, false);

        // RadioButtons
        rbDefault = view.findViewById(R.id.rb_default);
        rbIcon1   = view.findViewById(R.id.rb_icon1);
        rbIcon2   = view.findViewById(R.id.rb_icon2);
        rbIcon3   = view.findViewById(R.id.rb_icon3);

        // Cards
        cardDefault = view.findViewById(R.id.card_default);
        card1       = view.findViewById(R.id.card_1);
        card2       = view.findViewById(R.id.card_2);
        card3       = view.findViewById(R.id.card_3);

        btnSubmit = view.findViewById(R.id.btnApply);

        // Card click = select icon
        cardDefault.setOnClickListener(v -> select(rbDefault));
        card1.setOnClickListener(v -> select(rbIcon1));
        card2.setOnClickListener(v -> select(rbIcon2));
        card3.setOnClickListener(v -> select(rbIcon3));

        // Apply button
        btnSubmit.setOnClickListener(v -> {

            if (rbDefault.isChecked()) {
                switchIcon("splash_screen");
            } else if (rbIcon1.isChecked()) {
                switchIcon("icon_alt1");
            } else if (rbIcon2.isChecked()) {
                switchIcon("icon_alt2");
            } else if (rbIcon3.isChecked()) {
                switchIcon("icon_alt3");
            } else {
                Toast.makeText(requireContext(),
                        "Koi icon select kar",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(requireContext(),
                    "Icon changed (launcher refresh me thoda time lag sakta hai)",
                    Toast.LENGTH_LONG).show();
        });

        return view;
    }

    private void select(RadioButton selected) {
        rbDefault.setChecked(false);
        rbIcon1.setChecked(false);
        rbIcon2.setChecked(false);
        rbIcon3.setChecked(false);

        selected.setChecked(true);
    }

    private void switchIcon(String component) {
        PackageManager pm = requireContext().getPackageManager();
        String pkg = requireContext().getPackageName();

        String[] allIcons = {
                pkg + ".splash_screen",
                pkg + ".icon_alt1",
                pkg + ".icon_alt2",
                pkg + ".icon_alt3"
        };

        for (String c : allIcons) {
            pm.setComponentEnabledSetting(
                    new ComponentName(pkg, c),
                    c.endsWith(component)
                            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
            );
        }
    }
}
