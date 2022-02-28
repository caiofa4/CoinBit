package com.signal.research.features.transaction

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.signal.research.CoinBitApplication
import com.signal.research.R
import com.signal.research.data.PreferencesManager
import com.signal.research.data.database.entities.Coin
import com.signal.research.data.database.entities.CoinTransaction
import com.signal.research.features.CryptoCompareRepository
import com.signal.research.features.exchangesearch.ExchangeSearchActivity
import com.signal.research.features.pairsearch.PairSearchActivity
import com.signal.research.network.models.ExchangePair
import com.signal.research.utils.Formaters
import com.signal.research.utils.TRANSACTION_TYPE_BUY
import com.signal.research.utils.dismissKeyboard
import com.signal.research.utils.resourcemanager.AndroidResourceManagerImpl
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.signal.research.utils.TRANSACTION_TYPE_SELL
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import kotlinx.android.synthetic.main.activity_coin_transaction.*
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.*

import CoinTransactionContract
import android.util.Log

class CoinTransactionActivity : AppCompatActivity(), CoinTransactionContract.View, DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    private var exchangeName = ""
    private var pairName = ""
    private lateinit var previousQuantity: BigDecimal
    private var previousTransactionType = 0

    private val transactionDate by lazy {
        Calendar.getInstance()
    }

    private val androidResourceManager by lazy {
        AndroidResourceManagerImpl(this)
    }
    private val formatter by lazy {
        Formaters(androidResourceManager)
    }

    private val coinRepo by lazy {
        CryptoCompareRepository(CoinBitApplication.database)
    }

    private val coinTransactionPresenter: CoinTransactionPresenter by lazy {
        CoinTransactionPresenter(coinRepo)
    }

    private val defaultCurrency: String by lazy {
        PreferencesManager.getDefaultCurrency(this)
    }

    private val mc: MathContext by lazy {
        MathContext(6, RoundingMode.HALF_UP)
    }

    private var exchangeCoinMap: HashMap<String, MutableList<ExchangePair>>? = null

    private var isNewTransaction: Boolean = true
    private var coin: Coin? = null
    private var coinTransaction: CoinTransaction? = null
    private var cost = BigDecimal.ZERO
    private var buyPrice = BigDecimal.ZERO
    private var buyPriceInHomeCurrency = BigDecimal.ZERO
    private var prices: MutableMap<String, BigDecimal> = hashMapOf()

    private var transactionType = TRANSACTION_TYPE_BUY

    companion object {
        const val COIN = "COIN"
        const val NEW_TRANSACTION = "NEW_TRANSACTION"
        const val COIN_TRANSACTION = "COIN_TRANSACTION"
        private const val EXCHANGE_REQUEST = 100
        private const val PAIR_REQUEST = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coin_transaction)

        val toolbar = findViewById<View>(R.id.coinTransactionToolbar)
        setSupportActionBar(toolbar as Toolbar?)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        coin = intent.getParcelableExtra(COIN)
        isNewTransaction = intent.getBooleanExtra(NEW_TRANSACTION, true)

        checkNotNull(coin)

        supportActionBar?.title = coin?.fullName

        coinTransactionPresenter.attachView(this)
        lifecycle.addObserver(coinTransactionPresenter)

        coinTransactionPresenter.getAllSupportedExchanges()

        initializeUI()

        FirebaseCrashlytics.getInstance().log("CoinTransactionActivity")
    }

    private fun initializeUI() {

        svContainer.setOnClickListener {
            dismissKeyboard(this)
        }

        swBuySell.setOnCheckedChangeListener{ _, isChecked ->
            if (isChecked) {
                tvBuy.visibility = View.VISIBLE
                tvSell.visibility = View.GONE
                transactionType = TRANSACTION_TYPE_BUY
            } else {
                tvBuy.visibility = View.GONE
                tvSell.visibility = View.VISIBLE
                transactionType = TRANSACTION_TYPE_SELL
            }
        }

//        swBuySell.setOnClickListener {
//            if (it.isEnabled) {
//                tvBuy.visibility = View.VISIBLE
//                tvSell.visibility = View.GONE
//                transactionType = TRANSACTION_TYPE_BUY
//            } else {
//                tvBuy.visibility = View.GONE
//                tvSell.visibility = View.VISIBLE
//                transactionType = TRANSACTION_TYPE_SELL
//            }
//        }

        containerExchange.setOnClickListener {
            val exchangeList = exchangeCoinMap?.get(coin?.symbol?.uppercase())
            exchangeList?.let {
                startActivityForResult(
                    ExchangeSearchActivity.buildLaunchIntent(this, getExchangeNameList(it), getString(R.string.change_exchange)),
                    EXCHANGE_REQUEST
                )
            } ?: kotlin.run {
                Toast.makeText(this, getString(R.string.try_again_exchange), Toast.LENGTH_SHORT).show()
            }
        }

        containerPair.setOnClickListener {
            val symbol = coin?.symbol

            val exchangeList = exchangeCoinMap?.get(symbol?.uppercase())
            if (exchangeList != null && symbol != null && exchangeName.isNotEmpty()) {
                startActivityForResult(
                    PairSearchActivity.buildLaunchIntent(this, getTopPair(exchangeList), symbol),
                    PAIR_REQUEST
                )
            }
        }

        containerDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog.newInstance(
                this, transactionDate.get(Calendar.YEAR), transactionDate.get(Calendar.MONTH),
                transactionDate.get(Calendar.DAY_OF_MONTH)
            )

            datePickerDialog.isThemeDark = true
            datePickerDialog.accentColor = ContextCompat.getColor(this, R.color.colorPrimaryDark)
            datePickerDialog.show(supportFragmentManager, "DatePickerDialog")
        }

        etBuyPrice.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                if (editable.isNullOrEmpty()) {
                    tvBuyPriceLabel.visibility = View.GONE
                } else {
                    tvBuyPriceLabel.visibility = View.VISIBLE
                    calculateCost()
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })

        etAmount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                if (editable.isNullOrEmpty()) {
                    tvBuyAmountLabel.visibility = View.GONE
                } else {
                    tvBuyAmountLabel.visibility = View.VISIBLE
                    calculateCost()
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })

        btnFinishTransaction.setOnClickListener {
            val coinTransaction = validateAndMakeTransaction()
            coinTransaction?.let {
                loading.show()
                coinTransactionPresenter.addTransaction(it)
            }
        }

        btnEraseTransaction.setOnClickListener {
            coinTransaction?.let {
                loading.show()
                coinTransactionPresenter.deleteTransaction(it)
            }
        }

        btnUpdateTransaction.setOnClickListener {
            coinTransaction = validateAndUpdateTransaction()
            coinTransaction?.let {
                loading.show()
                coinTransactionPresenter.updateTransaction(it, previousQuantity, previousTransactionType)
            }
        }

        if (!isNewTransaction) {
            populateTransactionView()
        }
    }

    private fun populateTransactionView() {
        btnFinishTransaction.visibility = View.GONE
        btnUpdateTransaction.visibility = View.VISIBLE
        btnEraseTransaction.visibility = View.VISIBLE

        coinTransaction = intent.getParcelableExtra(COIN_TRANSACTION)
        coinTransaction?.let {
            if (it.transactionType == TRANSACTION_TYPE_SELL) {
                swBuySell.isChecked = false
            }

            tvExchange.text = it.exchange
            tvPair.text = it.pair
            tvDatetime.text = formatter.formatTransactionDate(it.transactionTime)
            etBuyPrice.setText(String.format(it.buyPrice.toString()))
            etAmount.setText(String.format(it.quantity.toString()))

            exchangeName = it.exchange
            pairName = it.pair

            previousQuantity = it.quantity
            previousTransactionType = it.transactionType
        }
    }

    override fun onAllSupportedExchangesLoaded(exchangeCoinMap: HashMap<String, MutableList<ExchangePair>>) {
        this.exchangeCoinMap = exchangeCoinMap
        // check for default exchange chosen
    }

    override fun onCoinPriceLoaded(prices: MutableMap<String, BigDecimal>) {
        etBuyPrice.setText(prices[pairName.uppercase()].toString())
        this.prices = prices
    }

    override fun onTransactionAdded() {
        setResult(Activity.RESULT_OK)
        loading.hide()
        finish()
    }

    override fun onTransactionDeleted() {
        loading.hide()
        finish()
    }

    override fun onTransactionUpdated() {
        loading.hide()
        finish()
    }

    private fun calculateCost() {
        if (etBuyPrice.text.isNotEmpty() && etAmount.text.isNotEmpty()) {
            buyPrice = BigDecimal(etBuyPrice.text.toString())
            buyPriceInHomeCurrency = buyPrice

            // this means the pair is not home currency one
            if (prices.size > 1 && prices.containsKey(defaultCurrency.uppercase())) {
                // get rate
                val rate = BigDecimal(etBuyPrice.text.toString()).divide(prices[pairName.uppercase()], mc)
                buyPriceInHomeCurrency = (prices[defaultCurrency.uppercase()]?.multiply(rate, mc))

                // cal cost
                cost = buyPriceInHomeCurrency.multiply(BigDecimal(etAmount.text.toString()), mc)
                tvTotalAmountInCurrencyLabel.text = getString(R.string.transactionCost, cost, defaultCurrency.uppercase())
            } else {
                cost = buyPrice.multiply(BigDecimal(etAmount.text.toString()), mc)
                tvTotalAmountInCurrencyLabel.text = getString(R.string.transactionCost, cost, pairName)
            }
        }
    }

    private fun validateAndMakeTransaction(): CoinTransaction? {
        calculateCost()

        coin?.let {
            if (pairName.isNotEmpty() && buyPrice > BigDecimal.ZERO && buyPriceInHomeCurrency > BigDecimal.ZERO &&
                etAmount.text.isNotEmpty() && cost > BigDecimal.ZERO
            ) {
                return CoinTransaction(
                    transactionType, it.symbol, pairName, buyPrice, buyPriceInHomeCurrency, BigDecimal(etAmount.text.toString()),
                    transactionDate.timeInMillis.toString(), cost.toPlainString(), exchangeName, BigDecimal.ZERO
                )
            }
        }
        return null
    }

    private fun validateAndUpdateTransaction(): CoinTransaction? {
        calculateCost()

        coinTransaction?.let { transaction ->
            coin?.let {
                if (pairName.isNotEmpty() && buyPrice > BigDecimal.ZERO && buyPriceInHomeCurrency > BigDecimal.ZERO &&
                    etAmount.text.isNotEmpty() && cost > BigDecimal.ZERO
                ) {
                    return CoinTransaction(
                        transactionType, it.symbol, pairName, buyPrice, buyPriceInHomeCurrency, BigDecimal(etAmount.text.toString()),
                        transactionDate.timeInMillis.toString(), cost.toPlainString(), exchangeName, BigDecimal.ZERO, transaction.idKey
                    )
                }
            }
        }

        return null
    }

    private fun getExchangeNameList(exchangePairList: MutableList<ExchangePair>): ArrayList<String> {
        val exchangeList: ArrayList<String> = arrayListOf()

        exchangePairList.forEach {
            exchangeList.add(it.exchangeName)
        }

        exchangeList.sort()
        return exchangeList
    }

    private fun getTopPair(exchangePairList: MutableList<ExchangePair>): ArrayList<String> {
        exchangePairList.filter {
            it.exchangeName.equals(exchangeName, true)
        }.map { exchangePair ->
            return exchangePair.pairList as ArrayList<String>
        }

        return arrayListOf()
    }

    override fun onDateSet(view: DatePickerDialog?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        transactionDate.set(Calendar.YEAR, year)
        transactionDate.set(Calendar.MONTH, monthOfYear)
        transactionDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)

        val timePickerDialog = TimePickerDialog.newInstance(
            this, transactionDate.get(Calendar.HOUR_OF_DAY), transactionDate.get(Calendar.MINUTE),
            transactionDate.get(Calendar.SECOND), false
        )

        timePickerDialog.accentColor = ContextCompat.getColor(this, R.color.colorPrimaryDark)
        timePickerDialog.isThemeDark = true
        timePickerDialog.show(supportFragmentManager, "TimePickerDialog")
    }

    override fun onTimeSet(view: TimePickerDialog?, hourOfDay: Int, minute: Int, second: Int) {
        transactionDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
        transactionDate.set(Calendar.MINUTE, minute)
        transactionDate.set(Calendar.SECOND, second)

        tvDatetimeLabel.visibility = View.VISIBLE
        tvDatetime.text = formatter.formatDatePretty(transactionDate.time)

        getCoinPrice()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            EXCHANGE_REQUEST -> {
                data?.let { receivedData ->
                    exchangeName = ExchangeSearchActivity.getResultFromIntent(receivedData)
                    tvExchange.text = exchangeName

                    pairName = ""
                    tvTotalAmountInCurrencyLabel.text = ""
                }
            }
            PAIR_REQUEST -> {
                data?.let { receivedData ->
                    pairName = PairSearchActivity.getResultFromIntent(receivedData)

                    coin?.let {
                        tvPair.text = getString(
                            R.string.coinPair,
                            it.symbol, pairName.uppercase()
                        )

                        getCoinPrice()
                    }

                    tvBuyPriceLabel.text = getString(R.string.buyPriceHint, pairName)
                    etBuyPrice.hint = getString(R.string.buyPriceHint, pairName)
                    tvTotalAmountInCurrencyLabel.text = ""
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getCoinPrice() {

        var toCurrencies = pairName

        // in case pair is not default currency get default currency as well say for example if pair is ETH/BTC get price in USD as well
        if (!pairName.contains(defaultCurrency, true)) {
            toCurrencies = "$toCurrencies,$defaultCurrency"
        }

        coinTransactionPresenter.getPriceForPair(
            coin?.symbol ?: "",
            toCurrencies, exchangeName, (transactionDate.timeInMillis / 1000).toInt().toString()
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            // Respond to the action bar's Up/Home button
            android.R.id.home -> {
                // tell the calling activity/fragment that we're done deleting this transaction
                onBackPressed()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onNetworkError(errorMessage: String) {
        Snackbar.make(svContainer, errorMessage, Snackbar.LENGTH_LONG).show()
    }
}

