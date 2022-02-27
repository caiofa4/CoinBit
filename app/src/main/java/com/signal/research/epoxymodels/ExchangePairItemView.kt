package com.signal.research.epoxymodels

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.TextProp
import com.signal.research.R

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class ExchangePairItemView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attributeSet, defStyle) {

    private val tvSearchItemName: TextView
    private val containerExchangeSearch: ConstraintLayout

    init {
        View.inflate(context, R.layout.exchange_pair_search_item, this)
        tvSearchItemName = findViewById(R.id.tvSearchItemName)
        containerExchangeSearch = findViewById(R.id.containerExchangeSearch)
    }

    @TextProp
    fun setExchangeName(exchangeName: CharSequence) {
        tvSearchItemName.text = exchangeName
    }

    @CallbackProp
    fun setItemClickListener(listener: OnClickListener?) {
        containerExchangeSearch.setOnClickListener(listener)
    }
}
