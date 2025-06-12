package com.example.crudfirebaseapp.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup // Importar RadioGroup
import android.widget.Toast
import com.example.crudfirebaseapp.BuildConfig // Importar BuildConfig
import com.example.crudfirebaseapp.FirebaseUtils
import com.example.crudfirebaseapp.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout // Importar TextInputLayout
import com.google.firebase.auth.FirebaseAuth
// import com.google.firebase.firestore.FirebaseFirestore // No se está usando para el registro
import com.example.crudfirebaseapp.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class RegisterFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    // private lateinit var db: FirebaseFirestore // Firestore no se usa en el código de registro actual
    private lateinit var nameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText // Añadir referencia
    private lateinit var registerButton: Button
    private lateinit var roleRadioGroup: RadioGroup
    private lateinit var radioAdmin: RadioButton
    private lateinit var radioUser: RadioButton // Referencia al RadioButton de usuario

    // Nuevos campos para la contraseña de administrador
    private lateinit var adminPasswordLayout: TextInputLayout
    private lateinit var adminPasswordEditText: TextInputEditText

    // Obtener la contraseña de admin desde BuildConfig
    // Asegúrate de que 'adminPass' esté definido en tu build.gradle (app) y local.properties
    private val masterAdminPass = BuildConfig.adminPass

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        // db = FirebaseFirestore.getInstance() // No se usa para el registro de usuarios según el código provisto
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nameEditText = view.findViewById(R.id.edit_text_name)
        emailEditText = view.findViewById(R.id.email_edit)
        passwordEditText = view.findViewById(R.id.password_edit)
        confirmPasswordEditText = view.findViewById(R.id.confirm_password_edit) // Inicializar
        registerButton = view.findViewById(R.id.button_register)

        roleRadioGroup = view.findViewById(R.id.role_radio_group)
        radioAdmin = view.findViewById(R.id.radio_admin)
        radioUser = view.findViewById(R.id.radio_user)

        adminPasswordLayout = view.findViewById(R.id.admin_password_layout)
        adminPasswordEditText = view.findViewById(R.id.admin_password_edit)

        // Listener para el RadioGroup para mostrar/ocultar el campo de contraseña de admin
        roleRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radio_admin) {
                adminPasswordLayout.visibility = View.VISIBLE
            } else {
                adminPasswordLayout.visibility = View.GONE
                adminPasswordEditText.text?.clear() // Limpiar el campo si se cambia de rol
                adminPasswordLayout.error = null // Limpiar cualquier error previo
            }
        }

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                passwordEditText.error = "La contraseña debe tener al menos 6 caracteres"
                Toast.makeText(requireContext(), "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                passwordEditText.error = null
            }


            if (password != confirmPassword) {
                confirmPasswordEditText.error = "Las contraseñas no coinciden"
                Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                confirmPasswordEditText.error = null
            }

            val isAdmin = radioAdmin.isChecked

            if (isAdmin) {
                val enteredAdminPass = adminPasswordEditText.text.toString().trim()
                if (enteredAdminPass.isEmpty()) {
                    adminPasswordLayout.error = "Contraseña de administrador requerida"
                    //Toast.makeText(requireContext(), "Por favor, ingrese la contraseña de administrador", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (enteredAdminPass != masterAdminPass) {
                    adminPasswordLayout.error = "Contraseña de administrador incorrecta"
                    //Toast.makeText(requireContext(), "Contraseña de administrador incorrecta", Toast.LENGTH_SHORT).show()
                    adminPasswordEditText.text?.clear()
                    return@setOnClickListener
                }
                adminPasswordLayout.error = null // Limpiar error si la contraseña es correcta
            }

            // Registrar usuario
            registerUser(name, email, password, isAdmin)
        }
    }

    private fun registerUser(name: String, email: String, password: String, isAdmin: Boolean) {
        Log.d("RegisterFragment", "Iniciando registro de usuario: $email, isAdmin: $isAdmin")
        registerButton.isEnabled = false // Deshabilitar botón durante el proceso

        // Aquí podrías mostrar un ProgressDialog si lo deseas
        // val progressDialog = ProgressDialog(requireContext()).apply {
        //     setMessage("Registrando...")
        //     setCancelable(false)
        //     show()
        // }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                // progressDialog.dismiss() // Ocultar ProgressDialog

                if (!isAdded) { // Verificar si el fragmento sigue adjunto
                    Log.w("RegisterFragment", "Fragmento no adjunto, abortando operación de registro.")
                    if (registerButton.isEnabled.not()) hideProgressAndEnableButton()
                    return@addOnCompleteListener
                }

                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        Log.d("RegisterFragment", "Usuario creado en Authentication. UID: ${user.uid}")

                        createUserDocumentInRealtimeDatabase(user.uid, name, email, isAdmin)
                        FirebaseUtils.updateFCMTokenForCurrentUser()

                    } else {
                        hideProgressAndEnableButton()
                        Toast.makeText(requireContext(), "Error: Usuario nulo después de registro", Toast.LENGTH_SHORT).show()
                        Log.e("RegisterFragment", "Usuario nulo después de registro exitoso")
                    }
                } else {
                    hideProgressAndEnableButton()
                    val errorMessage = task.exception?.message ?: "Error de registro desconocido"
                    Toast.makeText(requireContext(), "Error de autenticación: $errorMessage", Toast.LENGTH_SHORT).show()
                    Log.e("RegisterFragment", "Error de registro en Authentication", task.exception)
                }
            }
    }

    private fun createUserDocumentInRealtimeDatabase(userId: String, name: String, email: String, isAdmin: Boolean) {
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users") // "users" es la ruta raíz para los usuarios

        val userData = HashMap<String, Any>()
        userData["uid"] = userId
        userData["name"] = name
        userData["email"] = email
        userData["isAdmin"] = isAdmin
        userData["profileImageUrl"] = "" // Valor por defecto o puedes dejarlo vacío
        userData["createdAt"] = ServerValue.TIMESTAMP

        val timeout = 30000L // 30 segundos
        val handler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (isAdded) {
                hideProgressAndEnableButton()
                Toast.makeText(requireContext(), "La operación ha tardado demasiado. Inténtalo de nuevo.", Toast.LENGTH_LONG).show()
                Log.e("RegisterFragment", "Timeout al crear usuario en Realtime Database")
            }
        }
        handler.postDelayed(timeoutRunnable, timeout)

        usersRef.child(userId).setValue(userData)
            .addOnSuccessListener {
                handler.removeCallbacks(timeoutRunnable)
                if (!isAdded) {
                    Log.w("RegisterFragment", "Fragmento no adjunto, abortando success listener de Realtime Database.")
                    return@addOnSuccessListener
                }
                Log.d("RegisterFragment", "Usuario creado con éxito en Realtime Database. UID: $userId")

                val sessionManager = SessionManager(requireContext())
                sessionManager.createLoginSession(userId, email, isAdmin, name)

                hideProgressAndEnableButton()
                clearInputFields() // Limpiar campos después de registro exitoso
                navigateBasedOnRole(isAdmin)
            }
            .addOnFailureListener { e ->
                handler.removeCallbacks(timeoutRunnable)
                if (!isAdded) {
                    Log.w("RegisterFragment", "Fragmento no adjunto, abortando failure listener de Realtime Database.")
                    return@addOnFailureListener
                }
                hideProgressAndEnableButton()
                Log.e("RegisterFragment", "Error al crear usuario en Realtime Database", e)
                Toast.makeText(requireContext(), "Error al guardar perfil en base de datos: ${e.message}", Toast.LENGTH_SHORT).show()

                // Considerar revertir la creación del usuario en Firebase Auth si falla la creación en Realtime Database
                // auth.currentUser?.delete()?.addOnCompleteListener { deleteTask ->
                //     if (deleteTask.isSuccessful) {
                //         Log.d("RegisterFragment", "Usuario de Auth revertido debido a fallo en Realtime DB.")
                //     } else {
                //         Log.e("RegisterFragment", "Error al revertir usuario de Auth.", deleteTask.exception)
                //     }
                // }
            }
    }

    private fun clearInputFields() {
        if (!isAdded) return
        nameEditText.text?.clear()
        emailEditText.text?.clear()
        passwordEditText.text?.clear()
        confirmPasswordEditText.text?.clear()
        adminPasswordEditText.text?.clear()
        adminPasswordLayout.error = null // Limpiar error del campo admin
        passwordEditText.error = null // Limpiar error del campo contraseña
        confirmPasswordEditText.error = null // Limpiar error del campo confirmar contraseña


        // Opcionalmente, resetear el RadioGroup al rol por defecto (ej. usuario) y ocultar el campo de admin
        if (radioUser.isChecked.not()){
            radioUser.isChecked = true // Esto disparará el listener y ocultará el campo admin si está visible
        } else {
            // Si ya estaba en usuario, nos aseguramos que el campo admin esté oculto por si acaso
            adminPasswordLayout.visibility = View.GONE
        }

    }


    private fun navigateBasedOnRole(isAdmin: Boolean) {
        if (activity == null || !isAdded) {
            Log.w("RegisterFragment", "Actividad nula o fragmento no adjunto al intentar navegar.")
            return
        }

        val navView = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)

        navView.menu.findItem(R.id.navigation_login).isVisible = false
        navView.menu.findItem(R.id.navigation_register).isVisible = false
        navView.menu.findItem(R.id.navigation_admin).isVisible = isAdmin
        navView.menu.findItem(R.id.navigation_profile).isVisible = true // Asumo que perfil siempre es visible post-login

        val destinationId = if (isAdmin) R.id.navigation_admin else R.id.navigation_profile
        if (navView.selectedItemId != destinationId) { // Evitar recarga si ya está en el destino
            navView.selectedItemId = destinationId
        }

        Toast.makeText(
            requireContext(),
            "Registro exitoso como ${if (isAdmin) "administrador" else "usuario"}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun hideProgressAndEnableButton() {
        if (isAdded) { // Solo interactuar con la UI si el fragmento está adjunto
            registerButton.isEnabled = true
        }
    }
}