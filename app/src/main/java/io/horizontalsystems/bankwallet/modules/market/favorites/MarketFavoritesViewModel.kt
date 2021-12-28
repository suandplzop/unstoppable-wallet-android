package io.horizontalsystems.bankwallet.modules.market.favorites

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.favorites.MarketFavoritesModule.SelectorDialogState
import io.horizontalsystems.bankwallet.modules.market.favorites.MarketFavoritesModule.ViewItem
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MarketFavoritesViewModel(
    private val service: MarketFavoritesService,
    private val menuService: MarketFavoritesMenuService
) : ViewModel() {

    private val disposables = CompositeDisposable()

    private val marketFieldTypes = MarketField.values().toList()
    private var marketField: MarketField by menuService::marketField
    private var marketItems: List<MarketItem> = listOf()

    private val marketFieldSelect: Select<MarketField>
        get() = Select(marketField, marketFieldTypes)

    private val sortingFieldSelect: Select<SortingField>
        get() = Select(service.sortingField, service.sortingFieldTypes)

    var viewState by mutableStateOf<ViewState?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var isRefreshing by mutableStateOf(false)
        private set
    var marketFavoritesData by mutableStateOf<ViewItem?>(null)
        private set
    var sortingFieldSelectorState by mutableStateOf<SelectorDialogState?>(null)
        private set

    init {
        viewModelScope.launch {
            service.marketItemsFlow
                .collect { state ->
                    loading = state == DataState.Loading

                    when (state) {
                        is DataState.Success -> {
                            viewState = ViewState.Success
                            marketItems = state.data
                            syncViewItem()
                        }
                        is DataState.Error -> {
                            viewState = ViewState.Error(state.error)
                        }
                    }
                }
        }

        service.start()
    }

    private fun refreshWithMinLoadingSpinnerPeriod() {
        service.refresh()
        viewModelScope.launch {
            isRefreshing = true
            delay(1000)
            isRefreshing = false
        }
    }

    private fun syncViewItem() {
        marketFavoritesData = ViewItem(
            sortingFieldSelect,
            marketFieldSelect,
            marketItems.map {
                MarketViewItem.create(it, marketField)
            }
        )
    }

    fun refresh() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onClickSortingField() {
        sortingFieldSelectorState = SelectorDialogState.Opened(sortingFieldSelect)
    }

    fun onSelectSortingField(sortingField: SortingField) {
        service.sortingField = sortingField
        sortingFieldSelectorState = SelectorDialogState.Closed
    }

    fun onSelectMarketField(marketField: MarketField) {
        this.marketField = marketField

        syncViewItem()
    }

    fun onSortingFieldDialogDismiss() {
        sortingFieldSelectorState = SelectorDialogState.Closed
    }

    override fun onCleared() {
        disposables.clear()
        service.stop()
    }
}
