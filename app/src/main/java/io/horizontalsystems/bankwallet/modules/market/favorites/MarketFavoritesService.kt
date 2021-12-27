package io.horizontalsystems.bankwallet.modules.market.favorites

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.ICurrencyManager
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MarketFavoritesService(
    private val repository: MarketFavoritesRepository,
    private val menuService: MarketFavoritesMenuService,
    private val currencyManager: ICurrencyManager,
    private val backgroundManager: BackgroundManager
) : BackgroundManager.Listener {
    private var favoritesDisposable: Disposable? = null
    private var repositoryDisposable: Disposable? = null
    private var currencyManagerDisposable: Disposable? = null

    private val marketItemsMutableFlow = MutableStateFlow<DataState<List<MarketItem>>>(DataState.Loading)
    val marketItemsFlow = marketItemsMutableFlow.asStateFlow()

    val sortingFieldTypes = SortingField.values().toList()
    var sortingField: SortingField = menuService.sortingField
        set(value) {
            field = value
            menuService.sortingField = value
            rebuildItems()
        }

    private fun fetch(forceRefresh: Boolean) {
        favoritesDisposable?.dispose()

        repository.get(sortingField, currencyManager.baseCurrency, forceRefresh)
            .doOnSubscribe {
                marketItemsMutableFlow.update {
                    DataState.Loading
                }
            }
            .subscribeIO({ marketItems ->
                marketItemsMutableFlow.update {
                    DataState.Success(marketItems)
                }
            }, { error ->
                marketItemsMutableFlow.update {
                    DataState.Error(error)
                }
            }).let {
                favoritesDisposable = it
            }
    }

    private fun rebuildItems() {
        fetch(false)
    }

    private fun forceRefresh() {
        fetch(true)
    }

    fun refresh() {
        forceRefresh()
    }

    fun start() {
        backgroundManager.registerListener(this)

        currencyManager.baseCurrencyUpdatedSignal
            .subscribeIO { forceRefresh() }
            .let { currencyManagerDisposable = it }

        repository.dataUpdatedObservable
            .subscribeIO { forceRefresh() }
            .let { repositoryDisposable = it }

        forceRefresh()
    }

    fun stop() {
        backgroundManager.unregisterListener(this)
        favoritesDisposable?.dispose()
        currencyManagerDisposable?.dispose()
    }

    override fun willEnterForeground() {
        forceRefresh()
    }
}
