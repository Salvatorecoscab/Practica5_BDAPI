package com.example.crudfirebaseapp.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.crudfirebaseapp.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class AddUserDialog(private val onUserAdded: () -> Unit) : DialogFragment() {

    private lateinit var nameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var isAdminCheckBox: CheckBox
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_add_user, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar vistas
        nameEditText = view.findViewById(R.id.edit_name)
        emailEditText = view.findViewById(R.id.edit_email)
        passwordEditText = view.findViewById(R.id.edit_password)
        confirmPasswordEditText = view.findViewById(R.id.edit_confirm_password)
        isAdminCheckBox = view.findViewById(R.id.checkbox_admin)
        saveButton = view.findViewById(R.id.button_save)
        cancelButton = view.findViewById(R.id.button_cancel)

        // Configurar botones
        saveButton.setOnClickListener {
            addUser()
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

    private fun addUser() {
        val name = nameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()
        val isAdmin = isAdminCheckBox.isChecked

        // Validación
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(requireContext(), "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        // Deshabilitar botón para evitar múltiples envíos
        saveButton.isEnabled = false

        // Crear usuario en Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid
                if (userId != null) {
                    // Añadir a Realtime Database
                    val userRef = database.getReference("users").child(userId)

                    val userData = HashMap<String, Any>()
                    userData["uid"] = userId
                    userData["name"] = name
                    userData["email"] = email
                    userData["isAdmin"] = isAdmin
                    userData["profileImageUrl"] = ""
                    userData["createdAt"] = ServerValue.TIMESTAMP

                    userRef.setValue(userData)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Usuario creado con éxito", Toast.LENGTH_SHORT).show()
                            onUserAdded()
                            dismiss()
                        }
                        .addOnFailureListener { e ->
                            saveButton.isEnabled = true
                            Toast.makeText(requireContext(), "Error al guardar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                saveButton.isEnabled = true
                Toast.makeText(requireContext(), "Error al crear cuenta: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}