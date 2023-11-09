package mobile.ui.login_signup;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;

import io.github.sceneview.sample.arcursorplacement.R;

public class SignUpActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText nameEditText;
    private Button signUpButton;
    private ProgressBar progressBar;

    private String params = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_home);

        emailEditText = findViewById(R.id.register_email_text);
        passwordEditText = findViewById(R.id.register_password_text);
        nameEditText = findViewById(R.id.register_name_text);
        signUpButton = findViewById(R.id.bt_register);
        progressBar = findViewById(R.id.progressbar);

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                String name = nameEditText.getText().toString();

                if (isValid(email, password, name)) {
                    signUp(email, password, name);
                } else {
                    Toast.makeText(SignUpActivity.this, "Invalid details", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean isValid(String email, String password, String name) {
        return isValidEmail(email) && isValidPassword(password) && isValidName(name);
    }

    private boolean isValidEmail(String email) {
        // Use a more restrictive regex pattern for email validation
        String emailPattern = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";
        return !email.isEmpty() && email.matches(emailPattern);
    }

    private boolean isValidPassword(String password) {
        // Add your password validation logic here
        // For example, check if it has a minimum length or specific requirements
        return !password.isEmpty() && password.length() >= 6; // Minimum length of 8 characters
    }

    private boolean isValidName(String name) {
        // Add your name validation logic here
        // For example, check if it's not empty and doesn't contain special characters
        return !name.isEmpty() && name.matches("[a-zA-Z]+");
    }

    private void signUp(String email, String password, String name) {
        progressBar.setVisibility(View.VISIBLE);

        JSONObject paramsObject = new JSONObject();
        try {
            paramsObject.put("email", email);
            paramsObject.put("password", password);
            paramsObject.put("name", name);
            params = paramsObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        SignUpTask task = new SignUpTask();
        task.execute();
    }

    private class SignUpTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            requestRegistration(params);
            return null;
        }
    }

    private void requestRegistration(String data) {
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        String url = "https://your-backend-url/register"; // Replace with your actual backend URL

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.e("requestTest", "Registration Response: " + response);

                        // Handle the registration response as needed
                        progressBar.setVisibility(View.INVISIBLE);
                        Toast.makeText(SignUpActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                        finish(); // Close the SignUpActivity after successful registration
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("requestTest", "Registration Error: " + error.getMessage());
                        progressBar.setVisibility(View.INVISIBLE);
                        // Handle registration error
                        Toast.makeText(SignUpActivity.this, "Registration Error", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            public byte[] getBody() {
                try {
                    return data.getBytes("utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };

        queue.add(stringRequest);
    }
}

