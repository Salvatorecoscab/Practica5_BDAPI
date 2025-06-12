package com.example.crudfirebaseapp.dialogs

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.crudfirebaseapp.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import java.util.UUID

class EditProfileDialog(
    private val userId: String,
    private val currentName: String,
    private val currentProfileImageUrl: String,
    private val onProfileUpdated: () -> Unit
) : DialogFragment() {

    private lateinit var nameEditText: TextInputEditText
    private lateinit var profileImageView: CircleImageView
    private lateinit var selectImageButton: Button
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    private val database = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var selectedImageUri: Uri? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar vistas
        nameEditText = view.findViewById(R.id.edit_name)
        profileImageView = view.findViewById(R.id.profile_image)
        selectImageButton = view.findViewById(R.id.button_select_image)
        saveButton = view.findViewById(R.id.button_save)
        cancelButton = view.findViewById(R.id.button_cancel)

        // Cargar datos actuales
        nameEditText.setText(currentName)

        // Cargar imagen de perfil actual
        if (currentProfileImageUrl.isNotEmpty()) {
            Glide.with(requireContext())
                .load(currentProfileImageUrl)
                .placeholder(R.drawable.default_profile)
                .into(profileImageView)
        }

        // Configurar selección de imagen
        selectImageButton.setOnClickListener {
            openImagePicker()
        }

        // Configurar botones
        saveButton.setOnClickListener {
            updateProfile()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            dialog.window?.setLayout(width, height)
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
        selectedImageUri = data.data

            // Mostrar imagen seleccionada
            Glide.with(requireContext())
                .load(selectedImageUri)
                .into(profileImageView)
        }
    }

    private fun updateProfile() {
        val name = nameEditText.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
            return
        }

        // Deshabilitar botón para evitar múltiples envíos
        saveButton.isEnabled = false

        if (selectedImageUri != null) {
            // Primero subir la imagen, luego actualizar perfil
            uploadImageAndUpdateProfile(name)
        } else {
            // Solo actualizar el perfil sin cambiar la imagen
            updateUserProfile(name, currentProfileImageUrl)
        }
    }

    private fun uploadImageAndUpdateProfile(name: String) {
        val imageFileName = "profile_images/${userId}_${UUID.randomUUID()}.jpg"
        val imageRef = storage.reference.child(imageFileName)

        Toast.makeText(requireContext(), "Subiendo imagen...", Toast.LENGTH_SHORT).show()

        selectedImageUri?.let { uri ->
            imageRef.putFile(uri)
                .addOnSuccessListener {
                    // Obtener URL de descarga
                    imageRef.downloadUrl
                        .addOnSuccessListener { downloadUrl ->
                            updateUserProfile(name, downloadUrl.toString())
                        }
                        .addOnFailureListener { e ->
                            saveButton.isEnabled = true
                            Toast.makeText(requireContext(), "Error al obtener URL: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    saveButton.isEnabled = true
                    Toast.makeText(requireContext(), "Error al subir imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                    saveButton.text = "Subiendo ${progress.toInt()}%"
                }
        }
    }

    private fun updateUserProfile(name: String, profileImageUrl: String) {
        val userRef = database.getReference("users").child(userId)

        val updates = HashMap<String, Any>()
        updates["name"] = name

        // Solo actualizar URL de imagen si ha cambiado
        if (profileImageUrl != currentProfileImageUrl) {
            updates["profileImageUrl"] = profileImageUrl
        }

        userRef.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
                onProfileUpdated()
                dismiss()
            }
            .addOnFailureListener { e ->
                saveButton.isEnabled = true
                Toast.makeText(requireContext(), "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}