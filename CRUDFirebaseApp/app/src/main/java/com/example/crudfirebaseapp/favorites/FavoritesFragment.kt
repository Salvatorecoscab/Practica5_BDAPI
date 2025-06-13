package com.example.crudfirebaseapp.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.crudfirebaseapp.R
import com.example.crudfirebaseapp.rickandmorty.detail.CharacterDetailFragment
import com.example.crudfirebaseapp.rickandmorty.ui.CharacterAdapter
import com.example.crudfirebaseapp.rickandmorty.ui.model.CharacterUiModel

class FavoritesFragment : Fragment() {

    private val viewModel: FavoritesViewModel by viewModels()
    private lateinit var characterAdapter: CharacterAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.favorites_recycler_view)
        val swipeRefreshLayout: SwipeRefreshLayout = view.findViewById(R.id.swipe_refresh_favorites)
        val emptyTextView: TextView = view.findViewById(R.id.empty_favorites_text)

        // --- CORRECCIÓN AQUÍ ---
        characterAdapter = CharacterAdapter(
            onItemClicked = { character ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, CharacterDetailFragment.newInstance(character.id))
                    .addToBackStack(null) // Para que el botón "atrás" funcione
                    .commit()
            },
            onFavoriteClicked = { character ->
                // Se llama a la función correcta del FavoritesViewModel
                viewModel.unfavoriteCharacter(character)
            }
        )
        // --- FIN DE LA CORRECCIÓN ---

        recyclerView.layoutManager = GridLayoutManager(context, 2)
        recyclerView.adapter = characterAdapter

        swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchFavoriteCharacters()
        }

        viewModel.favorites.observe(viewLifecycleOwner) { favorites ->
            characterAdapter.submitList(favorites)
            emptyTextView.visibility = if (favorites.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            swipeRefreshLayout.isRefreshing = isLoading
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchFavoriteCharacters()
    }
}