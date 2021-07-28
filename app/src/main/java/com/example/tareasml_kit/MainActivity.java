package com.example.tareasml_kit;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.TextRecognizerOptions;

public class MainActivity extends AppCompatActivity {
     //---------------------VRIABLES-----------------------------//
    //---------------------Firebase----------------------//
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    //-------------------------------------------------//

    //-----Identificación-||-texto-||--idioma--||--Traducción---------//
    Translator translator;
    String idomaIdentificado="en";
    TextView resultadoIdentificacion;
    Button btnIdentificarTraducir, btnExplorar;
    EditText txtTraducido, txtEscaneado,resultadoTextoImagen;
    private static final String TAG = "MiTag";
    private static  final int STORAGE_PERMISSION_CODE=113;
    ActivityResultLauncher<Intent> activityResultLauncher;

    InputImage inputImage;
    TextRecognizer textReconocido;

    //-------------------------------------------------------------------------------------//

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Firebase();
        firebaseDatabase= FirebaseDatabase.getInstance();
        databaseReference=firebaseDatabase.getReference();

        resultadoTextoImagen=findViewById(R.id.txtResultadoEscaneo);
        btnExplorar=findViewById(R.id.btnAbrirExplorador);
        textReconocido= TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        activityResultLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        Intent data= result.getData();
                        Uri image=data.getData();
                        convertirImagen(image);
                    }
                });

        btnExplorar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent=new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                activityResultLauncher.launch(intent);


            }
        });
    }
    private void Firebase() {

        FirebaseApp.initializeApp(this);
        firebaseDatabase= FirebaseDatabase.getInstance();
        databaseReference=firebaseDatabase.getReference();
        txtEscaneado=findViewById(R.id.txtResultadoEscaneo);
        btnIdentificarTraducir=findViewById(R.id.btnTraducirTexto);
        resultadoIdentificacion=findViewById(R.id.lblIdentificacion);
        txtTraducido=findViewById(R.id.txtTextoTraducido);

        btnIdentificarTraducir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String lanText=txtEscaneado.getText().toString().replace("\n","");
                if(lanText.equals(""))
                {
                  Toast.makeText(MainActivity.this,"Ingrese un texto por favor", Toast.LENGTH_SHORT).show();
                } else
                    {identificar();}
            }
        });
    }
    public void identificar()
    {
        idomaIdentificado=txtEscaneado.getText().toString();
        prepareModel();
    }
    private void traslaterlenguaje() {
        translator.translate(idomaIdentificado).addOnSuccessListener(new OnSuccessListener<String>(){
            @Override
            public  void onSuccess(String s)
            {
                txtTraducido.setText(s);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull  Exception e) {
                txtTraducido.setText("Error al traducir el texto");
            }
        });
    }
    private void prepareModel() {
        String[] sa=resultadoIdentificacion.getText().toString().split(":");
        String otherL="en";
        if(sa[1].equals("en"))
        {
            otherL="es";
        }
        TextView otro=findViewById(R.id.lblOtherIdioma);
        otro.setText("A: "+otherL);
        TranslatorOptions options =
                new TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.fromLanguageTag(sa[1]))
                        .setTargetLanguage(TranslateLanguage.fromLanguageTag(otherL))
                        .build();
        translator = Translation.getClient(options);
        translator.downloadModelIfNeeded().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                traslaterlenguaje();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
            }
        });
    }
    //----------------------------------------Identificar el texto----------------------------------------------------//
    private void detectarTexto(String texto) {
        LanguageIdentifier languageIdentifier= LanguageIdentification.getClient();
        languageIdentifier.identifyLanguage(texto).addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String languageCode) {
                if(languageCode.equals("und"))
                {resultadoIdentificacion.setText("No se pudo identificar el idioma");}
                else
                {resultadoIdentificacion.setText("DE :"+languageCode);}
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull  Exception e) {
                resultadoIdentificacion.setText("Exception: "+e);
            }
        });
    }
    //--------------------------------------Converti imagen a texto-------------------------------------//
    private void convertirImagen(Uri image) {
        try{
            inputImage=InputImage.fromFilePath(getApplicationContext(),image);
            Task<Text> result=textReconocido.process(inputImage)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(@NonNull Text text) {
                            //String lanText=txtEscaneado.getText().toString().replace("\n","");
                            resultadoTextoImagen.setText(text.getText().replace("\n",""));
                            detectarTexto(text.getText().replace("\n",""));
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            resultadoTextoImagen.setText("Error: "+e.getMessage());
                            Log.d(TAG, "Error: "+e.getMessage());
                        }
                    });

        }catch (Exception e)
        {
         Log.d(TAG,"error:"+e.getMessage());
        }
    }
    //--------------------------------------------Parte de los permisos---------------------------------------------------//
    @Override
    protected void onResume()
    {
        super.onResume();
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE);
    }
    public void checkPermission(String permission, int requestCode)
    {
        if(ContextCompat.checkSelfPermission(MainActivity.this, permission)== PackageManager.PERMISSION_DENIED)
        {
        ActivityCompat.requestPermissions(MainActivity.this,new String[]{permission},requestCode);
        }
    }
    @Override
    public  void onRequestPermissionsResult(int requestCode, @NonNull  String[] permission, @NonNull int[] grantResults )
    {
        super.onRequestPermissionsResult(requestCode, permission, grantResults);
        if(requestCode==STORAGE_PERMISSION_CODE)
        {
          if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED)
          {Toast.makeText(MainActivity.this,"Acepto los permisos", Toast.LENGTH_SHORT).show();}
        }else
          {Toast.makeText(MainActivity.this,"Permisos denegados", Toast.LENGTH_SHORT).show();}
    }
    //--------------------------------------------Parte de los permisos---------------------------------------------------//
}