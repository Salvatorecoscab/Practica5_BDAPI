package com.example.crudfirebaseapp.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.crudfirebaseapp.FirebaseUtils // Asumo que tienes esta clase o puedes quitarla si no se usa
import com.example.crudfirebaseapp.R
import com.example.crudfirebaseapp.dialogs.EditProfileDialog
import com.example.crudfirebaseapp.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import de.hdodenhof.circleimageview.CircleImageView

class ProfileFragment : Fragment() {
    private lateinit var logoutButton: Button
    private lateinit var editProfileButton: FloatingActionButton
    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView
    private lateinit var userRoleTextView: TextView
    private lateinit var profileImageView: CircleImageView
    private lateinit var sessionManager: SessionManager

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    private var profileImageUrl = ""

    companion object {
        private const val TAG = "ProfileFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logoutButton = view.findViewById(R.id.button_logout)
        editProfileButton = view.findViewById(R.id.button_edit_profile)
        userNameTextView = view.findViewById(R.id.text_user_name)
        userEmailTextView = view.findViewById(R.id.text_user_email)
        userRoleTextView = view.findViewById(R.id.text_user_role)
        profileImageView = view.findViewById(R.id.profile_image)

        userNameTextView.text = sessionManager.getUserName() ?: "Usuario"
        userEmailTextView.text = sessionManager.getUserEmail() ?: "correo@ejemplo.com"
        userRoleTextView.text = if (sessionManager.isAdmin()) "Administrador" else "Usuario"

        loadUserProfile()

        editProfileButton.setOnClickListener {
            showEditProfileDialog()
        }

        logoutButton.setOnClickListener {
            // Iniciar el proceso de logout que ahora incluye la eliminación del token
            attemptFcmTokenRemovalAndLogout()
        }
    }

    private fun loadUserProfile() {
        val userId = sessionManager.getUserId()
        if (userId.isEmpty()) {
            Log.w(TAG, "User ID is empty in session manager, cannot load profile.")
            // Podrías manejar esto, por ejemplo, forzando el logout si el ID no es válido
            return
        }

        database.getReference("users").child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isAdded || context == null) return

                    if (snapshot.exists()) {
                        val name = snapshot.child("name").getValue(String::class.java) ?: ""
                        val email = snapshot.child("email").getValue(String::class.java) ?: ""
                        val isAdmin = snapshot.child("isAdmin").getValue(Boolean::class.java) ?: false
                        profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java) ?: ""

                        userNameTextView.text = name
                        userEmailTextView.text = email
                        userRoleTextView.text = if (isAdmin) "Administrador" else "Usuario"

                        sessionManager.createLoginSession(userId, email, isAdmin, name)

                        if (profileImageUrl.isNotEmpty()) {
                            Glide.with(requireContext())
                                .load(profileImageUrl)
                                .placeholder(R.drawable.default_profile)
                                .into(profileImageView)
                        } else {
                            profileImageView.setImageResource(R.drawable.default_profile)
                        }
                    } else {
                        Log.w(TAG, "User profile snapshot does not exist for UID: $userId")
                        // El usuario podría haber sido eliminado, manejar apropiadamente
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (!isAdded || context == null) return
                    Log.e(TAG, "Error al cargar perfil: ${error.message}")
                    Toast.makeText(requireContext(), "Error al cargar perfil: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showEditProfileDialog() {
        val userId = sessionManager.getUserId()
        if (userId.isEmpty()) {
            Toast.makeText(requireContext(), "Error: Usuario no identificado.", Toast.LENGTH_SHORT).show()
            return
        }
        val dialog = EditProfileDialog(
            userId = userId,
            currentName = userNameTextView.text.toString(),
            currentProfileImageUrl = profileImageUrl
        ) {
            // El ValueEventListener en loadUserProfile se encargará de actualizar la UI
        }
        dialog.show(childFragmentManager, "EditProfileDialog")
    }

    private fun attemptFcmTokenRemovalAndLogout() {
        val currentFirebaseUser = auth.currentUser
        val userId = currentFirebaseUser?.uid

        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "User ID es null o vacío al intentar cerrar sesión. Procediendo con logout básico.")
            performActualLogoutActions() // Proceder con el logout si no hay UID
            return
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed during logout", task.exception)
                performActualLogoutActions() // Continuar con el logout aunque falle la obtención del token
                return@addOnCompleteListener
            }

            val fcmToken = task.result
            if (fcmToken != null && fcmToken.isNotEmpty()) {
                Log.d(TAG, "Attempting to remove FCM Token: $fcmToken for user: $userId")
                removeFcmTokenFromDatabase(userId, fcmToken) { success ->
                    if (success) {
                        Log.i(TAG, "FCM token removed successfully from database.")
                    } else {
                        Log.w(TAG, "Failed to remove FCM token from database or token was not present.")
                    }
                    performActualLogoutActions() // Continuar con el logout después del intento de eliminación
                }
            } else {
                Log.w(TAG, "FCM token is null or empty, cannot remove from DB.")
                performActualLogoutActions() // Continuar con el logout si no hay token
            }
        }
    }

    private fun removeFcmTokenFromDatabase(userId: String, fcmToken: String, callback: (Boolean) -> Unit) {
        val tokenPath = "/users/$userId/fcmTokens/$fcmToken"
        database.getReference(tokenPath).removeValue()
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error removing FCM token from $tokenPath", e)
                callback(false)
            }
    }

    private fun performActualLogoutActions() {
        // Asegurarse de que estas acciones se ejecuten en el hilo principal si actualizan la UI
        activity?.runOnUiThread {
            sessionManager.logoutUser() // Limpia datos de sesión locales
            auth.signOut() // Cierra sesión en Firebase Auth

            // Actualizar UI de BottomNavigationView
            val navView = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
            navView?.menu?.findItem(R.id.navigation_login)?.isVisible = true
            navView?.menu?.findItem(R.id.navigation_register)?.isVisible = true
            navView?.menu?.findItem(R.id.navigation_admin)?.isVisible = false
            navView?.menu?.findItem(R.id.navigation_profile)?.isVisible = false

            // Seleccionar la pestaña de login por defecto
            navView?.selectedItemId = R.id.navigation_login

            Toast.makeText(requireContext(), "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "User logout process completed.")
        }
    }
}
