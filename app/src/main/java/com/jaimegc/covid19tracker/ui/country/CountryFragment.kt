package com.jaimegc.covid19tracker.ui.country

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.ConcatAdapter
import com.jaimegc.covid19tracker.R
import com.jaimegc.covid19tracker.common.extensions.containsAdapter
import com.jaimegc.covid19tracker.common.extensions.enableItem
import com.jaimegc.covid19tracker.common.extensions.hide
import com.jaimegc.covid19tracker.common.extensions.isCurrentItemChecked
import com.jaimegc.covid19tracker.common.extensions.isVisible
import com.jaimegc.covid19tracker.common.extensions.onItemSelected
import com.jaimegc.covid19tracker.common.extensions.removeAllAdapters
import com.jaimegc.covid19tracker.common.extensions.show
import com.jaimegc.covid19tracker.databinding.FragmentCountryBinding
import com.jaimegc.covid19tracker.data.preference.CountryPreferences
import com.jaimegc.covid19tracker.ui.adapter.CountrySpinnerAdapter
import com.jaimegc.covid19tracker.ui.adapter.PlaceAdapter
import com.jaimegc.covid19tracker.ui.adapter.PlaceBarChartAdapter
import com.jaimegc.covid19tracker.ui.adapter.PlaceLineChartAdapter
import com.jaimegc.covid19tracker.ui.adapter.PlacePieChartAdapter
import com.jaimegc.covid19tracker.ui.adapter.PlaceSpinnerAdapter
import com.jaimegc.covid19tracker.ui.adapter.PlaceTotalAdapter
import com.jaimegc.covid19tracker.ui.adapter.PlaceTotalBarChartAdapter
import com.jaimegc.covid19tracker.ui.adapter.PlaceTotalPieChartAdapter
import com.jaimegc.covid19tracker.ui.base.BaseFragment
import com.jaimegc.covid19tracker.ui.model.StatsChartUI
import com.jaimegc.covid19tracker.ui.base.states.PlaceStateScreen
import com.jaimegc.covid19tracker.ui.base.states.ScreenState
import org.koin.core.component.inject

class CountryFragment : BaseFragment<CountryViewModel, PlaceStateScreen>(R.layout.fragment_country) {

    override val viewModel: CountryViewModel by inject()

    private val countryPreferences: CountryPreferences by inject()
    private val placeTotalAdapter = PlaceTotalAdapter()
    private val placeAdapter = PlaceAdapter()
    private val placeTotalBarChartAdapter = PlaceTotalBarChartAdapter()
    private val placeBarChartAdapter = PlaceBarChartAdapter()
    private val placeTotalPieChartAdapter = PlaceTotalPieChartAdapter()
    private val placePieChartAdapter = PlacePieChartAdapter()
    private val placeLineChartAdapter = PlaceLineChartAdapter()
    private val concatAdapter = ConcatAdapter()

    private lateinit var binding: FragmentCountryBinding
    private lateinit var countrySpinnerAdapter: CountrySpinnerAdapter
    private lateinit var placeSpinnerAdapter: PlaceSpinnerAdapter
    private lateinit var statsParent: StatsChartUI

    private var countryJustSelected = false
    private var currentMenuItem = menuItemList

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCountryBinding.bind(view)

        initializeMenu()

        binding.recyclerPlace.adapter = concatAdapter

        viewModel.screenState.observe(
            viewLifecycleOwner,
            { screenState ->
                when (screenState) {
                    ScreenState.Loading ->
                        if (concatAdapter.adapters.isEmpty()) {
                            binding.emptyDatabase.layout.hide()
                            binding.loading.layout.show()
                        }
                    ScreenState.EmptyData ->
                        if (currentMenuItem == menuItemLineChart) {
                           binding.loading.layout.hide()
                           binding.emptyDatabase.layout.show()
                        }
                    is ScreenState.Render<PlaceStateScreen> -> {
                        binding.loading.layout.hide()
                        handleRenderState(screenState.renderState)
                    }
                    is ScreenState.Error<PlaceStateScreen> -> Unit // Not implemented
                }
            }
        )

        viewModel.getCountries()
    }

    override fun handleRenderState(renderState: PlaceStateScreen) {
        when (renderState) {
            is PlaceStateScreen.SuccessSpinnerCountries -> {
                countrySpinnerAdapter = CountrySpinnerAdapter(renderState.data)
                binding.countrySpinner.adapter = countrySpinnerAdapter

                binding.countrySpinner.setSelection(
                    renderState.data.indexOf(
                        renderState.data.first { country ->
                            country.id == countryPreferences.getId() }
                    )
                )

                binding.countrySpinner.onItemSelected { pos ->
                    countrySpinnerAdapter.getCountryId(pos).let { idCountry ->
                        countryPreferences.save(idCountry)
                        countryJustSelected = true
                        countrySpinnerAdapter.saveCurrentPosition(pos)
                        viewModel.getRegionsByCountry(idCountry)
                        selectMenu(idCountry)
                    }
                }
            }
            is PlaceStateScreen.SuccessSpinnerRegions -> {
                if (renderState.data.isNotEmpty()) {
                    binding.regionSpinner.show()
                    binding.icExpandRegion.show()
                    placeSpinnerAdapter =
                        PlaceSpinnerAdapter(requireContext(), renderState.data.toMutableList())
                    binding.regionSpinner.adapter = placeSpinnerAdapter

                    binding.regionSpinner.onItemSelected(ignoreFirst = false) { pos ->
                        if (countryJustSelected.not()) {
                            placeSpinnerAdapter.saveCurrentPosition(pos)
                            selectMenu(
                                countrySpinnerAdapter.getCurrentCountryId(),
                                placeSpinnerAdapter.getId(pos)
                            )
                        }
                        countryJustSelected = false
                    }
                } else {
                    binding.regionSpinner.hide()
                    binding.icExpandRegion.hide()
                }
            }
            is PlaceStateScreen.SuccessPlaceAndStats -> {
                if (menu.isCurrentItemChecked(menuItemList)) {
                    concatAdapter.addAdapter(placeTotalAdapter)
                    placeTotalAdapter.submitList(listOf(renderState.data))
                    binding.recyclerPlace.scrollToPosition(0)
                }
            }
            is PlaceStateScreen.SuccessPlaceStats -> {
                if (menu.isCurrentItemChecked(menuItemList)) {
                    concatAdapter.addAdapter(placeAdapter)
                    placeAdapter.submitList(renderState.data)
                    binding.recyclerPlace.scrollToPosition(0)
                }
            }
            is PlaceStateScreen.SuccessPlaceTotalStatsBarChart -> {
                if (menu.isCurrentItemChecked(menuItemBarChart)) {
                    concatAdapter.addAdapter(0, placeTotalBarChartAdapter)
                    if (concatAdapter.containsAdapter(placeBarChartAdapter)) {
                        binding.recyclerPlace.scrollToPosition(0)
                    }
                    placeTotalBarChartAdapter.submitList(listOf(renderState.data))
                }
            }
            is PlaceStateScreen.SuccessPlaceStatsBarChart -> {
                if (menu.isCurrentItemChecked(menuItemBarChart)) {
                    if (concatAdapter.containsAdapter(placeTotalBarChartAdapter)) {
                        concatAdapter.addAdapter(1, placeBarChartAdapter)
                    } else {
                        concatAdapter.addAdapter(0, placeBarChartAdapter)
                    }
                    placeBarChartAdapter.submitList(renderState.data)
                }
            }
            is PlaceStateScreen.SuccessPlaceTotalStatsPieChart -> {
                if (menu.isCurrentItemChecked(menuItemPieChart)) {
                    statsParent = renderState.data

                    if (statsParent.isNotEmpty()) {
                        concatAdapter.addAdapter(0, placeTotalPieChartAdapter)

                        if (concatAdapter.containsAdapter(placePieChartAdapter)) {
                            binding.recyclerPlace.scrollToPosition(0)
                        }

                        placeTotalPieChartAdapter.submitList(listOf(statsParent))
                    } else {
                        binding.emptyDatabase.layout.show()
                    }
                }
            }
            is PlaceStateScreen.SuccessPlaceAndStatsPieChart -> {
                if (menu.isCurrentItemChecked(menuItemPieChart)) {
                    if (concatAdapter.containsAdapter(placeTotalPieChartAdapter)) {
                        if (placeTotalPieChartAdapter.currentList.isNotEmpty()) {
                            renderState.data.map { placeStats ->
                                placeStats.statsParent = statsParent
                            }
                        }
                        concatAdapter.addAdapter(1, placePieChartAdapter)
                    } else {
                        concatAdapter.addAdapter(0, placePieChartAdapter)
                    }

                    placePieChartAdapter.submitList(renderState.data)
                    binding.recyclerPlace.scrollToPosition(0)
                }
            }
            is PlaceStateScreen.SuccessPlaceStatsLineCharts -> {
                if (menu.isCurrentItemChecked(menuItemLineChart)) {
                    concatAdapter.addAdapter(placeLineChartAdapter)
                    placeLineChartAdapter.submitList(listOf(renderState.data))
                    placeLineChartAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun initializeMenu() {
        configureToolbar(binding.toolbar.toolbar, R.string.title_country, R.menu.menu_country)

        binding.toolbar.toolbar.setOnMenuItemClickListener { item ->
            if (::countrySpinnerAdapter.isInitialized) {
                when (item.itemId) {
                    R.id.list_view -> {
                        if (menu.isCurrentItemChecked(menuItemList).not()) {
                            concatAdapter.removeAllAdapters()
                            menu.enableItem(menuItemList)
                            selectMenu(getSelectedCountry(), getSelectedPlace())
                        }
                        true
                    }
                    R.id.bar_chart_view -> {
                        if (menu.isCurrentItemChecked(menuItemBarChart).not()) {
                            menu.enableItem(menuItemBarChart)
                            selectMenu(getSelectedCountry(), getSelectedPlace())
                        }
                        true
                    }
                    R.id.line_chart_view -> {
                        if (menu.isCurrentItemChecked(menuItemLineChart).not()) {
                            menu.enableItem(menuItemLineChart)
                            selectMenu(getSelectedCountry(), getSelectedPlace())
                        }
                        true
                    }
                    R.id.pie_chart_view -> {
                        if (menu.isCurrentItemChecked(menuItemPieChart).not()) {
                            menu.enableItem(menuItemPieChart)
                            selectMenu(getSelectedCountry(), getSelectedPlace())
                        }
                        true
                    }
                    else -> super.onOptionsItemSelected(item)
                }
            } else {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun getSelectedCountry(): String =
        countrySpinnerAdapter.getCountryId(
            binding.countrySpinner.selectedItemId.toInt()
        )

    private fun getSelectedPlace(): String =
        if (binding.regionSpinner.isVisible() && ::placeSpinnerAdapter.isInitialized) {
            placeSpinnerAdapter.getCurrentPlaceId()
        } else {
            ""
        }

    private fun selectMenu(idCountry: String, idRegion: String = "") {
        concatAdapter.removeAllAdapters()
        binding.emptyDatabase.layout.hide()
        binding.loading.layout.hide()
        currentMenuItem = menu.isCurrentItemChecked()

        when (currentMenuItem) {
            menuItemList ->
                viewModel.getListStats(idCountry, idRegion)
            menuItemBarChart ->
                viewModel.getBarChartStats(idCountry, idRegion)
            menuItemLineChart ->
                viewModel.getLineChartStats(idCountry, idRegion)
            else -> viewModel.getPieChartStats(idCountry, idRegion)
        }
    }
}
