package com.beagile.fastcontacts;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.beagile.fastcontacts.contacts.PhoneContacts;
import com.beagile.fastcontacts.contacts.PhoneContactsCallback;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    private PhoneContacts mPhoneContacts;
    private ProgressBar mProgressBar;
    private Button mBtnSyncContacts;
    private TextView mProgressText, mTotalSecondsText, mRecordsPerSecondText, mTotalRecordsText, mSyncingContactsText;
    private int mMax;
    private long mStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.mPhoneContacts = new PhoneContacts(this, mPhoneContactsCallback);

        mProgressBar = findViewById(R.id.progress_bar);
        mProgressText = findViewById(R.id.progress_text);
        mTotalSecondsText = findViewById(R.id.summary_total_seconds);
        mRecordsPerSecondText = findViewById(R.id.summary_records_per_second);
        mTotalRecordsText = findViewById(R.id.summary_total_records);
        mSyncingContactsText = findViewById(R.id.syncing_contacts);

        mBtnSyncContacts = findViewById(R.id.button_sync_contacts);

        mBtnSyncContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(this.getClass().getSimpleName(), "Importing contacts");
                mPhoneContacts.starSyncWithPermissionsCheck();
            }
        });
    }

    private PhoneContactsCallback mPhoneContactsCallback = new PhoneContactsCallback() {
        @Override
        public void didFinishLoadingPerson(boolean wasChanged, int current, int max) {
            mProgressBar.setIndeterminate(false);
            mProgressBar.setMax(max);
            mProgressBar.setProgress(current);
            mMax = max;
            DecimalFormat format = new DecimalFormat("00.00");
            double percent = ((double) current / (double) max) * 100;
            String percentText = format.format(percent);
            String progressText = percentText + "%";
            mProgressText.setText(progressText);

            long end = System.currentTimeMillis();
            long duration = end - mStart;
            double seconds = duration / (float) 1000;

            double recordsPerSecond = current / seconds;

            String totalRecordsText = "Total Contacts: " + mMax;
            mTotalRecordsText.setText(totalRecordsText);

            String totalSecondsText = "Total Seconds: " + format.format(seconds);
            mTotalSecondsText.setText(totalSecondsText);

            String recordsPerSecondText = "Contacts per second: " + format.format(recordsPerSecond);
            mRecordsPerSecondText.setText(recordsPerSecondText);
        }

        @Override
        public void didStartSyncingContacts() {
            mBtnSyncContacts.setVisibility(View.GONE);
            mSyncingContactsText.setVisibility(View.VISIBLE);
            mStart = System.currentTimeMillis();
            mProgressBar.setVisibility(View.VISIBLE);
            mProgressText.setVisibility(View.VISIBLE);
            mProgressBar.setIndeterminate(true);

            mTotalRecordsText.setVisibility(View.VISIBLE);
            mTotalSecondsText.setVisibility(View.VISIBLE);
            mRecordsPerSecondText.setVisibility(View.VISIBLE);
        }

        @Override
        public void didEndSyncingContacts() {
            mBtnSyncContacts.setVisibility(View.VISIBLE);
            mSyncingContactsText.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.INVISIBLE);
            mProgressText.setVisibility(View.INVISIBLE);
        }
    };

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        this.mPhoneContacts.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void startPhoneContactsSynchronization() {
        mPhoneContacts.sync();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPhoneContacts.release();
    }
}
