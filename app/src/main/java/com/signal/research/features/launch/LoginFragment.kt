package com.signal.research.features.launch

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.airbnb.lottie.LottieAnimationView
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
import kotlinx.android.synthetic.main.fragment_login.*

/**
 * A simple [Fragment] subclass.
 * Use the [LoginFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoginFragment : Fragment() {

    lateinit var mGoogleSignInClient: GoogleSignInClient
    val Req_Code:Int = 123
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
        }
    }

    override fun onStart() {
        super.onStart()
        if (GoogleSignIn.getLastSignedInAccount(context) != null) {
            startActivity(Intent(context, HomeActivity::class.java))
            activity?.finish()
        }
    }

    // signInGoogle() function
    private fun signInGoogle() {
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent,Req_Code)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==Req_Code) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleResult(task)
        }
    }

    // handleResult() function -  this is where we update the UI after Google signin takes place
    private fun handleResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account: GoogleSignInAccount? =completedTask.getResult(ApiException::class.java)
            if (account != null) {
                updateUI(account)
            }
        } catch (e:ApiException){
            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    // UpdateUI() function - this is where we specify what UI updation are needed after google signin has taken place.
    private fun updateUI(account: GoogleSignInAccount) {
        val credential= GoogleAuthProvider.getCredential(account.idToken,null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                context?.let {
                    SavedPreference.setEmail(it, account.email.toString())
                    SavedPreference.setUsername(it, account.displayName.toString())
                }
                val intent = Intent(context, HomeActivity::class.java)
                startActivity(intent)
                activity?.finish()
            }
        }
    }


}