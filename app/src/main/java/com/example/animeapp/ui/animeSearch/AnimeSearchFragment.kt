package com.example.animeapp.ui.animeSearch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.animeapp.R
import com.example.animeapp.data.remote.api.RetrofitInstance
import com.example.animeapp.databinding.BottomSheetAnimeSearchFilterBinding
import com.example.animeapp.databinding.FragmentAnimeSearchBinding
import com.example.animeapp.models.CompletePagination
import com.example.animeapp.models.Genres
import com.example.animeapp.repository.AnimeSearchRepository
import com.example.animeapp.ui.common.AnimeHeaderAdapter
import com.example.animeapp.ui.common.FlowLayout
import com.example.animeapp.utils.Debounce
import com.example.animeapp.utils.FilterUtils
import com.example.animeapp.utils.Limit
import com.example.animeapp.utils.MinMaxInputFilter
import com.example.animeapp.utils.Navigation
import com.example.animeapp.utils.Pagination
import com.example.animeapp.utils.Resource
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AnimeSearchFragment : Fragment(), MenuProvider {

    private var _binding: FragmentAnimeSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var animeHeaderAdapter: AnimeHeaderAdapter

    private val viewModel: AnimeSearchViewModel by viewModels {
        val animeSearchRepository = AnimeSearchRepository(RetrofitInstance.api)
        AnimeSearchViewModelProviderFactory(animeSearchRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimeSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
        setupRecyclerView()
        setupSearchView()
        setupLimitSpinner()
        setupObservers()
        setupClickListeners()
        setupRefreshFloatingActionButton()
        updatePagination(null)
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerView() {
        animeHeaderAdapter = AnimeHeaderAdapter()
        binding.rvAnimeSearch.apply {
            adapter = animeHeaderAdapter
            layoutManager = LinearLayoutManager(activity)
        }
    }

    private fun setupClickListeners() {
        animeHeaderAdapter.setOnItemClickListener { animeId ->
            Navigation.navigateToAnimeDetail(
                this,
                animeId,
                R.id.action_animeSearchFragment_to_animeDetailFragment
            )
        }
    }

    private fun setupSearchView() {
        val debounce = Debounce(
            lifecycleScope,
            1000L,
            { query ->
                viewModel.applyFilters(viewModel.queryState.value.copy(query = query)) },
            viewModel
        )

        binding.searchView.setOnQueryTextListener(object :
            OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    debounce.query(it)
                }
                return true
            }
        })
    }

    private fun setupLimitSpinner() {
        val limitSpinner: Spinner = binding.subMenuContainer.limitSpinner
        val limitAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            Limit.limitOptions
        )
        limitSpinner.adapter = limitAdapter

        if (viewModel.queryState.value.limit == Limit.DEFAULT_LIMIT) {
            val defaultLimitIndex = Limit.limitOptions.indexOf(10)
            limitSpinner.setSelection(defaultLimitIndex)
        }

        limitSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedLimit = Limit.getLimitValue(position)
                if (viewModel.queryState.value.limit != selectedLimit) {
                    val updatedQueryState = viewModel.queryState.value.copy(
                        limit = selectedLimit
                    )
                    viewModel.applyFilters(
                        updatedQueryState
                    )
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                viewModel.applyFilters(viewModel.queryState.value.copy(limit = Limit.DEFAULT_LIMIT))
            }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.search_fragment_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_filter -> {
                showFilterBottomSheet()
                true
            }

            else -> false
        }
    }

    private fun showFilterBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomSheetBinding = BottomSheetAnimeSearchFilterBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(bottomSheetBinding.root)

        populateBottomSheetFilters(bottomSheetBinding)

        bottomSheetBinding.applyButton.setOnClickListener {
            viewModel.applyFilters(
                FilterUtils.collectFilterValues(
                    viewModel.queryState.value,
                    bottomSheetBinding
                )
            )
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setOnShowListener { dialog ->
            val bottomSheet =
                (dialog as BottomSheetDialog).findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            val behavior = BottomSheetBehavior.from(bottomSheet!!)

            bottomSheet.apply {
                val layoutParams = layoutParams as CoordinatorLayout.LayoutParams
                val horizontalMargin =
                    resources.getDimensionPixelSize(R.dimen.bottom_sheet_horizontal_margin)
                layoutParams.leftMargin = horizontalMargin
                layoutParams.rightMargin = horizontalMargin

                background =
                    MaterialShapeDrawable.createWithElevationOverlay(requireContext()).apply {
                        shapeAppearanceModel =
                            shapeAppearanceModel.toBuilder()
                                .setTopLeftCorner(CornerFamily.ROUNDED, 40f)
                                .setTopRightCorner(CornerFamily.ROUNDED, 40f)
                                .build()
                    }
            }

            behavior.apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                maxHeight = resources.displayMetrics.heightPixels / 2
            }
        }

        bottomSheetDialog.show()
    }

    private fun populateBottomSheetFilters(binding: BottomSheetAnimeSearchFilterBinding) {
        val currentFilterState = viewModel.queryState.value
        binding.apply {
            typeSpinner.setText(currentFilterState.type ?: "Any")
            statusSpinner.setText(currentFilterState.status ?: "Any")
            ratingSpinner.setText(
                FilterUtils.getRatingDescription(
                    currentFilterState.rating ?: "Any"
                )
            )

            val minMaxFilter = MinMaxInputFilter(1.0, 10.0)
            scoreEditText.filters = arrayOf(minMaxFilter)
            scoreEditText.setText(currentFilterState.score.toString())
            minScoreEditText.filters = arrayOf(minMaxFilter)
            minScoreEditText.setText(currentFilterState.minScore.toString())
            maxScoreEditText.filters = arrayOf(minMaxFilter)
            maxScoreEditText.setText(currentFilterState.maxScore.toString())

            orderBySpinner.setText(currentFilterState.orderBy ?: "Any")
            sortSpinner.setText(currentFilterState.sort ?: "Any")
            producersEditText.setText(currentFilterState.producers ?: "")

            enableDateRangeCheckBox.setOnCheckedChangeListener { _, isChecked ->
                startDateLabel.visibility = if (isChecked) View.VISIBLE else View.GONE
                startDateTf.visibility = if (isChecked) View.VISIBLE else View.GONE
                endDateLabel.visibility = if (isChecked) View.VISIBLE else View.GONE
                endDateTf.visibility = if (isChecked) View.VISIBLE else View.GONE
            }

            val startDateString = currentFilterState.startDate
            if (startDateString != null) {
                val startDate = startDateString.split("-")
                if (startDate.size == 3) {
                    startDatePicker.updateDate(
                        startDate[0].toInt(),
                        startDate[1].toInt() - 1,
                        startDate[2].toInt()
                    )
                }
            }
            val endDateString = currentFilterState.endDate
            if (endDateString != null) {
                val endDate = endDateString.split("-")
                if (endDate.size == 3) {
                    endDatePicker.updateDate(
                        endDate[0].toInt(),
                        endDate[1].toInt() - 1,
                        endDate[2].toInt()
                    )
                }
            }

            enableDateRangeCheckBox.isChecked =
                currentFilterState.startDate != null && currentFilterState.endDate != null
            sfwCheckBox.isChecked = currentFilterState.sfw ?: false
            unapprovedCheckBox.isChecked = currentFilterState.unapproved ?: false

            val typeAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                FilterUtils.TYPE_OPTIONS
            )
            typeSpinner.setAdapter(typeAdapter)

            val statusAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                FilterUtils.STATUS_OPTIONS
            )
            statusSpinner.setAdapter(statusAdapter)

            val ratingAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                FilterUtils.RATING_OPTIONS.map { ratingCode ->
                    FilterUtils.getRatingDescription(ratingCode)
                }
            )
            ratingSpinner.setAdapter(ratingAdapter)

            val orderByAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                FilterUtils.ORDER_BY_OPTIONS
            )
            orderBySpinner.setAdapter(orderByAdapter)

            val sortAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                FilterUtils.SORT_OPTIONS
            )
            sortSpinner.setAdapter(sortAdapter)

            viewLifecycleOwner.lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.genres.collect { genresResponse ->
                        if (genresResponse is Resource.Success) {
                            val genres = genresResponse.data?.data?: emptyList()
                            populateGenreChipGroup(genreFlowLayout, genres)
                        }
                    }
                }
            }
        }
    }

    private fun populateGenreChipGroup(flowLayout: FlowLayout, genres: List<Genres>) {
        flowLayout.removeAllViews()
        for (genre in genres) {
            val chip = Chip(requireContext())
            chip.text = genre.name

            //... (Set other chip properties like onClickListener, etc.)...
            flowLayout.addView(chip)
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.animeSearchResults.collectLatest { response ->
                    binding.apply {
                        when (response) {
                            is Resource.Success -> {
                                response.data?.let { searchResponse ->
                                    animeHeaderAdapter.setLoading(false)
                                    if (searchResponse.data.isEmpty()) {
                                        tvError.visibility = View.VISIBLE
                                        updatePagination(null)
                                        "No results found".also { tvError.text = it }
                                    } else {
                                        updatePagination(response.data.pagination)

                                        subMenuContainer.limitSpinner.adapter
                                        val limitIndex =
                                            Limit.limitOptions.indexOf(viewModel.queryState.value.limit)
                                        subMenuContainer.limitSpinner.setSelection(if (limitIndex == -1) 0 else limitIndex)

                                        animeHeaderAdapter.differ.submitList(searchResponse.data)
                                    }
                                }
                            }

                            is Resource.Error -> {
                                animeHeaderAdapter.setLoading(false)
                                tvError.visibility = View.VISIBLE
                                updatePagination(null)
                                "An error occurred: ${response.message}".also {
                                    tvError.text = it
                                }
                                Toast.makeText(
                                    requireContext(),
                                    "An error occurred: ${response.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            is Resource.Loading -> {
                                animeHeaderAdapter.setLoading(true)
                                tvError.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupRefreshFloatingActionButton() {
        binding.fabRefresh.setOnClickListener {
            viewModel.applyFilters(viewModel.queryState.value)
        }
    }

    private fun updatePagination(pagination: CompletePagination?) {
        binding.apply {
            Pagination.setPaginationButtons(
                subMenuContainer.paginationButtonContainer,
                pagination
            ) { pageNumber ->
                viewModel.applyFilters(viewModel.queryState.value.copy(page = pageNumber))
            }
            subMenuContainer.paginationButtonContainer.visibility =
                if (pagination == null) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}