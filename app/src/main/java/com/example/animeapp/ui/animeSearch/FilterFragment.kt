package com.example.animeapp.ui.animeSearch

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.PopupWindow
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.animeapp.databinding.FragmentFilterBinding
import com.example.animeapp.databinding.GenresFlowLayoutBinding
import com.example.animeapp.databinding.ProducersFlowLayoutBinding
import com.example.animeapp.models.CompletePagination
import com.example.animeapp.models.GenresResponse
import com.example.animeapp.models.ProducersResponse
import com.example.animeapp.utils.Debounce
import com.example.animeapp.utils.Limit
import com.example.animeapp.utils.Pagination
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.Theme
import com.example.animeapp.utils.ViewUtils.toPx
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FilterFragment : Fragment() {

    private var _binding: FragmentFilterBinding? = null
    private val binding get() = _binding!!

    private lateinit var genresFlowLayoutBinding: GenresFlowLayoutBinding
    private lateinit var genreRecyclerView: RecyclerView
    private lateinit var producersFlowLayoutBinding: ProducersFlowLayoutBinding
    private lateinit var producerRecyclerView: RecyclerView

    private val viewModel: AnimeSearchViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearchView()
        setupGenresPopupWindow()
        setupGenresRecyclerView()
        setupProducersPopupWindow()
        setupProducersRecyclerView()
        setupProducersObservers()
        setupLimitSpinner(Limit.DEFAULT_LIMIT)
        updatePagination(null)
    }

    private fun setupSearchView() {
        val debounce = Debounce(
            lifecycleScope,
            1000L,
            { query ->
                viewModel.applyFilters(viewModel.queryState.value.copy(query = query))
            },
            viewModel
        )

        binding.apply {
            searchView.setOnQueryTextListener(object : OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    newText?.let { debounce.query(it) }
                    return true
                }
            })
        }
    }

    private fun setupGenresPopupWindow() {
        val genresPopupWindow = createPopupWindow()

        binding.apply {
            genresFlowLayoutBinding = GenresFlowLayoutBinding.inflate(layoutInflater, root, false)
            genresFlowLayoutBinding.apply {
                genresPopupWindow.contentView = root
                retryButton.setOnClickListener { viewModel.fetchGenres() }

                viewLifecycleOwner.lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        viewModel.genres.collect { response ->
                            handleGenreResponse(response, genresFlowLayoutBinding)
                        }
                    }
                }

                resetButton.setOnClickListener {
                    if (viewModel.queryState.value.isGenresDefault()) {
                        Toast.makeText(
                            requireContext(),
                            "No genres filter applied yet",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    } else {
                        viewModel.resetGenreSelection()
                        genresPopupWindow.dismiss()
                    }
                }
                applyButton.setOnClickListener {
                    if (viewModel.selectedGenreId.value.isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            "No genres filter applied",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        viewModel.applyGenreFilters()
                        genresPopupWindow.dismiss()
                    }
                }
            }

            genresField.setOnClickListener {
                genresPopupWindow.showAsDropDown(it, -it.width, 1.toPx())
            }
        }
    }

    private fun setupProducersPopupWindow() {
        val producersPopupWindow = createPopupWindow()

        binding.apply {
            producersFlowLayoutBinding =
                ProducersFlowLayoutBinding.inflate(layoutInflater, root, false)
            producersFlowLayoutBinding.apply {
                producersPopupWindow.contentView = root
                retryButton.setOnClickListener { viewModel.fetchProducers() }

                resetButton.setOnClickListener {
                    if (viewModel.queryState.value.isProducersDefault()) {
                        Toast.makeText(
                            requireContext(),
                            "No producers filter applied yet",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        viewModel.resetProducerSelection()
                        producersPopupWindow.dismiss()
                    }
                }

                applyButton.setOnClickListener {
                    if (viewModel.selectedProducerId.value.isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            "No producers filter applied",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        viewModel.applyProducerFilters()
                        producersPopupWindow.dismiss()
                    }
                }
            }

            producersField.setOnClickListener {
                producersPopupWindow.showAsDropDown(it, it.width, 1.toPx())
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun handleGenreResponse(
        response: Resource<GenresResponse>,
        binding: GenresFlowLayoutBinding
    ) {
        binding.apply {
            when (response) {
                is Resource.Success -> {
                    val genres = response.data?.data ?: emptyList()
                    if (genres.isEmpty()) {
                        emptyTextView.visibility = View.VISIBLE
                        retryButton.visibility = View.VISIBLE
                    } else {
                        emptyTextView.visibility = View.GONE
                        retryButton.visibility = View.GONE

                        val adapter = genreRecyclerView.adapter as GenreChipAdapter
                        adapter.items = genres
                        adapter.selectedIds = viewModel.selectedGenreId.value
                        adapter.notifyDataSetChanged()
                    }
                }

                is Resource.Loading -> {
                    emptyTextView.visibility = View.GONE
                    retryButton.visibility = View.GONE
                }

                is Resource.Error -> {
                    emptyTextView.visibility = View.VISIBLE
                    retryButton.visibility = View.VISIBLE
                    Toast.makeText(
                        requireContext(),
                        "An error occurred",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setupGenresRecyclerView() {
        genreRecyclerView = genresFlowLayoutBinding.genreRecyclerView
        genreRecyclerView.apply {
            adapter = GenreChipAdapter { genre -> viewModel.setSelectedGenreId(genre.mal_id) }
            layoutManager = FlexboxLayoutManager(requireContext()).apply {
                flexWrap = FlexWrap.WRAP
                flexDirection = FlexDirection.ROW
                alignItems = AlignItems.STRETCH
            }
        }
    }

    private fun setupProducersRecyclerView() {
        producerRecyclerView = producersFlowLayoutBinding.producerRecyclerView
        producerRecyclerView.apply {
            adapter =
                ProducerChipAdapter { producer -> viewModel.setSelectedProducerId(producer.mal_id) }

            layoutManager = FlexboxLayoutManager(requireContext()).apply {
                flexWrap = FlexWrap.WRAP
                flexDirection = FlexDirection.ROW
                alignItems = AlignItems.STRETCH
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun handleProducersResponse(
        response: Resource<ProducersResponse>,
        binding: ProducersFlowLayoutBinding
    ) {
        binding.apply {
            when (response) {
                is Resource.Success -> {
                    val producers = response.data?.data ?: emptyList()
                    if (producers.isEmpty()) {
                        emptyTextView.visibility = View.VISIBLE
                        retryButton.visibility = View.VISIBLE
                    } else {
                        emptyTextView.visibility = View.GONE
                        retryButton.visibility = View.GONE

                        val adapter = producerRecyclerView.adapter as ProducerChipAdapter
                        adapter.items = producers
                        adapter.selectedIds = viewModel.selectedProducerId.value
                        adapter.notifyDataSetChanged()
                    }

                    response.data?.data?.let { data ->
                        if (data.isEmpty()) {
                            limitAndPaginationFragment.limitSpinner.visibility = View.GONE
                            updatePagination(null)
                        } else {
                            updatePagination(response.data.pagination)

                            if (data.size <= 4) {
                                limitAndPaginationFragment.limitSpinner.visibility = View.GONE
                            } else {
                                limitAndPaginationFragment.limitSpinner.visibility = View.VISIBLE
                                setupLimitSpinner(
                                    viewModel.producersQueryState.value.limit ?: Limit.DEFAULT_LIMIT
                                )
                            }
                        }
                    }
                }

                is Resource.Loading -> {
                    emptyTextView.visibility = View.GONE
                    retryButton.visibility = View.GONE
                    limitAndPaginationFragment.limitSpinner.visibility = View.GONE
                    updatePagination(null)
                }

                is Resource.Error -> {
                    emptyTextView.visibility = View.VISIBLE
                    retryButton.visibility = View.VISIBLE
                    Toast.makeText(
                        requireContext(),
                        "An error occurred",
                        Toast.LENGTH_LONG
                    ).show()
                    limitAndPaginationFragment.limitSpinner.visibility = View.GONE
                    updatePagination(null)
                }
            }
        }
    }

    private fun createPopupWindow(): PopupWindow {
        return PopupWindow(requireContext()).apply {
            isOutsideTouchable = true
            isFocusable = true
            elevation = 10f
            width = ViewGroup.LayoutParams.MATCH_PARENT

            val backgroundDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(if (Theme.isDarkMode()) Color.WHITE else Color.BLACK)
                cornerRadius = 20f
                alpha = (255 * 0.7f).toInt()
            }

            setBackgroundDrawable(backgroundDrawable)
        }
    }

    private fun setupProducersObservers() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.producers.collectLatest { response ->
                    handleProducersResponse(
                        response,
                        producersFlowLayoutBinding
                    )
                    producersFlowLayoutBinding.limitAndPaginationFragment.apply {
                        when (response) {
                            is Resource.Success -> {
                                response.data?.data?.let { data ->
                                    if (data.isEmpty()) {
                                        limitSpinner.visibility = View.GONE
                                        updatePagination(null)
                                    } else {
                                        updatePagination(response.data.pagination)

                                        if (data.size <= 4) {
                                            limitSpinner.visibility = View.GONE
                                        } else {
                                            limitSpinner.visibility =
                                                View.VISIBLE
                                            setupLimitSpinner(
                                                viewModel.producersQueryState.value.limit
                                                    ?: Limit.DEFAULT_LIMIT
                                            )
                                        }
                                    }
                                }
                            }

                            is Resource.Error -> {
                                limitSpinner.visibility = View.GONE
                                updatePagination(null)
                            }

                            is Resource.Loading -> {
                                limitSpinner.visibility = View.GONE
                                updatePagination(null)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupLimitSpinner(limit: Int) {
        val limitSpinner: Spinner =
            producersFlowLayoutBinding.limitAndPaginationFragment.limitSpinner
        val limitAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            Limit.limitOptions
        )
        limitSpinner.adapter = limitAdapter
        limitSpinner.setSelection(Limit.limitOptions.indexOf(limit))

        limitSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedLimit = Limit.getLimitValue(position)
                if (viewModel.producersQueryState.value.limit != selectedLimit) {
                    val updatedQueryState = viewModel.producersQueryState.value.copy(
                        limit = selectedLimit, page = 1
                    )
                    viewModel.applyProducersFilters(updatedQueryState)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                viewModel.applyProducersFilters(
                    viewModel.producersQueryState.value.copy(
                        limit = Limit.DEFAULT_LIMIT,
                        page = 1
                    )
                )
            }
        }
    }

    private fun updatePagination(pagination: CompletePagination?) {
        producersFlowLayoutBinding.limitAndPaginationFragment.apply {
            Pagination.setPaginationButtons(
                paginationButtonContainer,
                pagination
            ) { pageNumber ->
                viewModel.applyProducersFilters(viewModel.producersQueryState.value.copy(page = pageNumber))
            }
            paginationButtonContainer.visibility =
                if (pagination == null) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}