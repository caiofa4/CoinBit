package com.signal.research.epoxymodels

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import coil.load
import coil.transform.RoundedCornersTransformation
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.signal.research.R
import com.signal.research.data.PreferencesManager
import com.signal.research.featurecomponents.ModuleItem
import com.signal.research.network.BASE_CRYPTOCOMPARE_IMAGE_URL
import com.signal.research.utils.CoinBitExtendedCurrency
import com.signal.research.utils.Formaters
import com.signal.research.utils.resourcemanager.AndroidResourceManager
import com.signal.research.utils.resourcemanager.AndroidResourceManagerImpl
import timber.log.Timber
import java.math.BigDecimal
import java.util.*

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class TopCardItemView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attributeSet, defStyle) {

    private val tvPair: TextView
    private val tvPrice: TextView
    private val tvPriceChange: TextView
    private val tvMarketCap: TextView
    private val ivCoin: ImageView
    private val topCardContainer: View

    private val androidResourceManager: AndroidResourceManager by lazy {
        AndroidResourceManagerImpl(context)
    }

    private val toCurrency: String by lazy {
        PreferencesManager.getDefaultCurrency(context.applicationContext)
    }

    private val currency by lazy {
        Currency.getInstance(toCurrency)
    }

    private val formatter by lazy {
        Formaters(androidResourceManager)
    }

    private val cropCircleTransformation by lazy {
        RoundedCornersTransformation(15F)
    }

    private var onTopItemClickedListener: OnTopItemClickedListener? = null

    private fun addFinalZero(value: String): String {
        if (value.indexOf(".") != value.length - 3) {
            return "${value}0"
        }
        return value
    }

    private fun removeExtraDigits(value: String): String {
        val index = value.indexOf(".")
        return if (index == value.length - 1) {   //dot is last character
            "${value}00"
        } else if (index == value.length - 2) {   //dot is second last character
            "${value}0"
        } else {   //dot is second last character
            value.substring(0, index)
        }
    }

    init {
        View.inflate(context, R.layout.top_card_module, this)
        tvPair = findViewById(R.id.tvPair)
        tvPrice = findViewById(R.id.tvPrice)
        tvPriceChange = findViewById(R.id.tvPriceChange)
        tvMarketCap = findViewById(R.id.tvMarketCap)
        topCardContainer = findViewById(R.id.topCardContainer)
        ivCoin = findViewById(R.id.ivCoin)
    }

    @ModelProp
    fun setTopCardData(topCardsModuleData: TopCardsModuleData) {
        val imageUrl = BASE_CRYPTOCOMPARE_IMAGE_URL + "${topCardsModuleData.imageUrl}?width=50"
        ivCoin.load(imageUrl) {
            crossfade(true)
            error(R.mipmap.ic_launcher_round)
            transformations(cropCircleTransformation)
        }

        val priceChangeAbsolute = formatter.formatAmount(addFinalZero(topCardsModuleData.priceChangeAbsolute.replace(",", ".")), currency)
        val priceChangePercentage = topCardsModuleData.priceChangePercentage.replace(",", ".")

        tvPair.text = topCardsModuleData.pair
        tvPrice.text = formatter.formatAmount(topCardsModuleData.price.replace(",", "."), currency)

        val priceChange = "$priceChangeAbsolute (${androidResourceManager.getString(
                R.string.coinDayChanges,
                priceChangePercentage.toDouble()
        )})"
        tvPriceChange.text = priceChange

        tvMarketCap.text = androidResourceManager.getString(
            R.string.marketCap,
            CoinBitExtendedCurrency.getAmountTextForDisplay(BigDecimal(topCardsModuleData.marketCap), currency)
        )

        topCardContainer.setOnClickListener {
            onTopItemClickedListener?.onItemClicked(topCardsModuleData.coinSymbol)
        }

        try {
            if (priceChangePercentage.toDouble() < 0) {
                tvPrice.setTextColor(ContextCompat.getColor(context, R.color.colorLoss))
                tvPriceChange.setTextColor(ContextCompat.getColor(context, R.color.colorLoss))
            }
        } catch (ex: NumberFormatException) {
            Timber.e(ex)
        }
    }

    @CallbackProp
    fun setItemClickListener(listener: OnTopItemClickedListener?) {
        onTopItemClickedListener = listener
    }

    interface OnTopItemClickedListener {
        fun onItemClicked(coinSymbol: String)
    }

    data class TopCardsModuleData(
        val pair: String,
        val price: String,
        val priceChangePercentage: String,
        val priceChangeAbsolute: String,
        val marketCap: String,
        val coinSymbol: String,
        val imageUrl: String = ""
    ) : ModuleItem
}
