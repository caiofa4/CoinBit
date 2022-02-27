package com.signal.research.epoxymodels

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.signal.research.R
import com.signal.research.data.PreferencesManager
import com.signal.research.data.database.entities.CoinTransaction
import com.signal.research.utils.Formaters
import com.signal.research.utils.TRANSACTION_TYPE_SELL
import com.signal.research.utils.resourcemanager.AndroidResourceManager
import com.signal.research.utils.resourcemanager.AndroidResourceManagerImpl
import java.util.*

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)

class CoinTransactionHistoryView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attributeSet, defStyle) {

    private val tvTxnTypeAndQuantity: TextView
    private val tvTxnTimeAndExchange: TextView
    private val tvTxnCost: TextView
    private val clTransaction: View

    val androidResourceManager: AndroidResourceManager by lazy {
        AndroidResourceManagerImpl(context)
    }

    private val formatter by lazy {
        Formaters(androidResourceManager)
    }

    init {
        View.inflate(context, R.layout.transaction_item, this)
        tvTxnTypeAndQuantity = findViewById(R.id.tvTxnTypeAndQuantity)
        tvTxnCost = findViewById(R.id.tvTxnCost)
        tvTxnTimeAndExchange = findViewById(R.id.tvTxnTimeAndExchange)
        clTransaction = findViewById(R.id.clTransaction)
    }

    @ModelProp(options = [ModelProp.Option.IgnoreRequireHashCode])
    fun setCoinTransaction(coinTransaction: CoinTransaction) {
        val currency = Currency.getInstance(PreferencesManager.getDefaultCurrency(context))

        val transactionType = if (coinTransaction.transactionType == TRANSACTION_TYPE_SELL) {
            androidResourceManager.getString(R.string.sell)
        } else {
            androidResourceManager.getString(R.string.buy)
        }

        tvTxnTypeAndQuantity.text = androidResourceManager.getString(
            R.string.transactionTypeWithQuantity,
            transactionType, coinTransaction.quantity.toPlainString()
        )

        tvTxnCost.text = formatter.formatAmount(coinTransaction.cost, currency, false)

        tvTxnTimeAndExchange.text = androidResourceManager.getString(
            R.string.transactionTimeWithExchange,
            formatter.formatTransactionDate(coinTransaction.transactionTime), coinTransaction.exchange
        )
    }

    @CallbackProp
    fun setMoreClickListener(listener: OnClickListener?) {
        clTransaction.setOnClickListener(listener)
    }

}
