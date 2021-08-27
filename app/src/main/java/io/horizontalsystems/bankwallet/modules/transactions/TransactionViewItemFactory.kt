package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.binancechain.BinanceChainIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.binancechain.BinanceChainOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.ColoredValue
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoAddressMapper
import java.util.*

class TransactionViewItemFactory {

    fun convertToViewItem(transactionItem: TransactionItem): TransactionViewItem {
        val record = transactionItem.record
        val status = record.status(transactionItem.lastBlockInfo?.height)
        val progress = when (status) {
            is TransactionStatus.Pending -> 15
            is TransactionStatus.Processing -> (status.progress * 100).toInt()
            else -> null
        }

        val lastBlockTimestamp = transactionItem.lastBlockInfo?.timestamp

        return when (record) {
            is ApproveTransactionRecord -> createViewItemFromApproveTransactionRecord(record, transactionItem.currencyValue, status, progress, lastBlockTimestamp)
            is BinanceChainIncomingTransactionRecord -> createViewItemFromBinanceChainIncomingTransactionRecord(record, transactionItem.currencyValue, status, progress, lastBlockTimestamp)
            is BinanceChainOutgoingTransactionRecord -> createViewItemFromBinanceChainOutgoingTransactionRecord(record, transactionItem.currencyValue, status, progress, lastBlockTimestamp)
            is BitcoinIncomingTransactionRecord -> createViewItemFromBitcoinIncomingTransactionRecord(record, transactionItem.currencyValue, status, progress, lastBlockTimestamp)
            is BitcoinOutgoingTransactionRecord -> createViewItemFromBitcoinOutgoingTransactionRecord(record, transactionItem.currencyValue, status, progress, lastBlockTimestamp)
            is ContractCallTransactionRecord -> createViewItemFromContractCallTransactionRecord(record, transactionItem.currencyValue, status, progress, lastBlockTimestamp)
            is ContractCreationTransactionRecord -> createViewItemFromContractCreationTransactionRecord(record, transactionItem.currencyValue, status, progress, lastBlockTimestamp)
            is EvmIncomingTransactionRecord -> createViewItemFromEvmIncomingTransactionRecord(record, transactionItem.currencyValue, status, progress, lastBlockTimestamp)
            is EvmOutgoingTransactionRecord -> createViewItemFromEvmOutgoingTransactionRecord(record, transactionItem.currencyValue, status, progress, lastBlockTimestamp)
            is SwapTransactionRecord -> createViewItemFromSwapTransactionRecord(record, transactionItem.currencyValue, status, progress, lastBlockTimestamp)
            else -> throw IllegalArgumentException("Undefined record type ${record.javaClass.name}")
        }
    }

    private fun createViewItemFromSwapTransactionRecord(
        record: SwapTransactionRecord,
        currencyValue: CurrencyValue?,
        status: TransactionStatus,
        progress: Int?,
        lastBlockTimestamp: Long?
    ): TransactionViewItem {
        val primaryValue = ColoredValue(getCoinString(record.valueIn), R.color.jacob)
        val secondaryValue = record.valueOut?.let {
            ColoredValue(
                getCoinString(it),
                if (record.foreignRecipient) R.color.grey else R.color.remus
            )
        }

        return TransactionViewItem(
            record.uid,
            R.drawable.ic_tx_swap_20,
            progress,
            Translator.getString(R.string.Transactions_Swap),
            Translator.getString(
                R.string.Transactions_From,
                getNameOrAddressTruncated(record.exchangeAddress)
            ),
            primaryValue,
            secondaryValue,
            Date(record.timestamp * 1000)
        )
    }

    private fun createViewItemFromEvmOutgoingTransactionRecord(
        record: EvmOutgoingTransactionRecord,
        currencyValue: CurrencyValue?,
        status: TransactionStatus,
        progress: Int?,
        lastBlockTimestamp: Long?
    ): TransactionViewItem {
        val primaryValue = currencyValue?.let {
            ColoredValue(
                getCurrencyString(it),
                R.color.jacob
            )
        }
        val secondaryValue = ColoredValue(getCoinString(record.value), R.color.grey)

        return TransactionViewItem(
            record.uid,
            R.drawable.ic_outgoing_20,
            progress,
            Translator.getString(R.string.Transactions_Send),
            Translator.getString(R.string.Transactions_To, getNameOrAddressTruncated(record.to)),
            primaryValue,
            secondaryValue,
            Date(record.timestamp * 1000),
            record.sentToSelf
        )
    }

    private fun createViewItemFromEvmIncomingTransactionRecord(
        record: EvmIncomingTransactionRecord,
        currencyValue: CurrencyValue?,
        status: TransactionStatus,
        progress: Int?,
        lastBlockTimestamp: Long?
    ): TransactionViewItem {
        val primaryValue = currencyValue?.let {
            ColoredValue(
                getCurrencyString(it),
                R.color.remus
            )
        }
        val secondaryValue = ColoredValue(getCoinString(record.value), R.color.grey)

        return TransactionViewItem(
            record.uid,
            R.drawable.ic_incoming_20,
            progress,
            Translator.getString(R.string.Transactions_Receive),
            Translator.getString(
                R.string.Transactions_From,
                getNameOrAddressTruncated(record.from)
            ),
            primaryValue,
            secondaryValue,
            Date(record.timestamp * 1000)
        )
    }

    private fun createViewItemFromContractCreationTransactionRecord(
        record: ContractCreationTransactionRecord,
        currencyValue: CurrencyValue?,
        status: TransactionStatus,
        progress: Int?,
        lastBlockTimestamp: Long?
    ): TransactionViewItem {
        return TransactionViewItem(
            record.uid,
            R.drawable.ic_tx_unordered,
            progress,
            Translator.getString(R.string.Transactions_ContractCreation),
            "---",
            null,
            null,
            Date(record.timestamp * 1000)
        )
    }

    private fun createViewItemFromContractCallTransactionRecord(
        record: ContractCallTransactionRecord,
        currencyValue: CurrencyValue?,
        status: TransactionStatus,
        progress: Int?,
        lastBlockTimestamp: Long?
    ): TransactionViewItem {
        return TransactionViewItem(
            record.uid,
            R.drawable.ic_tx_unordered,
            progress,
            record.blockchainTitle + " " + Translator.getString(R.string.Transactions_ContractCall),
            Translator.getString(
                R.string.Transactions_From,
                getNameOrAddressTruncated(record.contractAddress)
            ),
            null,
            null,
            Date(record.timestamp * 1000)
        )
    }

    private fun createViewItemFromBitcoinOutgoingTransactionRecord(
        record: BitcoinOutgoingTransactionRecord,
        currencyValue: CurrencyValue?,
        status: TransactionStatus,
        progress: Int?,
        lastBlockTimestamp: Long?
    ): TransactionViewItem {
        val subtitle = record.to?.let {
            Translator.getString(
                R.string.Transactions_To,
                getNameOrAddressTruncated(it)
            )
        } ?: "---"

        val primaryValue = currencyValue?.let {
            ColoredValue(
                getCurrencyString(it),
                R.color.jacob
            )
        }
        val secondaryValue = ColoredValue(getCoinString(record.value), R.color.grey)

        val lockState = record.lockState(lastBlockTimestamp)
        val locked = when {
            lockState == null -> null
            lockState.locked -> true
            else -> false
        }

        return TransactionViewItem(
            record.uid,
            R.drawable.ic_outgoing_20,
            progress,
            Translator.getString(R.string.Transactions_Send),
            subtitle,
            primaryValue,
            secondaryValue,
            Date(record.timestamp * 1000),
            record.sentToSelf,
            record.conflictingHash != null,
            locked
        )
    }

    private fun createViewItemFromBitcoinIncomingTransactionRecord(
        record: BitcoinIncomingTransactionRecord,
        currencyValue: CurrencyValue?,
        status: TransactionStatus,
        progress: Int?,
        lastBlockTimestamp: Long?
    ): TransactionViewItem {
        val subtitle = record.from?.let {
            Translator.getString(
                R.string.Transactions_From,
                getNameOrAddressTruncated(it)
            )
        } ?: "---"

        val primaryValue = currencyValue?.let {
            ColoredValue(
                getCurrencyString(it),
                R.color.remus
            )
        }
        val secondaryValue = ColoredValue(getCoinString(record.value), R.color.grey)

        val lockState = record.lockState(lastBlockTimestamp)
        val locked = when {
            lockState == null -> null
            lockState.locked -> true
            else -> false
        }

        return TransactionViewItem(
            record.uid,
            R.drawable.ic_incoming_20,
            progress,
            Translator.getString(R.string.Transactions_Receive),
            subtitle,
            primaryValue,
            secondaryValue,
            Date(record.timestamp * 1000),
            false,
            record.conflictingHash != null,
            locked
        )
    }

    private fun createViewItemFromBinanceChainOutgoingTransactionRecord(
        record: BinanceChainOutgoingTransactionRecord,
        currencyValue: CurrencyValue?,
        status: TransactionStatus,
        progress: Int?,
        lastBlockTimestamp: Long?
    ): TransactionViewItem {
        val primaryValue = currencyValue?.let {
            ColoredValue(
                getCurrencyString(it),
                R.color.jacob
            )
        }
        val secondaryValue = ColoredValue(getCoinString(record.value), R.color.grey)

        return TransactionViewItem(
            record.uid,
            R.drawable.ic_outgoing_20,
            progress,
            Translator.getString(R.string.Transactions_Send),
            Translator.getString(R.string.Transactions_To, getNameOrAddressTruncated(record.to)),
            primaryValue,
            secondaryValue,
            Date(record.timestamp * 1000),
            record.sentToSelf
        )
    }

    private fun createViewItemFromBinanceChainIncomingTransactionRecord(
        record: BinanceChainIncomingTransactionRecord,
        currencyValue: CurrencyValue?,
        status: TransactionStatus,
        progress: Int?,
        lastBlockTimestamp: Long?
    ): TransactionViewItem {
        val primaryValue = currencyValue?.let {
            ColoredValue(
                getCurrencyString(it),
                R.color.remus
            )
        }
        val secondaryValue = ColoredValue(getCoinString(record.value), R.color.grey)

        return TransactionViewItem(
            record.uid,
            R.drawable.ic_incoming_20,
            progress,
            Translator.getString(R.string.Transactions_Receive),
            Translator.getString(
                R.string.Transactions_From,
                getNameOrAddressTruncated(record.from)
            ),
            primaryValue,
            secondaryValue,
            Date(record.timestamp * 1000)
        )
    }

    private fun createViewItemFromApproveTransactionRecord(
        record: ApproveTransactionRecord,
        currencyValue: CurrencyValue?,
        status: TransactionStatus,
        progress: Int?,
        lastBlockTimestamp: Long?
    ): TransactionViewItem {
        val primaryValueText: String?
        val secondaryValueText: String

        if (record.value.isMaxValue) {
            primaryValueText = "∞"
            secondaryValueText =
                Translator.getString(R.string.Transaction_Unlimited, record.value.coin.code)
        } else {
            primaryValueText = currencyValue?.let { getCurrencyString(it) }
            secondaryValueText = getCoinString(record.value)
        }

        val primaryValue = primaryValueText?.let { ColoredValue(it, R.color.leah) }
        val secondaryValue = ColoredValue(secondaryValueText, R.color.grey)

        return TransactionViewItem(
            record.uid,
            R.drawable.ic_tx_checkmark_20,
            progress,
            Translator.getString(R.string.Transactions_Approve),
            Translator.getString(
                R.string.Transactions_From,
                getNameOrAddressTruncated(record.spender)
            ),
            primaryValue,
            secondaryValue,
            Date(record.timestamp * 1000)
        )
    }

    private fun getCurrencyString(currencyValue: CurrencyValue): String {
        return App.numberFormatter.formatFiat(currencyValue.value.abs(), currencyValue.currency.symbol, 0, 2)
    }

    private fun getCoinString(coinValue: CoinValue): String {
        val significantDecimal = App.numberFormatter.getSignificantDecimalCoin(coinValue.value)
        return App.numberFormatter.formatCoin(
            coinValue.value.abs(),
            coinValue.coin.code,
            0,
            significantDecimal
        )
    }

    private fun getNameOrAddressTruncated(address: String): String {
        return TransactionInfoAddressMapper.title(address) ?: "${address.take(5)}...${address.takeLast(5)}"
    }
}