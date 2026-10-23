package com.example.timdahan;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class LoginActivity extends AppCompatActivity {

    private boolean isPasswordVisible = false;
    private DatabaseHelper databaseHelper;
    private UserSession userSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        hideNavigationBar();

        databaseHelper = new DatabaseHelper(this);
        userSession = new UserSession(this);

        EditText etEmail = findViewById(R.id.et_login_email);
        EditText etPassword = findViewById(R.id.et_login_password);
        ImageView btnTogglePassword = findViewById(R.id.btn_toggle_password_login);
        Button btnLogin = findViewById(R.id.btn_login);
        TextView tvSignupLink = findViewById(R.id.tv_signup_link);

        // Password visibility toggle
        if (btnTogglePassword != null) {
            btnTogglePassword.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isPasswordVisible = !isPasswordVisible;
                    if (isPasswordVisible) {
                        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    } else {
                        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    }
                    etPassword.setSelection(etPassword.getText().length());
                }
            });
        }

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String rawEmail = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (rawEmail.isEmpty()) {
                    etEmail.setError("Please enter your email");
                    etEmail.requestFocus();
                    return;
                }

                String email = BackupManager.getCanonicalEmail(rawEmail);

                if (password.isEmpty()) {
                    etPassword.setError("Please enter your password");
                    etPassword.requestFocus();
                    return;
                }

                // Verify user credentials in local SQLite
                boolean isValidUser = databaseHelper.checkUser(email, password);

                if (isValidUser) {
                    int userId = databaseHelper.getUserIdByEmail(email);
                    String storeName = databaseHelper.getStoreNameByEmail(email);
                    userSession.createSession(userId, storeName, email);

                    Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();

                    // Navigate to Dashboard
                    Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                    startActivity(intent);
                    finish();
                } else if (databaseHelper.checkEmailExists(email)) {
                    // Email exists on this phone's SQLite, but password was wrong
                    Toast.makeText(LoginActivity.this, "Invalid Password. Please try again.", Toast.LENGTH_LONG).show();
                } else {
                    // Account not in local SQLite -> Attempt Cloud Login & Account Sync for new phone
                    Toast.makeText(LoginActivity.this, "Checking Cloud Account...", Toast.LENGTH_SHORT).show();
                    btnLogin.setEnabled(false);

                    BackupManager.loginAndSyncFromCloud(LoginActivity.this, email, password, new BackupManager.CloudCallback() {
                        @Override
                        public void onSuccess(String message) {
                            btnLogin.setEnabled(true);
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            btnLogin.setEnabled(true);
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });

        tvSignupLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Sign Up
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });
    }

    private void hideNavigationBar() {
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );

        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars());
    }
}