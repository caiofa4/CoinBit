package com.signal.research.epoxymodels

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelView
import com.signal.research.R
import com.signal.research.featurecomponents.ModuleItem

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class AddCoinItemView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attributeSet, defStyle) {

    private val addCoinCard: View

    init {
        View.inflate(context, R.layout.dashboard_new_coin_module, this)
        addCoinCard = findViewById(R.id.addCoinCard)
    }

    @CallbackProp
    fun setAddCoinClickListener(listener: OnClickListener?) {
        addCoinCard.setOnClickListener(listener)
    }

    object AddCoinModuleItem : ModuleItem
}
