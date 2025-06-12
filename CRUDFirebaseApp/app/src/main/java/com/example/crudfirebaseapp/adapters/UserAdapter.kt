package com.example.crudfirebaseapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton // Asegúrate que esté importado
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.crudfirebaseapp.R
import com.example.crudfirebaseapp.models.User
import de.hdodenhof.circleimageview.CircleImageView

class UserAdapter(
    private val users: MutableList<User>, // Cambiado a MutableList si planeas modificarla directamente, sino List está bien
    private val listener: OnUserClickListener
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    interface OnUserClickListener {
        fun onUserClick(user: User)
        fun onUserEditClick(user: User)
        fun onUserDeleteClick(user: User)
        fun onUserNotificationClick(user: User) // NUEVO MÉTODO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false) // Usando tu R.layout.item_user
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int = users.size

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.text_user_name)
        private val emailTextView: TextView = itemView.findViewById(R.id.text_user_email)
        private val adminContainer: LinearLayout = itemView.findViewById(R.id.admin_container)
        private val profileImageView: CircleImageView = itemView.findViewById(R.id.profile_image)
        private val editButton: ImageButton = itemView.findViewById(R.id.button_edit)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.button_delete)
        private val notificationButton: ImageButton = itemView.findViewById(R.id.button_send_notification_item) // NUEVO BOTÓN

        fun bind(user: User) {
            nameTextView.text = user.name
            emailTextView.text = user.email
            adminContainer.visibility = if (user.isAdmin) View.VISIBLE else View.GONE

            if (user.profileImageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(user.profileImageUrl)
                    .placeholder(R.drawable.default_profile) // Asegúrate de tener este drawable
                    .error(R.drawable.default_profile)       // Imagen por defecto si hay error
                    .into(profileImageView)
            } else {
                profileImageView.setImageResource(R.drawable.default_profile) // Asegúrate de tener este drawable
            }

            itemView.setOnClickListener {
                listener.onUserClick(user)
            }
            editButton.setOnClickListener {
                listener.onUserEditClick(user)
            }
            deleteButton.setOnClickListener {
                listener.onUserDeleteClick(user)
            }
            notificationButton.setOnClickListener { // LISTENER PARA EL NUEVO BOTÓN
                listener.onUserNotificationClick(user)
            }
        }
    }
}