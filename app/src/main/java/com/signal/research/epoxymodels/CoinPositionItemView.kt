package com.signal.research.epoxymodels

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.signal.research.R
import com.signal.research.data.PreferencesManager
import com.signal.research.data.database.entities.CoinTransaction
import com.signal.research.featurecomponents.ModuleItem
import com.signal.research.network.models.CoinPrice
import com.signal.research.utils.Formaters
import com.signal.research.utils.TRANSACTION_TYPE_BUY
import com.signal.research.utils.resourcemanager.AndroidResourceManager
import com.signal.research.utils.resourcemanager.AndroidResourceManagerImpl
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.*

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class CoinPositionItemView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attributeSet, defStyle) {

    private val tvNoOfCoins: TextView
    private val tvCoinLabel: TextView
    private val tvCoinValue: TextView
    private val tvAvgCostValue: TextView
    private val tvTotalReturnValue: TextView

    private val mc: MathContext by lazy {
        MathContext(2, RoundingMode.HALF_UP)
    }

    private val androidResourceManager: AndroidResourceManager by lazy {
        AndroidResourceManagerImpl(context)
    }

    private val formatter by lazy {
        Formaters(androidResourceManager)
    }
    private val currency: Currency by lazy {
        Currency.getInstance(PreferencesManager.getDefaultCurrency(context))
    }

    init {
        View.inflate(context, R.layout.coin_position_card_module, this)
        tvNoOfCoins = findViewById(R.id.tvNoOfCoins)
        tvCoinLabel = findViewById(R.id.tvCoinLabel)
        tvCoinValue = findViewById(R.id.tvCoinValue)
        tvAvgCostValue = findViewById(R.id.tvAvgCostValue)
        tvTotalReturnValue = findViewById(R.id.tvTotalReturnValue)
    }

    @ModelProp(options = [ModelProp.Option.IgnoreRequireHashCode])
    fun setCoinPrice(coinPositionItem: CoinPositionCardModuleData) {
        val coinPrice = coinPositionItem.coinPrice

        val noOfCoinsAndTotalCost = getNoOfCoinsAndTotalCost(coinPositionItem.coinTransactionList)
        val noOfCoins = noOfCoinsAndTotalCost.first
        val totalCost = noOfCoinsAndTotalCost.second

        tvNoOfCoins.text = noOfCoins.toPlainString()
        tvCoinLabel.text = coinPrice.fromSymbol

        val totalCurrentValue = coinPrice.price?.toBigDecimal()?.multiply(noOfCoins)
        if (totalCurrentValue != null) {
            tvCoinValue.text = formatter.formatAmount(totalCurrentValue.toPlainString(), currency)
        }

        tvAvgCostValue.text = formatter.formatAmount(totalCost.divide(noOfCoins, mc).toPlainString(), currency)

        val totalReturnAmount = totalCurrentValue?.subtract(totalCost)
        val totalReturnPercentage = (totalReturnAmount?.divide(totalCost, mc))?.multiply(BigDecimal(100), mc)

        if (totalReturnAmount != null && totalReturnPercentage != null) {

            tvTotalReturnValue.text = androidResourceManager.getString(
                R.string.amountWithChangePercentage,
                formatter.formatAmount(totalReturnAmount.toPlainString(), currency), totalReturnPercentage
            )

            if (totalReturnPercentage < BigDecimal.ZERO) {
                tvTotalReturnValue.setTextColor(androidResourceManager.getColor(R.color.colorLoss))
            } else {
                tvTotalReturnValue.setTextColor(androidResourceManager.getColor(R.color.colorGain))
            }
        }
    }

    private fun getNoOfCoinsAndTotalCost(coinTransactionList: List<CoinTransaction>): Pair<BigDecimal, BigDecimal> {
        var noOfCoins = BigDecimal.ZERO
        //var totalCost = BigDecimal.ZERO

        var totalCost = 0.0

        coinTransactionList.forEach { coinTransaction ->
            if (coinTransaction.transactionType == TRANSACTION_TYPE_BUY) {
                noOfCoins += coinTransaction.quantity
                //totalCost += totalCost.add(coinTransaction.cost.toBigDecimal())
                totalCost += coinTransaction.cost.toDouble()
            } else {
                noOfCoins -= coinTransaction.quantity
                //totalCost -= totalCost.add(coinTransaction.cost.toBigDecimal())
                totalCost -= coinTransaction.cost.toDouble()
            }
        }

        return Pair<BigDecimal, BigDecimal>(noOfCoins, totalCost.toBigDecimal())
    }

    data class CoinPositionCardModuleData(val coinPrice: CoinPrice, val coinTransactionList: List<CoinTransaction>) : ModuleItem
}
