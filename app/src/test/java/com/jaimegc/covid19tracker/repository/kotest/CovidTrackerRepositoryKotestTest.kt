package com.jaimegc.covid19tracker.repository.kotest

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.jaimegc.covid19tracker.ModelFactoryTest.countryOneStats
import com.jaimegc.covid19tracker.ModelFactoryTest.covidTracker
import com.jaimegc.covid19tracker.ModelFactoryTest.listCountry
import com.jaimegc.covid19tracker.ModelFactoryTest.listCountryAndStats
import com.jaimegc.covid19tracker.ModelFactoryTest.listCountryOnlyStats
import com.jaimegc.covid19tracker.ModelFactoryTest.listRegion
import com.jaimegc.covid19tracker.ModelFactoryTest.listRegionAndStats
import com.jaimegc.covid19tracker.ModelFactoryTest.listRegionOnlyStats
import com.jaimegc.covid19tracker.ModelFactoryTest.listRegionStats
import com.jaimegc.covid19tracker.ModelFactoryTest.listSubRegionAndStats
import com.jaimegc.covid19tracker.ModelFactoryTest.listSubRegionStats
import com.jaimegc.covid19tracker.ModelFactoryTest.listWorldStats
import com.jaimegc.covid19tracker.ModelFactoryTest.regionOneStats
import com.jaimegc.covid19tracker.data.datasource.LocalCovidTrackerDatasource
import com.jaimegc.covid19tracker.data.datasource.RemoteCovidTrackerDatasource
import com.jaimegc.covid19tracker.data.preference.CovidTrackerPreferences
import com.jaimegc.covid19tracker.data.repository.CovidTrackerRepository
import com.jaimegc.covid19tracker.domain.model.DomainError
import com.jaimegc.covid19tracker.domain.states.State
import com.jaimegc.covid19tracker.domain.states.StateError
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateCountryOneStatsEmptyData
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateCountryOneStatsSuccess
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateCovidTrackerEmptyData
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateCovidTrackerLoading
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateCovidTrackerSuccess
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateLineChartMostConfirmedListRegionAndStats
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateLineChartMostConfirmedListRegionAndStatsEmptySuccess
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateLineChartMostConfirmedListRegionAndStatsSuccess
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateLineChartMostConfirmedListSubRegionAndStats
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateLineChartMostConfirmedListSubRegionAndStatsEmptySuccess
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateLineChartMostConfirmedListSubRegionAndStatsSuccess
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateListCountryAndStatsEmptyData
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateListCountryAndStatsSuccess
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateListCountryEmptyData
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateListCountryOnlyStatsEmptyData
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateListCountryOnlyStatsSuccess
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateListCountrySuccess
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateListRegionAndStatsEmptyData
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateListRegionAndStatsSuccess
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateListRegionEmptyData
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateListRegionOnlyStatsSuccess
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateListRegionStatsEmptyData
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateListRegionStatsSuccess
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateListRegionSuccess
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateListSubRegionAndStatsEmptyData
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateListSubRegionAndStatsSuccess
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateListSubRegionStatsEmptyData
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateListSubRegionStatsSuccess
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateListWorldStatsEmptyData
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateListWorldStatsSuccess
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateRegionOneStatsEmptyData
import com.jaimegc.covid19tracker.ScreenStateFactoryTest.stateRegionOneStatsSuccess
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.flow

class CovidTrackerRepositoryKotestTest : FunSpec({
    val DATES = listOf("date1", "date2", "date3")
    val DATE = "date"
    val ID_COUNTRY = "id_country"
    val ID_REGION = "id_region"

    val local: LocalCovidTrackerDatasource = mockk()
    val remote: RemoteCovidTrackerDatasource = mockk()
    val preferences: CovidTrackerPreferences = mockk()

    lateinit var repository: CovidTrackerRepository

    beforeTest {
        MockKAnnotations.init(this)
        repository = CovidTrackerRepository(local, remote, preferences)
    }

    test("get covid tracker by date should return loading and save database if the cache is expired") {
        every { preferences.isCacheExpired() } returns true
        coEvery { remote.getCovidTrackerByDate(any()) } returns Either.right(covidTracker)
        coEvery { local.save(covidTracker) } returns Unit

        val flowRepository = repository.getCovidTrackerByDate(DATE)

        verify { preferences.isCacheExpired() }
        verify { local.getCovidTrackerByDate(any()) wasNot Called }

        flowRepository.collect { data ->
            assertThatIsEqualToState(data, stateCovidTrackerLoading)
        }
        coVerify { remote.getCovidTrackerByDate(any()) }
        coVerify { local.save(covidTracker) }
    }

    test("get covid tracker by date should return loading and local ds is not called if the cache is expired and date doesnt exist") {
        every { preferences.isCacheExpired() } returns true
        coEvery { remote.getCovidTrackerByDate(any()) } returns Either.left(DomainError.ServerDomainError)

        val flowRepository = repository.getCovidTrackerByDate(DATE)

        verify { preferences.isCacheExpired() }
        verify { local.getCovidTrackerByDate(any()) wasNot Called }
        coVerify(exactly = 0) { local.save(any()) }
        flowRepository.collect { data ->
            assertThatIsEqualToState(data, stateCovidTrackerLoading)
        }
    }
    
    test("get covid tracker by date should return loading and local ds is not called if the cache is expired and connection is offline") {
        every { preferences.isCacheExpired() } returns true
        coEvery { remote.getCovidTrackerByDate(any()) } returns Either.left(DomainError.NoInternetError)

        val flowRepository = repository.getCovidTrackerByDate(DATE)

        verify { preferences.isCacheExpired() }
        verify { local.getCovidTrackerByDate(any()) wasNot Called }
        coVerify(exactly = 0) { local.save(any()) }
        flowRepository.collect { data ->
            assertThatIsEqualToState(data, stateCovidTrackerLoading)
        }
    }

    test("get covid tracker by date should return loading and local ds is not called if the cache is expired and mapper fails") {
        every { preferences.isCacheExpired() } returns true
        coEvery { remote.getCovidTrackerByDate(any()) } returns Either.left(DomainError.MapperDomainError())

        val flowRepository = repository.getCovidTrackerByDate(DATE)

        verify { preferences.isCacheExpired() }
        verify { local.getCovidTrackerByDate(any()) wasNot Called }
        coVerify(exactly = 0) { local.save(any()) }
        flowRepository.collect { data ->
            assertThatIsEqualToState(data, stateCovidTrackerLoading)
        }
    }

    test("get covid tracker by date should return loading and local ds is not called if the cache is expired and required field is null") {
        every { preferences.isCacheExpired() } returns true
        coEvery { remote.getCovidTrackerByDate(any()) } returns Either.left(DomainError.GenericDomainError)

        val flowRepository = repository.getCovidTrackerByDate(DATE)

        verify { preferences.isCacheExpired() }
        verify { local.getCovidTrackerByDate(any()) wasNot Called }
        coVerify(exactly = 0) { local.save(any()) }
        flowRepository.collect { data ->
            assertThatIsEqualToState(data, stateCovidTrackerLoading)
        }
    }

    test("get covid tracker by date should return only loading if the cache is not expired") {
        every { preferences.isCacheExpired() } returns false

        val flowRepository = repository.getCovidTrackerByDate(DATE)

        verify { preferences.isCacheExpired() }
        verify { local.getCovidTrackerByDate(any()) wasNot Called }
        coVerify { remote.getCovidTrackerByDate(any()) wasNot Called }
        flowRepository.collect { data ->
            assertThatIsEqualToState(data, stateCovidTrackerLoading)
        }
    }

    test("get world and countries by date should return loading and success") {
        val flow = flow {
            emit(Either.right(covidTracker))
        }

        every { local.getWorldAndCountriesByDate(any()) } returns flow

        val flowRepository = repository.getWorldAndCountriesByDate(DATE)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateCovidTrackerSuccess)
            }
        }
        verify { local.getWorldAndCountriesByDate(any()) }
    }

    test("get world and countries by date with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getWorldAndCountriesByDate(any()) } returns flow

        val flowRepository = repository.getWorldAndCountriesByDate(DATE)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateCovidTrackerEmptyData)
            }
        }
        verify { local.getWorldAndCountriesByDate(any()) }
    }

    test("get world all stats should return loading and success") {
        val flow = flow {
            emit(Either.right(listWorldStats))
        }

        every { local.getWorldAllStats() } returns flow

        val flowRepository = repository.getWorldAllStats()

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateListWorldStatsSuccess)
            }
        }
        verify { local.getWorldAllStats() }
    }

    test("get world all stats with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getWorldAllStats() } returns flow

        val flowRepository = repository.getWorldAllStats()

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateListWorldStatsEmptyData)
            }
        }
        verify { local.getWorldAllStats() }
    }

    test("get country all stats should return loading and success") {
        val flow = flow {
            emit(Either.right(listCountryOnlyStats))
        }

        every { local.getCountryAllStats(any()) } returns flow

        val flowRepository = repository.getCountryAllStats(ID_COUNTRY)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateListCountryOnlyStatsSuccess)
            }
        }
        verify { local.getCountryAllStats(any()) }
    }

    test("get country all stats with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getCountryAllStats(any()) } returns flow

        val flowRepository = repository.getCountryAllStats(ID_COUNTRY)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateListCountryOnlyStatsEmptyData)
            }
        }
        verify { local.getCountryAllStats(any()) }
    }

    test("get region all stats should return loading and success") {
        val flow = flow {
            emit(Either.right(listRegionOnlyStats))
        }

        every { local.getRegionAllStats(any(), any()) } returns flow

        val flowRepository = repository.getRegionAllStats(ID_COUNTRY, ID_REGION)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateListRegionOnlyStatsSuccess)
            }
        }
        verify { local.getRegionAllStats(any(), any()) }
    }

    test("get region all stats with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getRegionAllStats(any(), any()) } returns flow

        val flowRepository = repository.getRegionAllStats(ID_COUNTRY, ID_REGION)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateListRegionAndStatsEmptyData)
            }
        }
        verify { local.getRegionAllStats(any(), any()) }
    }

    test("get countries stats ordered by confirmed should return loading and success") {
        val flow = flow {
            emit(Either.right(listCountryAndStats))
        }

        every { local.getCountriesStatsOrderByConfirmed() } returns flow

        val flowRepository = repository.getCountriesStatsOrderByConfirmed()

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateListCountryAndStatsSuccess)
            }
        }
        verify { local.getCountriesStatsOrderByConfirmed() }
    }

    test("get countries stats ordered by confirmed with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getCountriesStatsOrderByConfirmed() } returns flow

        val flowRepository = repository.getCountriesStatsOrderByConfirmed()

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateListCountryAndStatsEmptyData)
            }
        }
        verify { local.getCountriesStatsOrderByConfirmed() }
    }

    test("get regions stats ordered by confirmed should return loading and success") {
        val flow = flow {
            emit(Either.right(listRegionStats))
        }

        every { local.getRegionsStatsOrderByConfirmed(any(), any()) } returns flow

        val flowRepository = repository.getRegionsStatsOrderByConfirmed(ID_REGION, DATE)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateListRegionStatsSuccess)
            }
        }
        verify { local.getRegionsStatsOrderByConfirmed(any(), any()) }
    }

    test("get regions stats ordered by confirmed with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getRegionsStatsOrderByConfirmed(any(), any()) } returns flow

        val flowRepository = repository.getRegionsStatsOrderByConfirmed(ID_REGION, DATE)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateListRegionStatsEmptyData)
            }
        }
        verify { local.getRegionsStatsOrderByConfirmed(any(), any()) }
    }

    test("get subregions stats ordered by confirmed should return loading and success") {
        val flow = flow {
            emit(Either.right(listSubRegionStats))
        }

        every { local.getSubRegionsStatsOrderByConfirmed(any(), any(), any()) } returns flow

        val flowRepository =
            repository.getSubRegionsStatsOrderByConfirmed(ID_COUNTRY, ID_REGION, DATE)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateListSubRegionStatsSuccess)
            }
        }
        verify { local.getSubRegionsStatsOrderByConfirmed(any(), any(), any()) }
    }

    test("get subregions stats ordered by confirmed with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getSubRegionsStatsOrderByConfirmed(any(), any(), any()) } returns flow

        val flowRepository =
            repository.getSubRegionsStatsOrderByConfirmed(ID_COUNTRY, ID_REGION, DATE)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateListSubRegionStatsEmptyData)
            }
        }
        verify { local.getSubRegionsStatsOrderByConfirmed(any(), any(), any()) }
    }

    test("get regions all stats ordered by confirmed should return loading and success") {
        val flow = flow {
            emit(Either.right(listRegionAndStats))
        }

        every { local.getRegionsAllStatsOrderByConfirmed(any()) } returns flow

        val flowRepository = repository.getRegionsAllStatsOrderByConfirmed(ID_COUNTRY)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateListRegionAndStatsSuccess)
            }
        }
        verify { local.getRegionsAllStatsOrderByConfirmed(any()) }
    }

    test("get regions all stats ordered by confirmed with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getRegionsAllStatsOrderByConfirmed(any()) } returns flow

        val flowRepository = repository.getRegionsAllStatsOrderByConfirmed(ID_COUNTRY)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateListRegionAndStatsEmptyData)
            }
        }
        verify { local.getRegionsAllStatsOrderByConfirmed(any()) }
    }

    test("get subregions all stats ordered by confirmed should return loading and success") {
        val flow = flow {
            emit(Either.right(listSubRegionAndStats))
        }

        every { local.getSubRegionsAllStatsOrderByConfirmed(any(), any()) } returns flow

        val flowRepository = repository.getSubRegionsAllStatsOrderByConfirmed(ID_COUNTRY, ID_REGION)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateListSubRegionAndStatsSuccess)
            }
        }
        verify { local.getSubRegionsAllStatsOrderByConfirmed(any(), any()) }
    }

    test("get subregions all stats ordered by confirmed with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getSubRegionsAllStatsOrderByConfirmed(any(), any()) } returns flow

        val flowRepository = repository.getSubRegionsAllStatsOrderByConfirmed(ID_COUNTRY, ID_REGION)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateListSubRegionAndStatsEmptyData)
            }
        }
        verify { local.getSubRegionsAllStatsOrderByConfirmed(any(), any()) }
    }

    test("get countries and stats with most confirmed should return loading and success") {
        val flow = flow {
            emit(Either.right(listCountryAndStats))
        }

        every { local.getCountriesAndStatsWithMostConfirmed() } returns flow

        val flowRepository = repository.getCountriesAndStatsWithMostConfirmed()

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateListCountryAndStatsSuccess)
            }
        }
        verify { local.getCountriesAndStatsWithMostConfirmed() }
    }

    test("get countries and stats with most confirmed with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getCountriesAndStatsWithMostConfirmed() } returns flow

        val flowRepository = repository.getCountriesAndStatsWithMostConfirmed()

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateListCountryAndStatsEmptyData)
            }
        }
        verify { local.getCountriesAndStatsWithMostConfirmed() }
    }

    test("get regions and stats with most confirmed should return loading and success") {
        val flow = flow {
            emit(Either.right(stateLineChartMostConfirmedListRegionAndStats))
        }

        every { local.getRegionsAndStatsWithMostConfirmed(any()) } returns flow

        val flowRepository = repository.getRegionsAndStatsWithMostConfirmed(ID_COUNTRY)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateLineChartMostConfirmedListRegionAndStatsSuccess)
            }
        }
        verify { local.getRegionsAndStatsWithMostConfirmed(any()) }
    }

    test("get regions and stats with most confirmed with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getRegionsAndStatsWithMostConfirmed(any()) } returns flow

        val flowRepository = repository.getRegionsAndStatsWithMostConfirmed(ID_COUNTRY)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateLineChartMostConfirmedListRegionAndStatsEmptySuccess)
            }
        }
        verify { local.getRegionsAndStatsWithMostConfirmed(any()) }
    }

    test("get subregions and stats with most confirmed should return loading and success") {
        val flow = flow {
            emit(Either.right(stateLineChartMostConfirmedListSubRegionAndStats))
        }

        every { local.getSubRegionsAndStatsWithMostConfirmed(any(), any()) } returns flow

        val flowRepository = repository.getSubRegionsAndStatsWithMostConfirmed(ID_COUNTRY, ID_REGION)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateLineChartMostConfirmedListSubRegionAndStatsSuccess)
            }
        }
        verify { local.getSubRegionsAndStatsWithMostConfirmed(any(), any()) }
    }

    test("get subregions and stats with most confirmed with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getSubRegionsAndStatsWithMostConfirmed(any(), any()) } returns flow

        val flowRepository = repository.getSubRegionsAndStatsWithMostConfirmed(ID_COUNTRY, ID_REGION)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateLineChartMostConfirmedListSubRegionAndStatsEmptySuccess)
            }
        }
        verify { local.getSubRegionsAndStatsWithMostConfirmed(any(), any()) }
    }

    test("get countries and stats with most deaths should return loading and success") {
        val flow = flow {
            emit(Either.right(listCountryAndStats))
        }

        every { local.getCountriesAndStatsWithMostDeaths() } returns flow

        val flowRepository = repository.getCountriesAndStatsWithMostDeaths()

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateListCountryAndStatsSuccess)
            }
        }
        verify { local.getCountriesAndStatsWithMostDeaths() }
    }

    test("get countries and stats with most deaths with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getCountriesAndStatsWithMostDeaths() } returns flow

        val flowRepository = repository.getCountriesAndStatsWithMostDeaths()

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateListCountryAndStatsEmptyData)
            }
        }
        verify { local.getCountriesAndStatsWithMostDeaths() }
    }

    test("get regions and stats with most deaths should return loading and success") {
        val flow = flow {
            emit(Either.right(stateLineChartMostConfirmedListRegionAndStats))
        }

        every { local.getRegionsAndStatsWithMostDeaths(any()) } returns flow

        val flowRepository = repository.getRegionsAndStatsWithMostDeaths(ID_COUNTRY)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateLineChartMostConfirmedListRegionAndStatsSuccess)
            }
        }
        verify { local.getRegionsAndStatsWithMostDeaths(any()) }
    }

    test("get regions and stats with most deaths with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getRegionsAndStatsWithMostDeaths(any()) } returns flow

        val flowRepository = repository.getRegionsAndStatsWithMostDeaths(ID_COUNTRY)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateLineChartMostConfirmedListRegionAndStatsEmptySuccess)
            }
        }
        verify { local.getRegionsAndStatsWithMostDeaths(any()) }
    }

    test("get subregions and stats with most deaths should return loading and success") {
        val flow = flow {
            emit(Either.right(stateLineChartMostConfirmedListSubRegionAndStats))
        }

        every { local.getSubRegionsAndStatsWithMostDeaths(any(), any()) } returns flow

        val flowRepository = repository.getSubRegionsAndStatsWithMostDeaths(ID_COUNTRY, ID_REGION)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateLineChartMostConfirmedListSubRegionAndStatsSuccess)
            }
        }
        verify { local.getSubRegionsAndStatsWithMostDeaths(any(), any()) }
    }

    test("get subregions and stats with most deaths with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getSubRegionsAndStatsWithMostDeaths(any(), any()) } returns flow

        val flowRepository = repository.getSubRegionsAndStatsWithMostDeaths(ID_COUNTRY, ID_REGION)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateLineChartMostConfirmedListSubRegionAndStatsEmptySuccess)
            }
        }
        verify { local.getSubRegionsAndStatsWithMostDeaths(any(), any()) }
    }

    test("get countries and stats with most open cases should return loading and success") {
        val flow = flow {
            emit(Either.right(listCountryAndStats))
        }

        every { local.getCountriesAndStatsWithMostOpenCases() } returns flow

        val flowRepository = repository.getCountriesAndStatsWithMostOpenCases()

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateListCountryAndStatsSuccess)
            }
        }
        verify { local.getCountriesAndStatsWithMostOpenCases() }
    }

    test("get countries and stats with most open cases with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getCountriesAndStatsWithMostOpenCases() } returns flow

        val flowRepository = repository.getCountriesAndStatsWithMostOpenCases()

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateListCountryAndStatsEmptyData)
            }
        }
        verify { local.getCountriesAndStatsWithMostOpenCases() }
    }

    test("get regions and stats with most open cases should return loading and success") {
        val flow = flow {
            emit(Either.right(stateLineChartMostConfirmedListRegionAndStats))
        }

        every { local.getRegionsAndStatsWithMostOpenCases(any()) } returns flow

        val flowRepository = repository.getRegionsAndStatsWithMostOpenCases(ID_COUNTRY)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateLineChartMostConfirmedListRegionAndStatsSuccess)
            }
        }
        verify { local.getRegionsAndStatsWithMostOpenCases(any()) }
    }

    test("get regions and stats with most open cases with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getRegionsAndStatsWithMostOpenCases(any()) } returns flow

        val flowRepository = repository.getRegionsAndStatsWithMostOpenCases(ID_COUNTRY)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateLineChartMostConfirmedListRegionAndStatsEmptySuccess)
            }
        }
        verify { local.getRegionsAndStatsWithMostOpenCases(any()) }
    }

    test("get subregions and stats with most open cases should return loading and success") {
        val flow = flow {
            emit(Either.right(stateLineChartMostConfirmedListSubRegionAndStats))
        }

        every { local.getSubRegionsAndStatsWithMostOpenCases(any(), any()) } returns flow

        val flowRepository = repository.getSubRegionsAndStatsWithMostOpenCases(ID_COUNTRY, ID_REGION)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateLineChartMostConfirmedListSubRegionAndStatsSuccess)
            }
        }
        verify { local.getSubRegionsAndStatsWithMostOpenCases(any(), any()) }
    }

    test("get subregions and stats with most open cases with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getSubRegionsAndStatsWithMostOpenCases(any(), any()) } returns flow

        val flowRepository = repository.getSubRegionsAndStatsWithMostOpenCases(ID_COUNTRY, ID_REGION)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateLineChartMostConfirmedListSubRegionAndStatsEmptySuccess)
            }
        }
        verify { local.getSubRegionsAndStatsWithMostOpenCases(any(), any()) }
    }

    test("get countries and stats with most recovered should return loading and success") {
        val flow = flow {
            emit(Either.right(listCountryAndStats))
        }

        every { local.getCountriesAndStatsWithMostRecovered() } returns flow

        val flowRepository = repository.getCountriesAndStatsWithMostRecovered()

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateListCountryAndStatsSuccess)
            }
        }
        verify { local.getCountriesAndStatsWithMostRecovered() }
    }

    test("get countries and stats with most recovered with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getCountriesAndStatsWithMostRecovered() } returns flow

        val flowRepository = repository.getCountriesAndStatsWithMostRecovered()

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateListCountryAndStatsEmptyData)
            }
        }
        verify { local.getCountriesAndStatsWithMostRecovered() }
    }

    test("get regions and stats with most recovered should return loading and success") {
        val flow = flow {
            emit(Either.right(stateLineChartMostConfirmedListRegionAndStats))
        }

        every { local.getRegionsAndStatsWithMostRecovered(any()) } returns flow

        val flowRepository = repository.getRegionsAndStatsWithMostRecovered(ID_COUNTRY)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateLineChartMostConfirmedListRegionAndStatsSuccess)
            }
        }
        verify { local.getRegionsAndStatsWithMostRecovered(any()) }
    }

    test("get regions and stats with most recovered with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getRegionsAndStatsWithMostRecovered(any()) } returns flow

        val flowRepository = repository.getRegionsAndStatsWithMostRecovered(ID_COUNTRY)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateLineChartMostConfirmedListRegionAndStatsEmptySuccess)
            }
        }
        verify { local.getRegionsAndStatsWithMostRecovered(any()) }
    }

    test("get subregions and stats with most recovered should return loading and success") {
        val flow = flow {
            emit(Either.right(stateLineChartMostConfirmedListSubRegionAndStats))
        }

        every { local.getSubRegionsAndStatsWithMostRecovered(any(), any()) } returns flow

        val flowRepository = repository.getSubRegionsAndStatsWithMostRecovered(ID_COUNTRY, ID_REGION)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateLineChartMostConfirmedListSubRegionAndStatsSuccess)
            }
        }
        verify { local.getSubRegionsAndStatsWithMostRecovered(any(), any()) }
    }

    test("get subregions and stats with most recovered with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getSubRegionsAndStatsWithMostRecovered(any(), any()) } returns flow

        val flowRepository = repository.getSubRegionsAndStatsWithMostRecovered(ID_COUNTRY, ID_REGION)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateLineChartMostConfirmedListSubRegionAndStatsEmptySuccess)
            }
        }
        verify { local.getSubRegionsAndStatsWithMostRecovered(any(), any()) }
    }

    test("get countries should return loading and success") {
        val flow = flow {
            emit(Either.right(listCountry))
        }

        every { local.getCountries() } returns flow

        val flowRepository = repository.getCountries()

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateListCountrySuccess)
            }
        }
        verify { local.getCountries() }
    }

    test("get countries with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getCountries() } returns flow

        val flowRepository = repository.getCountries()

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateListCountryEmptyData)
            }
        }
        verify { local.getCountries() }
    }

    test("get regions by country should return loading and success") {
        val flow = flow {
            emit(Either.right(listRegion))
        }

        every { local.getRegionsByCountry(any()) } returns flow

        val flowRepository = repository.getRegionsByCountry(ID_COUNTRY)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateListRegionSuccess)
            }
        }
        verify { local.getRegionsByCountry(any()) }
    }

    test("get regions by country with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getRegionsByCountry(any()) } returns flow

        val flowRepository = repository.getRegionsByCountry(ID_COUNTRY)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateListRegionEmptyData)
            }
        }
        verify { local.getRegionsByCountry(any()) }
    }

    test("get country and stats by date should return loading and success") {
        val flow = flow {
            emit(Either.right(countryOneStats))
        }

        every { local.getCountryAndStatsByDate(any(), any()) } returns flow

        val flowRepository = repository.getCountryAndStatsByDate(ID_COUNTRY, DATE)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateCountryOneStatsSuccess)
            }
        }
        verify { local.getCountryAndStatsByDate(any(), any()) }
    }

    test("get country and stats by date with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getCountryAndStatsByDate(any(), any()) } returns flow

        val flowRepository = repository.getCountryAndStatsByDate(ID_COUNTRY, DATE)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateCountryOneStatsEmptyData)
            }
        }
        verify { local.getCountryAndStatsByDate(any(), any()) }
    }

    test("get region and stats by date should return loading and success") {
        val flow = flow {
            emit(Either.right(regionOneStats))
        }

        every { local.getRegionAndStatsByDate(any(), any(), any()) } returns flow

        val flowRepository = repository.getRegionAndStatsByDate(ID_COUNTRY, ID_REGION, DATE)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> data.shouldBeRight(stateRegionOneStatsSuccess)
            }
        }
        verify { local.getRegionAndStatsByDate(any(), any(), any()) }
    }

    test("get region and stats by date with empty database should return loading and empty success") {
        val flow = flow {
            emit(Either.left(DomainError.DatabaseEmptyData))
        }

        every { local.getRegionAndStatsByDate(any(), any(), any()) } returns flow

        val flowRepository = repository.getRegionAndStatsByDate(ID_COUNTRY, ID_REGION, DATE)

        flowRepository.collectIndexed { index, data ->
            when (index) {
                0 -> assertThatIsEqualToState(data, stateCovidTrackerLoading)
                1 -> assertThatIsEqualToState(data, stateRegionOneStatsEmptyData)
            }
        }
        verify { local.getRegionAndStatsByDate(any(), any(), any()) }
    }

    test("get all dates should return a list with all dates") {
        coEvery { local.getAllDates() } returns Either.right(DATES)

        val eitherUseCase = repository.getAllDates()

        coVerify { local.getAllDates() }
        eitherUseCase.map { dates ->
            dates.shouldHaveSize(3)
            dates shouldBe DATES
        }
    }

    test("get all dates with empty data should return an empty list") {
        coEvery { local.getAllDates() } returns Either.right(listOf())

        val eitherUseCase = repository.getAllDates()

        coVerify { local.getAllDates() }
        eitherUseCase.map { dates ->
            dates.shouldBeEmpty()
        }
    }

    test("add covid trackers should return unit") {
        coEvery { local.populateDatabase(any()) } returns Unit

        repository.addCovidTrackers(listOf(covidTracker))

        coVerify { local.populateDatabase(any()) }
    }
})

private fun <R, S> assertThatIsEqualToState(
    data: Either<StateError<DomainError>, R>,
    state: State<S>
) {
    data.shouldBeRight()
    data.map { assertThat(it).isInstanceOf(state::class.java) }
}