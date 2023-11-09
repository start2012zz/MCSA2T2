package mobile.ui.login_signup;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView signUpTextView;
    private TextView forgotPasswordTextView;

    private static HttpURLConnection connection = null;
    private static DataOutputStream out = null;
    private static InputStream in = null;
    private static URL realUrl = null;
    private String params = "";//定义参数字符串

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_home);

        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        loginButton = findViewById(R.id.buttonLogin);
        signUpTextView = findViewById(R.id.textViewSignUp);
        forgotPasswordTextView = findViewById(R.id.textViewForgotPassword);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                // Check if email and password are valid (add your authentication logic here)
                if (isValid(email, password)) {
                    // Continue with the login process
                    login(email, password);
                } else {
                    // Show an error message or handle invalid credentials
                    Toast.makeText(LoginActivity.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                }
            }
        });


        signUpTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle sign-up navigation or logic here
            }
        });

        forgotPasswordTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle forgot password logic here
            }
        });
    }

    private boolean isValid(String email, String password) {
        // Add your authentication logic here
        // For example, you can check if email and password meet certain criteria
        // For simplicity, this example checks if both fields are non-empty
        return !TextUtils.isEmpty(email) && !TextUtils.isEmpty(password);
    }
}

