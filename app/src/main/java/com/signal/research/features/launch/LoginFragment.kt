package com.signal.research.features.launch

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.signal.research.R
import com.signal.research.data.SavedPreference
import com.signal.research.features.HomeActivity
import com.signal.research.utils.isOnline
import com.signal.research.utils.isValidEmail
import com.signal.research.utils.setEnabledRecursively
import kotlinx.android.synthetic.main.fragment_login.*
import androidx.annotation.NonNull

import com.google.android.gms.tasks.OnCompleteListener




/**
 * A simple [Fragment] subclass.
 * Use the [LoginFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoginFragment : Fragment() {

    lateinit var mGoogleSignInClient: GoogleSignInClient
    private val googleSignInReqCode:Int = 123
    var firebaseAuth = FirebaseAuth.getInstance()

    companion object {
        fun newInstance(): LoginFragment {
            return LoginFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_cli_id))
                .requestEmail()
                .build()

        // getting the value of gso inside the GoogleSigninClient
        mGoogleSignInClient = activity?.let { GoogleSignIn.getClient(it, gso) }!!
        // initialize the firebaseAuth variable
        firebaseAuth= FirebaseAuth.getInstance()

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.apply {
            signin.setOnClickListener {
                signInGoogle()
            }
            login.setOnClickListener {
                validateLogin()
            }
            text_sign_up.setOnClickListener {
                signUp()
            }
            text_forgot_password.setOnClickListener {
                resetPassword()
            }
        }
    }

    private fun resetPassword() {
        val email = edit_email.text.toString()
        if (!isValidEmail(email)) {
            edit_email.error = getString(R.string.enter_proper_email)
        } else {
            firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context,  getString(R.string.reset_password_success), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context,  getString(R.string.reset_password_fail), Toast.LENGTH_SHORT).show()
                        }
                    }
        }
    }

    override fun onStart() {
        super.onStart()
        if (GoogleSignIn.getLastSignedInAccount(context) != null) {
            //startActivity(Intent(context, HomeActivity::class.java))
            //activity?.finish()
        }
    }

    // signInGoogle() function
    private fun signInGoogle() {
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, googleSignInReqCode)
    }

    // signUp() function
    private fun signUp() {
        fragmentManager?.let {
            val registerFragment = it.findFragmentByTag("RegisterFragment")
                    ?: RegisterFragment()

            // if we switch to home clear everything
            it.popBackStack(HomeActivity.FRAGMENT_OTHER, FragmentManager.POP_BACK_STACK_INCLUSIVE)

            it.beginTransaction()
                    .replace(R.id.loginLayout, registerFragment, "RegisterFragment")
                    .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                    .commit()
        }
    }

    // validateLogin() function
    private fun validateLogin() {
        val email = edit_email.text.toString()
        val password = edit_password.text.toString()

        if (!isValidEmail(email)) {
            edit_email.error = getString(R.string.enter_proper_email)
        } else if (password.isEmpty() || password.length < 6) {
            edit_password.error = getString(R.string.enter_proper_password)
        } else {
            if (context?.let { isOnline(it) } == true) {
                login(email, password)
            } else {
                Toast.makeText(context, getString(R.string.check_your_connection), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // login() function
    private fun login(email: String, password: String) {
        view?.setEnabledRecursively(false)
        sign_in_progress_bar.visibility = View.VISIBLE
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener {  task ->
            view?.setEnabledRecursively(true)
            sign_in_progress_bar.visibility = View.GONE
            if (task.isSuccessful) {
                Toast.makeText(context, getString(R.string.login_successful), Toast.LENGTH_SHORT).show()
                sendToHomeActivity()
            } else {
                task.exception?.localizedMessage?.let { it -> getErrorMessage(it) };
            }
        }
    }

    private fun getErrorMessage(message: String) {
        if (message.contains("password is invalid")) {
            Toast.makeText(context, getString(R.string.invalid_password), Toast.LENGTH_SHORT).show()
        } else if (message.contains("no user record corresponding to this identifier")) {
            Toast.makeText(context, getString(R.string.no_user_found), Toast.LENGTH_SHORT).show()
        } else if (message.contains("blocked all requests from this device")) {
            Toast.makeText(context, getString(R.string.account_temporarily_disabled), Toast.LENGTH_SHORT).show()
        } else if (message.contains("network error")) {
            Toast.makeText(context, getString(R.string.network_error), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, getString(R.string.generic_error), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == googleSignInReqCode) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleResult(task)
        }
    }

    // handleResult() function -  this is where we update the UI after Google signin takes place
    private fun handleResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account: GoogleSignInAccount? = completedTask.getResult(ApiException::class.java)
            account?.let {
                updateUI(it)
            }
        } catch (e:ApiException){
            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    // UpdateUI() function - this is where we specify what UI updation are needed after google signin has taken place.
    private fun updateUI(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken,null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                context?.let {
                    SavedPreference.setEmail(it, account.email.toString())
                    SavedPreference.setUsername(it, account.displayName.toString())
                }
                sendToHomeActivity()
            }
        }
    }

    private fun sendToHomeActivity() {
        val intent = Intent(context, HomeActivity::class.java)
        startActivity(intent)
        activity?.finish()
    }


}