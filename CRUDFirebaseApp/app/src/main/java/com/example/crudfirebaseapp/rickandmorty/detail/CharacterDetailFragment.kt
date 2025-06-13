package com.example.crudfirebaseapp.rickandmorty.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.crudfirebaseapp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CharacterDetailFragment : Fragment() {

    private val viewModel: CharacterDetailViewModel by viewModels()
    private var characterId: Int = -1

    companion object {
        const val CHARACTER_ID = "character_id"

        fun newInstance(id: Int): CharacterDetailFragment {
            val fragment = CharacterDetailFragment()
            val args = Bundle()
            args.putInt(CHARACTER_ID, id)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            characterId = it.getInt(CHARACTER_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_character_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (characterId != -1) {
            viewModel.fetchCharacterDetails(characterId)
        }

        val favoriteButton: FloatingActionButton = view.findViewById(R.id.detail_favorite_button)
        favoriteButton.setOnClickListener { viewModel.toggleFavorite() }

        viewModel.characterDetails.observe(viewLifecycleOwner) { character ->
            if (character != null) {
                view.findViewById<TextView>(R.id.detail_character_name).text = character.name
                view.findViewById<TextView>(R.id.detail_character_status).text = character.status
                view.findViewById<TextView>(R.id.detail_character_species).text = character.species
                view.findViewById<TextView>(R.id.detail_character_gender).text = character.gender
                view.findViewById<TextView>(R.id.detail_character_origin).text = character.originName
                view.findViewById<TextView>(R.id.detail_character_location).text = character.locationName

                val image: ImageView = view.findViewById(R.id.detail_character_image)
                Glide.with(this).load(character.image).into(image)

                val favIcon = if (character.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                favoriteButton.setImageResource(favIcon)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            view.findViewById<ProgressBar>(R.id.detail_progress_bar).visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }
}