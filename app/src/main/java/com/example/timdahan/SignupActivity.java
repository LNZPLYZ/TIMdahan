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

public class SignupActivity extends AppCompatActivity {

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private DatabaseHelper databaseHelper;
    private UserSession userSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        hideNavigationBar();

        databaseHelper = new DatabaseHelper(this);
        userSession = new UserSession(this);

        EditText etName = findViewById(R.id.et_signup_name);
        EditText etEmail = findViewById(R.id.et_signup_email);
        EditText etPassword = findViewById(R.id.et_signup_password);
        EditText etConfirmPassword = findViewById(R.id.et_signup_confirm_password);
        ImageView btnTogglePassword = findViewById(R.id.btn_toggle_password_signup);
        ImageView btnToggleConfirmPassword = findViewById(R.id.btn_toggle_confirm_password_signup);
        Button btnSignup = findViewById(R.id.btn_signup);
        TextView tvLoginLink = findViewById(R.id.tv_login_link);

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

        // Confirm Password visibility toggle
        if (btnToggleConfirmPassword != null) {
            btnToggleConfirmPassword.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isConfirmPasswordVisible = !isConfirmPasswordVisible;
                    if (isConfirmPasswordVisible) {
                        etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    } else {
                        etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    }
                    etConfirmPassword.setSelection(etConfirmPassword.getText().length());
                }
            });
        }

        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etName.getText().toString().trim();
                String rawEmail = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                String confirmPassword = etConfirmPassword.getText().toString().trim();

                if (name.isEmpty()) {
                    etName.setError("Please enter your store or full name");
                    etName.requestFocus();
                    return;
                }

                if (rawEmail.isEmpty()) {
                    etEmail.setError("Please enter your email");
                    etEmail.requestFocus();
                    return;
                }

                String email = BackupManager.getCanonicalEmail(rawEmail);

                if (password.isEmpty()) {
                    etPassword.setError("Please enter a password");
                    etPassword.requestFocus();
                    return;
                }

                if (confirmPassword.isEmpty()) {
                    etConfirmPassword.setError("Please confirm your password");
                    etConfirmPassword.requestFocus();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    etConfirmPassword.setError("Passwords do not match");
                    etConfirmPassword.requestFocus();
                    return;
                }

                // 1. Check local SQLite DB first
                if (databaseHelper.checkEmailExists(email)) {
                    etEmail.setError("Email is already registered. Please log in.");
                    etEmail.requestFocus();
                    return;
                }

                // 2. Check Cloud DB to ensure 1 unique email holds data for backup
                btnSignup.setEnabled(false);
                Toast.makeText(SignupActivity.this, "Checking email availability...", Toast.LENGTH_SHORT).show();

                BackupManager.checkCloudAccountExists(email, new BackupManager.CloudCheckCallback() {
                    @Override
                    public void onResult(boolean exists, String errorMessage) {
                        if (exists) {
                            btnSignup.setEnabled(true);
                            etEmail.setError("An account with this email already exists in the Cloud. Please log in.");
                            etEmail.requestFocus();
                        } else {
                            // Proceed with account creation
                            long result = databaseHelper.addUser(name, email, password);

                            if (result != -1) {
                                btnSignup.setEnabled(true);
                                int userId = databaseHelper.getUserIdByEmail(email);
                                userSession.createSession(userId, name, email);

                                Toast.makeText(SignupActivity.this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();

                                // Navigate to Dashboard
                                Intent intent = new Intent(SignupActivity.this, DashboardActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                btnSignup.setEnabled(true);
                                Toast.makeText(SignupActivity.this, "Registration failed. Try again.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
            }
        });

        tvLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Login
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
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