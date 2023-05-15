package com.example.mappingmemoriesapp;

import static android.text.TextUtils.isEmpty;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.mappingmemoriesapp.Models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity implements
        View.OnClickListener
{
    private static final String TAG = "RegisterActivity";

    //widgets
    private EditText email, password, confirmPassword;
    private ProgressBar progressBar;

    //vars
    private FirebaseFirestore firebaseFirestore;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        email = (EditText) findViewById(R.id.input_email);
        password = (EditText) findViewById(R.id.input_password);
        confirmPassword = (EditText) findViewById(R.id.input_confirm_password);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        findViewById(R.id.btn_register).setOnClickListener(this);

        firebaseFirestore = FirebaseFirestore.getInstance();

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    //Registra un nuevo usuario en Firebase
    public void registerNewEmail(final String email, String password){

        showDialog();

        //Se crea un nuevo usuario con el email y la contraseña proporcionadas
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());

                        if (task.isSuccessful()){
                            Log.d(TAG, "onComplete: AuthState: " + FirebaseAuth.getInstance().getCurrentUser().getUid());

                            //Se crea instancia del objeto User y se establecen sus valores
                            User user = new User();
                            user.setEmail(email);
                            user.setUsername(email.substring(0, email.indexOf("@")));
                            user.setUser_id(FirebaseAuth.getInstance().getUid());

                            //Referencia al documento creado de la colección Users
                            DocumentReference newUserRef = firebaseFirestore
                                    .collection("Users")
                                    .document(FirebaseAuth.getInstance().getUid());

                            //Se establecen los datos del usuario
                            newUserRef.set(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    hideDialog();

                                    if(task.isSuccessful()){
                                        //Si es exitoso se vuelve a la pantalla de Login
                                        redirectLoginScreen();
                                    }else{
                                        View parentLayout = findViewById(android.R.id.content);
                                        Snackbar.make(parentLayout, "Something went wrong.", Snackbar.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                        else {
                            View parentLayout = findViewById(android.R.id.content);
                            Snackbar.make(parentLayout, "Something went wrong.", Snackbar.LENGTH_SHORT).show();
                            hideDialog();
                        }

                    }
                });
    }

    //Se redirige a la actividad Login
    private void redirectLoginScreen(){
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }


    @Override
    public void onClick(View view) {
        //Cuando se pulsa el botón de registrar
        if (view.getId() == R.id.btn_register) {
                //Se comprueba que no haya ningun campo vacío
                if(!isEmpty(email.getText().toString())
                        && !isEmpty(password.getText().toString())
                        && !isEmpty(confirmPassword.getText().toString())){

                    //Se comparan la contraseña y la confirmación para asegurar que sean iguales
                    if(password.getText().toString().equals(confirmPassword.getText().toString())){
                        //Se llama al método necesario para registrar el nuevo email en Firebase
                        registerNewEmail(email.getText().toString(), password.getText().toString());
                    }else{
                        Toast.makeText(RegisterActivity.this, "Contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(RegisterActivity.this, "Campos obligatorios incompletos", Toast.LENGTH_SHORT).show();
                }
            }
        }


    //Muestra la barra de progreso
    private void showDialog(){
        progressBar.setVisibility(View.VISIBLE);

    }

    //Esconde la barra de progreso
    private void hideDialog(){
        if(progressBar.getVisibility() == View.VISIBLE){
            progressBar.setVisibility(View.INVISIBLE);
        }
    }
}