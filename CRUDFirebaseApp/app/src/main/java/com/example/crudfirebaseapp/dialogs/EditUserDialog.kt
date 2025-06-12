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
import android.widget.CheckBox
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.crudfirebaseapp.R
import com.example.crudfirebaseapp.models.User
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import java.util.UUID

class EditUserDialog(
    private val user: User,
    private val onUserUpdated: () -> Unit
) : DialogFragment() {

    private lateinit var nameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var isAdminCheckBox: CheckBox
    private lateinit var profileImageView: CircleImageView
    private lateinit var selectImageButton: Button
    private lateinit var resetPasswordButton: Button
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    private val database = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var selectedImageUri: Uri? = null
    private var isUploadingImage = false

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_edit_user, container, false)
    }

    @Override
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar vistas
        nameEditText = view.findViewById(R.id.edit_name)
        emailEditText = view.findViewById(R.id.edit_email)
        isAdminCheckBox = view.findViewById(R.id.checkbox_admin)
        profileImageView = view.findViewById(R.id.profile_image)
        selectImageButton = view.findViewById(R.id.button_select_image)
        resetPasswordButton = view.findViewById(R.id.button_reset_password)
        saveButton = view.findViewById(R.id.button_save)
        cancelButton = view.findViewById(R.id.button_cancel)

        // Cargar datos del usuario - CORREGIDO
        nameEditText.setText(user.name)  // Asegurarse de que este sea el nombre real
        emailEditText.setText(user.email) // Asegurarse de que este sea el email real
        isAdminCheckBox.isChecked = user.isAdmin

        // Cargar imagen de perfil
        if (user.profileImageUrl.isNotEmpty()) {
            Glide.with(requireContext())
                .load(user.profileImageUrl)
                .placeholder(R.drawable.default_profile)
                .into(profileImageView)
        }

        // Email no se puede editar (restricción de Firebase Auth)
        emailEditText.isEnabled = false

        // Configurar selección de imagen
        selectImageButton.setOnClickListener {
            openImagePicker()
        }

        // Configurar botones
        saveButton.setOnClickListener {
            updateUser()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }

        resetPasswordButton.setOnClickListener {
            resetPassword()
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

    private fun updateUser() {
        val name = nameEditText.text.toString().trim()
        val isAdmin = isAdminCheckBox.isChecked

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
            return
        }

        // Deshabilitar botón para evitar múltiples envíos
        saveButton.isEnabled = false

        if (selectedImageUri != null) {
            // Primero subir la imagen, luego actualizar perfil
            uploadImageAndUpdateProfile(name, isAdmin)
        } else {
            // Solo actualizar el perfil sin cambiar la imagen
            updateUserProfile(name, isAdmin, user.profileImageUrl)
        }
    }

    private fun uploadImageAndUpdateProfile(name: String, isAdmin: Boolean) {
        isUploadingImage = true
        val imageFileName = "profile_images/${user.uid}_${UUID.randomUUID()}.jpg"
        val imageRef = storage.reference.child(imageFileName)

        Toast.makeText(requireContext(), "Subiendo imagen...", Toast.LENGTH_SHORT).show()

        selectedImageUri?.let { uri ->
            imageRef.putFile(uri)
                .addOnSuccessListener {
                    // Obtener URL de descarga
                    imageRef.downloadUrl
                        .addOnSuccessListener { downloadUrl ->
                            isUploadingImage = false
                            updateUserProfile(name, isAdmin, downloadUrl.toString())
                        }
                        .addOnFailureListener { e ->
                            isUploadingImage = false
                            saveButton.isEnabled = true
                            Toast.makeText(requireContext(), "Error al obtener URL: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    isUploadingImage = false
                    saveButton.isEnabled = true
                    Toast.makeText(requireContext(), "Error al subir imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                    saveButton.text = "Subiendo ${progress.toInt()}%"
                }
        }
    }

    private fun updateUserProfile(name: String, isAdmin: Boolean, profileImageUrl: String) {
        val userRef = database.getReference("users").child(user.uid)

        val updates = HashMap<String, Any>()
        updates["name"] = name
        updates["isAdmin"] = isAdmin

        // Solo actualizar URL de imagen si ha cambiado
        if (profileImageUrl != user.profileImageUrl) {
            updates["profileImageUrl"] = profileImageUrl
        }

        userRef.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Usuario actualizado correctamente", Toast.LENGTH_SHORT).show()
                onUserUpdated()
                dismiss()
            }
            .addOnFailureListener { e ->
                saveButton.isEnabled = true
                Toast.makeText(requireContext(), "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun resetPassword() {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

        auth.sendPasswordResetEmail(user.email)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Email de restablecimiento enviado a ${user.email}", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al enviar email: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}