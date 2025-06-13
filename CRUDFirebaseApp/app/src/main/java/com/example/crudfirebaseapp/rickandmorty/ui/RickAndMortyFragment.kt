package com.example.crudfirebaseapp.rickandmorty.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.crudfirebaseapp.R
import com.example.crudfirebaseapp.rickandmorty.detail.CharacterDetailFragment
import com.example.crudfirebaseapp.rickandmorty.ui.model.CharacterUiModel
import com.google.android.material.textfield.TextInputEditText

class RickAndMortyFragment : Fragment() {

    private val viewModel: RickAndMortyViewModel by viewModels()

    // 1. DECLARA las vistas como lateinit. Se inicializarán más tarde.
    private lateinit var characterAdapter: CharacterAdapter
    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var searchEditText: TextInputEditText
    private lateinit var favoritesFilterButton: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla el layout y devuelve la vista.
        return inflater.inflate(R.layout.fragment_rick_and_morty, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2. INICIALIZA todas tus vistas aquí, usando el parámetro 'view'.
        recyclerView = view.findViewById(R.id.characters_recycler_view)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
        searchEditText = view.findViewById(R.id.search_bar_edit_text)
        favoritesFilterButton = view.findViewById(R.id.favorites_filter_button)

        characterAdapter = CharacterAdapter(
            onItemClicked = { character ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, CharacterDetailFragment.newInstance(character.id))
                    .addToBackStack(null) // Para que el botón "atrás" funcione
                    .commit()
            },
            onFavoriteClicked = { character ->
                // La lógica existente para el clic en el botón de favoritos
                viewModel.toggleFavorite(character) // O viewModel.unfavoriteCharacter(character) en el de favoritos
            }
        )
        gridLayoutManager = GridLayoutManager(context, 2)
        recyclerView.layoutManager = gridLayoutManager
        recyclerView.adapter = characterAdapter

        // 4. ESTABLECE los listeners y observers UNA SOLA VEZ.
        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        // Listener para el scroll infinito (paginación)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val visibleItemCount = gridLayoutManager.childCount
                val totalItemCount = gridLayoutManager.itemCount
                val firstVisibleItemPosition = gridLayoutManager.findFirstVisibleItemPosition()

                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                    viewModel.loadMoreCharacters()
                }
            }
        })

        // Listener para el botón de filtro de favoritos
        favoritesFilterButton.setOnClickListener {
            viewModel.onFavoritesFilterToggled()
        }

        // Listener para el gesto de "refrescar"
        swipeRefreshLayout.setOnRefreshListener {
            searchEditText.text?.clear()
            viewModel.onRefresh()
        }

        // Listener para cambios en el texto de búsqueda
        searchEditText.doOnTextChanged { text, _, _, _ ->
            viewModel.setSearchQuery(text.toString())
        }
    }

    private fun setupObservers() {
        // Observa la lista de personajes y la envía al adaptador
        viewModel.characters.observe(viewLifecycleOwner) { characters ->
            characterAdapter.submitList(characters)
        }

        // Observa el estado de carga para mostrar/ocultar el SwipeRefreshLayout
        viewModel.showLoading.observe(viewLifecycleOwner) { isLoading ->
            swipeRefreshLayout.isRefreshing = isLoading
        }

        // Observa el estado del filtro de favoritos para cambiar el ícono y la UI
        viewModel.isFavoritesFilterActive.observe(viewLifecycleOwner) { isActive ->
            val iconRes = if (isActive) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            favoritesFilterButton.setImageResource(iconRes)
            searchEditText.isEnabled = !isActive
            if (isActive) {
                searchEditText.text?.clear()
            }
        }
    }
}