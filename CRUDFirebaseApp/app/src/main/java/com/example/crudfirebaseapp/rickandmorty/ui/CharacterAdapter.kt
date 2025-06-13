package com.example.crudfirebaseapp.rickandmorty.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.crudfirebaseapp.R
import com.example.crudfirebaseapp.rickandmorty.ui.model.CharacterUiModel

class CharacterAdapter(
    private val onItemClicked: (CharacterUiModel) -> Unit,
    private val onFavoriteClicked: (CharacterUiModel) -> Unit
) : ListAdapter<CharacterUiModel, CharacterAdapter.CharacterViewHolder>(CharacterDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_character, parent, false)
        return CharacterViewHolder(view)
    }

    override fun onBindViewHolder(holder: CharacterViewHolder, position: Int) {
        val character = getItem(position)
        // CORRECCIÓN 1: Pasar ambos listeners a la función bind.
        holder.bind(character, onItemClicked, onFavoriteClicked)
    }

    class CharacterViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.character_name)
        private val statusTextView: TextView = itemView.findViewById(R.id.character_status)
        private val imageView: ImageView = itemView.findViewById(R.id.character_image)
        private val favoriteButton: ImageButton = itemView.findViewById(R.id.favorite_button)

        fun bind(
            character: CharacterUiModel,
            onItemClicked: (CharacterUiModel) -> Unit,
            onFavoriteClicked: (CharacterUiModel) -> Unit
        ) {
            // CORRECCIÓN 2: Añadida la lógica para rellenar las vistas.
            nameTextView.text = character.name
            statusTextView.text = "${character.status} - ${character.species}"
            Glide.with(itemView.context).load(character.image).into(imageView)

            // Cambia el ícono de la estrella según si es favorito o no
            val starDrawable = if (character.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            favoriteButton.setImageResource(starDrawable)

            // Asigna los listeners a los botones y a la vista del ítem.
            favoriteButton.setOnClickListener {
                onFavoriteClicked(character)
            }

            itemView.setOnClickListener {
                onItemClicked(character)
            }
        }
    }
}

class CharacterDiffCallback : DiffUtil.ItemCallback<CharacterUiModel>() {
    override fun areItemsTheSame(oldItem: CharacterUiModel, newItem: CharacterUiModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: CharacterUiModel, newItem: CharacterUiModel): Boolean {
        // Compara todo el objeto. Si usas 'data class', esto funciona perfectamente.
        return oldItem == newItem
    }
}