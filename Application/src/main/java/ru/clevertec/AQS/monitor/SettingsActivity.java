package ru.clevertec.AQS.monitor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import ru.clevertec.AQS.common.AppStorage;
import ru.clevertec.AQS.monitor.protocol.Data;

public class SettingsActivity extends Activity {

    private Spinner mFirstGraphSpinner, mSecondGraphSpinner;

    private Button mOKButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_settings);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        mFirstGraphSpinner = (Spinner) findViewById(R.id.spnFirstGraph);
        mSecondGraphSpinner = (Spinner) findViewById(R.id.spnSecondGraph);

        mOKButton = (Button) findViewById(R.id.btnOk);

        mOKButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create the result Intent and include the MAC address
                Intent intent = new Intent();

                AppStorage.setDataType1(v.getContext(), (DataType)mFirstGraphSpinner.getSelectedItem());
                AppStorage.setDataType2(v.getContext(), (DataType)mSecondGraphSpinner.getSelectedItem());

                // Set result and finish this Activity
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });

        ArrayAdapter<DataType> typesAdapter = new ArrayAdapter<DataType>(this, R.layout.settings_spinner_chart_item);
        mFirstGraphSpinner.setAdapter(typesAdapter);
        mSecondGraphSpinner.setAdapter(typesAdapter);

        typesAdapter.addAll(DataType.values());

        mFirstGraphSpinner.setSelection(AppStorage.getDataType1(this).ordinal());
        mSecondGraphSpinner.setSelection(AppStorage.getDataType2(this).ordinal());

    }
}
