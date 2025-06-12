package com.example.crudfirebaseapp.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import com.example.crudfirebaseapp.FirebaseUtils
import com.example.crudfirebaseapp.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.example.crudfirebaseapp.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar vistas
        emailEditText = view.findViewById(R.id.edit_text_email)
        passwordEditText = view.findViewById(R.id.edit_text_password)
        loginButton = view.findViewById(R.id.button_login)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Mostrar progreso y deshabilitar botón
            loginButton.isEnabled = false

            // Login simplificado para evitar callbacks anidados
            loginUser(email, password)
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!isAdded) return@addOnCompleteListener // Verificar si el fragmento sigue adjunto

                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        Toast.makeText(requireContext(), "Autenticación exitosa, verificando rol...", Toast.LENGTH_SHORT).show()
                        FirebaseUtils.updateFCMTokenForCurrentUser()

                        // Verificar rol en Firestore
                        checkAdminStatus(user.uid)
                    } else {
                        hideProgressAndEnableButton()
                        Toast.makeText(requireContext(), "Error: Usuario nulo después de autenticación", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    hideProgressAndEnableButton()
                    val errorMessage = task.exception?.message ?: "Error de autenticación"
                    Toast.makeText(requireContext(), "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                    Log.e("LoginFragment", "Error de autenticación", task.exception)
                }
            }
    }

    private fun checkAdminStatus(userId: String) {
        // Usar Realtime Database
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("users").child(userId)

        Log.d("LoginFragment", "Verificando estado de admin para userId: $userId")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                if (snapshot.exists()) {
                    val isAdmin = snapshot.child("isAdmin").getValue(Boolean::class.java) ?: false
                    val name = snapshot.child("name").getValue(String::class.java) ?: ""

                    Log.d("LoginFragment", "Usuario encontrado. isAdmin: $isAdmin, nombre: $name")

                    // Crear sesión
                    val sessionManager = SessionManager(requireContext())
                    sessionManager.createLoginSession(userId, auth.currentUser?.email ?: "", isAdmin, name)

                    // Navegación basada en rol
                    navigateBasedOnRole(isAdmin)
                } else {
                    Log.w("LoginFragment", "Usuario no encontrado en la base de datos")

                    // Crear usuario básico si no existe
                    val userData = HashMap<String, Any>()
                    userData["uid"] = userId
                    userData["name"] = auth.currentUser?.email?.substringBefore('@') ?: "Usuario"
                    userData["email"] = auth.currentUser?.email ?: ""
                    userData["isAdmin"] = false
                    userData["createdAt"] = ServerValue.TIMESTAMP

                    userRef.setValue(userData)
                        .addOnSuccessListener {
                            // Crear sesión para usuario normal
                            val sessionManager = SessionManager(requireContext())
                            sessionManager.createLoginSession(
                                userId,
                                auth.currentUser?.email ?: "",
                                false,
                                auth.currentUser?.email?.substringBefore('@') ?: "Usuario"
                            )

                            // Navegación para usuario normal
                            navigateBasedOnRole(false)
                        }
                        .addOnFailureListener { e ->
                            Log.e("LoginFragment", "Error al crear usuario en Database", e)

                            // Aún permitir login como usuario normal
                            val sessionManager = SessionManager(requireContext())
                            sessionManager.createLoginSession(userId, auth.currentUser?.email ?: "", false, "Usuario")
                            navigateBasedOnRole(false)
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) return

                Log.e("LoginFragment", "Error al verificar admin: ${error.message}")
                Toast.makeText(requireContext(), "Error de conectividad: ${error.message}", Toast.LENGTH_SHORT).show()

                // Permitir login como usuario normal en caso de error
                val sessionManager = SessionManager(requireContext())
                sessionManager.createLoginSession(userId, auth.currentUser?.email ?: "", false, "Usuario")
                navigateBasedOnRole(false)
            }
        })
    }
    private fun createUserDocumentInFirestore(userId: String, email: String, isAdmin: Boolean) {
        val userData = hashMapOf(
            "uid" to userId,
            "email" to email,
            "isAdmin" to isAdmin,
            "name" to email.substringBefore('@'),
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener

                Log.d("LoginFragment", "Documento de usuario creado con éxito en Firestore")

                // Crear sesión
                val sessionManager = SessionManager(requireContext())
                sessionManager.createLoginSession(userId, email, isAdmin, email.substringBefore('@'))

                // Ocultar progreso
                hideProgressAndEnableButton()

                // Navegación basada en rol
                navigateBasedOnRole(isAdmin)
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener

                hideProgressAndEnableButton()
                Log.e("LoginFragment", "Error al crear documento de usuario", e)
                Toast.makeText(requireContext(), "Error al crear perfil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateBasedOnRole(isAdmin: Boolean) {
        // Actualizar navegación
        val navView = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Ocultar/mostrar elementos del menú según el rol
        navView.menu.findItem(R.id.navigation_login).isVisible = false
        navView.menu.findItem(R.id.navigation_register).isVisible = false
        navView.menu.findItem(R.id.navigation_admin).isVisible = isAdmin
        navView.menu.findItem(R.id.navigation_profile).isVisible = true

        // Navegar al fragmento correspondiente
        val destinationId = if (isAdmin) R.id.navigation_admin else R.id.navigation_profile
        navView.selectedItemId = destinationId

        Toast.makeText(
            requireContext(),
            "Inicio de sesión exitoso como ${if (isAdmin) "administrador" else "usuario"}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun hideProgressAndEnableButton() {
        loginButton.isEnabled = true
    }
}