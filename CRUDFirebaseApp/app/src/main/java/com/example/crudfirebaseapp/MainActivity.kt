package com.example.crudfirebaseapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.crudfirebaseapp.fragments.AdminFragment
import com.example.crudfirebaseapp.fragments.LoginFragment
import com.example.crudfirebaseapp.fragments.ProfileFragment
import com.example.crudfirebaseapp.fragments.RegisterFragment
import com.example.crudfirebaseapp.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

private const val REQUEST_CODE_POST_NOTIFICATIONS = 101

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var sessionManager: SessionManager
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        askNotificationPermission()

        val options = FirebaseApp.getInstance().options
        Log.i("FIREBASE_INFO", "ProjectId=${options.projectId}, AppId=${options.applicationId}")

        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val idToken = task.result?.token
                Log.d("MY_APP_TOKEN", "Firebase ID Token: $idToken")
            } else {
                Log.e("MY_APP_TOKEN", "Error obteniendo ID Token", task.exception)
            }
        }

        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_POST_NOTIFICATIONS)
            }
        }

        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) {
            Log.e("MainActivity", "Persistencia ya estaba habilitada: ${e.message}")
        }

        bottomNavigation = findViewById(R.id.bottom_navigation)
        sessionManager = SessionManager(this)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setupBottomNavigation()

        val currentUser = auth.currentUser
        if (currentUser != null && sessionManager.isLoggedIn()) {
            Log.d("MainActivity", "Usuario autenticado: ${currentUser.uid}")
            FirebaseUtils.updateFCMTokenForCurrentUser()

            val isAdmin = sessionManager.isAdmin()
            updateUIBasedOnRole(isAdmin)

            if (savedInstanceState == null) {
                loadFragment(if (isAdmin) AdminFragment() else ProfileFragment())
            }

            verifyAdminRole(currentUser.uid)
        } else {
            Log.d("MainActivity", "Sin sesión activa, mostrando login")
            if (savedInstanceState == null) {
                loadFragment(LoginFragment())
                updateUIForLoggedOut()
            }
        }

        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null && sessionManager.isLoggedIn()) {
                Log.d("MainActivity", "Sesión cerrada remotamente")
                sessionManager.logoutUser()
                updateUIForLoggedOut()
                loadFragment(LoginFragment())
                Toast.makeText(this, "Tu sesión ha finalizado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyAdminRole(userId: String) {
        val userRef = database.getReference("users").child(userId)
        userRef.keepSynced(true)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val isAdmin = snapshot.child("isAdmin").getValue(Boolean::class.java) ?: false
                    val name = snapshot.child("name").getValue(String::class.java) ?: ""
                    val email = snapshot.child("email").getValue(String::class.java) ?: ""

                    if (isAdmin != sessionManager.isAdmin()) {
                        Log.d("MainActivity", "Actualizando rol a isAdmin=$isAdmin")
                        sessionManager.createLoginSession(userId, email, isAdmin, name)
                        updateUIBasedOnRole(isAdmin)

                        if (!isAdmin && bottomNavigation.selectedItemId == R.id.navigation_admin) {
                            bottomNavigation.selectedItemId = R.id.navigation_profile
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity", "Error al verificar rol: ${error.message}")
            }
        })
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_login -> {
                    if (!sessionManager.isLoggedIn()) {
                        loadFragment(LoginFragment())
                        true
                    } else false
                }
                R.id.navigation_register -> {
                    if (!sessionManager.isLoggedIn()) {
                        loadFragment(RegisterFragment())
                        true
                    } else false
                }
                R.id.navigation_admin -> {
                    if (sessionManager.isLoggedIn() && sessionManager.isAdmin()) {
                        loadFragment(AdminFragment())
                        true
                    } else {
                        Toast.makeText(this, "Acceso denegado. Debes ser administrador.", Toast.LENGTH_SHORT).show()
                        bottomNavigation.selectedItemId = if (sessionManager.isLoggedIn()) R.id.navigation_profile else R.id.navigation_login
                        false
                    }
                }
                R.id.navigation_profile -> {
                    if (sessionManager.isLoggedIn()) {
                        loadFragment(ProfileFragment())
                        true
                    } else {
                        Toast.makeText(this, "Debes iniciar sesión primero", Toast.LENGTH_SHORT).show()
                        loadFragment(LoginFragment())
                        bottomNavigation.selectedItemId = R.id.navigation_login
                        false
                    }
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        return true
    }

    private fun updateUIBasedOnRole(isAdmin: Boolean) {
        bottomNavigation.menu.findItem(R.id.navigation_admin).isVisible = isAdmin
        bottomNavigation.menu.findItem(R.id.navigation_login).isVisible = false
        bottomNavigation.menu.findItem(R.id.navigation_register).isVisible = false
        bottomNavigation.menu.findItem(R.id.navigation_profile).isVisible = true
    }

    private fun updateUIForLoggedOut() {
        bottomNavigation.menu.findItem(R.id.navigation_login).isVisible = true
        bottomNavigation.menu.findItem(R.id.navigation_register).isVisible = true
        bottomNavigation.menu.findItem(R.id.navigation_admin).isVisible = false
        bottomNavigation.menu.findItem(R.id.navigation_profile).isVisible = false
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("Permissions", "Permiso de notificación concedido")
        } else {
            Log.d("Permissions", "Permiso de notificación denegado")
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED -> {
                    Log.d("Permissions", "Permiso ya concedido")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Muestra un mensaje educativo aquí si lo deseas
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}
