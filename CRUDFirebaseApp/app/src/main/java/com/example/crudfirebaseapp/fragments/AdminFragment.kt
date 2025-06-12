package com.example.crudfirebaseapp.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.crudfirebaseapp.R
import com.example.crudfirebaseapp.adapters.UserAdapter
import com.example.crudfirebaseapp.dialogs.AddUserDialog
import com.example.crudfirebaseapp.dialogs.EditUserDialog
import com.example.crudfirebaseapp.models.User
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.crudfirebaseapp.utils.SessionManager
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
class AdminFragment : Fragment(), UserAdapter.OnUserClickListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var authRequiredCard: MaterialCardView
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var addUserButton: FloatingActionButton // Puedes mantener este FAB si quieres
    private lateinit var emptyView: TextView
    private lateinit var loadingView: ProgressBar

    private lateinit var userAdapter: UserAdapter
    private val usersList = mutableListOf<User>()
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        sessionManager = SessionManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar vistas
        authRequiredCard = view.findViewById(R.id.auth_required_card)
        usersRecyclerView = view.findViewById(R.id.users_recycler_view)
        addUserButton = view.findViewById(R.id.add_user_button)
        emptyView = view.findViewById(R.id.empty_view)
        loadingView = view.findViewById(R.id.loading_view)

        // Configurar RecyclerView
        usersRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Inicializar adapter
        userAdapter = UserAdapter(usersList, this)
        usersRecyclerView.adapter = userAdapter

        // Mostrar UI y cargar datos solo si es admin
        if (sessionManager.isAdmin()) {
            showAdminUI()
            loadUsers()
        } else {
            showAuthRequiredUI()
            Toast.makeText(requireContext(), "Debes acceder como administrador para ver esta sección", Toast.LENGTH_LONG).show()
        }


// Configurar botón para agregar usuario
        addUserButton.setOnClickListener {
            val addUserDialog = AddUserDialog {
                // Este callback se ejecuta cuando se añade un usuario exitosamente
                loadUsers() // Recargar la lista de usuarios
            }
            addUserDialog.show(childFragmentManager, "AddUserDialog")
        }


    }

    private fun loadUsers() {
        loadingView.visibility = View.VISIBLE
        emptyView.visibility = View.GONE

        val usersRef = database.getReference("users")
        usersRef.keepSynced(true) // Para acceso offline

        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                loadingView.visibility = View.GONE
                usersList.clear()

                for (userSnapshot in snapshot.children) {
                    try {
                        val uid = userSnapshot.key ?: continue
                        val name = userSnapshot.child("name").getValue(String::class.java) ?: ""
                        val email = userSnapshot.child("email").getValue(String::class.java) ?: ""
                        val isAdmin = userSnapshot.child("isAdmin").getValue(Boolean::class.java) ?: false
                        val profileImageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java) ?: ""

                        val user = User(uid, name, email, isAdmin, profileImageUrl)
                        usersList.add(user)
                    } catch (e: Exception) {
                        Log.e("AdminFragment", "Error procesando usuario: ${e.message}")
                    }
                }

                if (usersList.isEmpty()) {
                    emptyView.visibility = View.VISIBLE
                    emptyView.text = "No hay usuarios para mostrar"
                } else {
                    emptyView.visibility = View.GONE
                }

                userAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) return

                loadingView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
                emptyView.text = "Error al cargar usuarios: ${error.message}"

                Toast.makeText(requireContext(), "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
                Log.e("AdminFragment", "Error de base de datos: ${error.message}")
        
            }
        })
    }

    private fun showAuthRequiredUI() {
        authRequiredCard.visibility = View.VISIBLE
        usersRecyclerView.visibility = View.GONE
        addUserButton.visibility = View.GONE
        loadingView.visibility = View.GONE
        emptyView.visibility = View.GONE
    }

    private fun showAdminUI() {
        authRequiredCard.visibility = View.GONE
        usersRecyclerView.visibility = View.VISIBLE
        addUserButton.visibility = View.VISIBLE
    }

    // Implementaciones de la interfaz OnUserClickListener
    override fun onUserClick(user: User) {
        Toast.makeText(requireContext(), "Usuario: ${user.name}", Toast.LENGTH_SHORT).show()
    }

    // Reemplaza el método onUserEditClick con esta implementación:
    override fun onUserEditClick(user: User) {
        val editDialog = EditUserDialog(user) {
            // Este callback se ejecuta cuando se actualiza un usuario exitosamente
            loadUsers() // Recargar la lista de usuarios
        }
        editDialog.show(childFragmentManager, "EditUserDialog")
    }

    override fun onUserDeleteClick(user: User) {
        if (user.uid == auth.currentUser?.uid) {
            Toast.makeText(requireContext(), "No puedes eliminar tu propia cuenta", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Usuario")
            .setMessage("¿Estás seguro de que deseas eliminar a ${user.name}?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteUser(user)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    // NUEVA IMPLEMENTACIÓN DEL MÉTODO DE LA INTERFAZ
    override fun onUserNotificationClick(user: User) {
        Log.d("AdminFragment", "Enviar notificación al usuario: ${user.name} (UID: ${user.uid})")

        val sendNotificationFragment = SendNotificationFragment()

        // Crear un Bundle para pasar el UID y el nombre del usuario
        val args = Bundle().apply {
            putString("TARGET_USER_ID", user.uid)
            putString("TARGET_USER_NAME", user.name) // Opcional, para mostrar en la UI de SendNotificationFragment
        }
        sendNotificationFragment.arguments = args
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, sendNotificationFragment) // ¡¡RECUERDA CAMBIAR ESTE ID!!
            .addToBackStack(null) // Para que el usuario pueda volver
            .commit()

    }


    private fun deleteUser(user: User) {
        database.getReference("users").child(user.uid).removeValue()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Usuario eliminado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}