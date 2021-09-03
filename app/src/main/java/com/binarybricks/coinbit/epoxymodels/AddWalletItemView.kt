package com.binarybricks.coinbit.epoxymodels

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelView
import com.binarybricks.coinbit.R
import com.binarybricks.coinbit.featurecomponents.ModuleItem

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class AddWalletItemView @JvmOverloads constructor(
        context: Context,
        attributeSet: AttributeSet? = null,
        defStyle: Int = 0,
) : ConstraintLayout(context, attributeSet, defStyle) {

    private val walletCard: View

    init {
        View.inflate(context, R.layout.dashboard_wallet_module, this)
        walletCard = findViewById(R.id.addWalletCard)
    }

    @CallbackProp
    fun setAddCoinClickListener(listener: OnClickListener?) {
        walletCard.setOnClickListener(listener)
    }

    object AddWalletModuleItem : ModuleItem
}