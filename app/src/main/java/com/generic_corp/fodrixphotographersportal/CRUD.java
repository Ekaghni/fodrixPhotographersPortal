package com.generic_corp.fodrixphotographersportal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import id.zelory.compressor.Compressor;

public class CRUD extends AppCompatActivity {

    private Button mSaveBtn,updateButton,deleteButton,readButton, uploadImage;
    private EditText ID,Name,number,update_existName,
            update_newName,update_existNumber,update_newNumer,delete_name;
    private FirebaseFirestore db;
    private Uri imageUri;
    private Bitmap compressor;
    private ImageView userImage;
    private ProgressDialog progressDialog;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crud);
        progressDialog = new ProgressDialog(this);
        uploadImage = findViewById(R.id.uploadImageButton);
        storageReference = FirebaseStorage.getInstance().getReference();
        readButton = findViewById(R.id.readButton);
        ID = findViewById(R.id.editTextTextPersonName);
        Name = findViewById(R.id.editTextTextPersonName2);
        number = findViewById(R.id.editTextTextPersonName3);
        mSaveBtn = findViewById(R.id.button2);
        update_existName = findViewById(R.id.update_existingName);
        update_newName = findViewById(R.id.update_newName);
        update_existNumber = findViewById(R.id.update_existingNumber);
        update_newNumer = findViewById(R.id.update_newNumber);
        updateButton = findViewById(R.id.updateButton);
        delete_name = findViewById(R.id.delete_name);
        deleteButton = findViewById(R.id.deleteButton);
        userImage = findViewById(R.id.imageView);
        db= FirebaseFirestore.getInstance();

        //////////Image Upload onClickListner///////
        uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
                    if (ContextCompat.checkSelfPermission(CRUD.this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                            PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(CRUD.this, "Permission Denied", Toast.LENGTH_SHORT).show();
                        ActivityCompat.requestPermissions(CRUD.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
                    }
                    else {
                        ChooseImage();
                    }
                }
                else {
                    ChooseImage();
                }
            }
        });

        mSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id_f = ID.getText().toString();
                //String id_f = UUID.randomUUID().toString();
                String name_f = Name.getText().toString();
                String number_f = number.getText().toString();
                File newFile = new File(imageUri.getPath());
                try {
                    compressor = new Compressor(CRUD.this)
                            .setMaxWidth(125).setMaxWidth(125).setQuality(50).compressToBitmap(newFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                compressor.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                byte[] thumb = byteArrayOutputStream.toByteArray();
                final UploadTask[] image_path = {storageReference.child("Photographer_image").child(name_f + ".jpg").putBytes(thumb)};
                image_path[0].addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {

                            saveToFireStore(id_f, name_f, number_f, imageUri, image_path);


                        }
                        else {
                            Toast.makeText(CRUD.this, "Unable to upload image", Toast.LENGTH_SHORT).show();
                        }
                    }
                });


            }
        });




        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ex_name = update_existName.getText().toString();
                String new_name = update_newName.getText().toString();
                String  ex_number = update_existNumber.getText().toString();
                String new_number = update_newNumer.getText().toString();

                Updatedata(ex_name,new_name,ex_number,new_number);

            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String deleteName = delete_name.getText().toString();
                DeleteData(deleteName);
            }
        });

        readButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Intent i = new Intent(CRUD.this,ReadData.class);
               // startActivity(i);

            }
        });
    }

    private void ChooseImage() {
        CropImage.activity().setGuidelines(CropImageView.Guidelines.ON)

                .start(CRUD.this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode==RESULT_OK){
                imageUri = result.getUri();
                userImage.setImageURI(imageUri);
            }else if (requestCode==CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
                Exception error = result.getError();
                System.out.println("resulttttt---"+error);
            }
        }
    }

    private void DeleteData(String deleteName) {

        db.collection("Documents").whereEqualTo("Name",deleteName).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful() && !task.getResult().isEmpty()){
                    DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                    String documentId = documentSnapshot.getId();
                    db.collection("Documents").document(documentId)
                            .delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Toast.makeText(CRUD.this, "Deleted Successfully", Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(CRUD.this, "Failed delete", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
                else {
                    Toast.makeText(CRUD.this, "Failed to update", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    private void Updatedata(String ex_name, String new_name, String ex_number, String new_number) {

        Map<String,Object> userDetail = new HashMap<>();
        userDetail.put("Name",new_name);
        userDetail.put("Number",new_number);

        db.collection("Documents").whereEqualTo("Name",ex_name).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful() && !task.getResult().isEmpty()){
                    DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                    String documentId = documentSnapshot.getId();
                    db.collection("Documents").document(documentId)
                            .update(userDetail).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Toast.makeText(CRUD.this, "Name Updated Successfully", Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(CRUD.this, "Failed to update name", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
                else {
                    Toast.makeText(CRUD.this, "Failed to update", Toast.LENGTH_SHORT).show();
                }
            }
        });

        db.collection("Documents").whereEqualTo("Number",ex_number).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful() && !task.getResult().isEmpty()){
                    DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                    String documentId = documentSnapshot.getId();
                    db.collection("Documents").document(documentId)
                            .update(userDetail).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Toast.makeText(CRUD.this, "Number Updated Successfully", Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(CRUD.this, "Failed to update number", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
                else {
                    Toast.makeText(CRUD.this, "Failed to update", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void saveToFireStore(String id, String title, String desc, Uri task, UploadTask[] image_path) {

        if (!title.isEmpty() && !desc.isEmpty()) {




            final Uri[] downloadUri = new Uri[1];
            //////////////////////////////
            final StorageReference ref = storageReference.child("Photographer_image/"+title+".jpg");
            image_path[0] = ref.putFile(imageUri);

            Task<Uri> urlTask = image_path[0].continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // Continue with the task to get the download URL
                    return ref.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        downloadUri[0] = task.getResult();
                        System.out.println("ddddddddddd--->"+ downloadUri[0]);


                        HashMap<String, Object> map = new HashMap<>();
                        map.put("Name", id);
                        map.put("Photographer_Type", title);
                        map.put("Number", desc);
                        map.put("Image_Url",downloadUri[0]);
                        db.collection("Documents").document(id).set(map)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(CRUD.this, "Data Saved !!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(CRUD.this, "Failed !!", Toast.LENGTH_SHORT).show();
                                    }
                                });

                    } else {
                        // Handle failures
                        // ...
                    }
                }
            });
            //System.out.println("Uriiiiiiii--->" + map.get("Image_Url"));


            //////////////////////////////






        } else {
            Toast.makeText(this, "Empty Fields not Allowed", Toast.LENGTH_SHORT).show();
        }


    }
}
