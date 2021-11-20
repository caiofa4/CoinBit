package com.signal.research.features.launch

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.signal.research.R
import kotlinx.android.synthetic.main.fragment_register.*
import android.text.TextUtils
import android.util.Patterns
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.signal.research.features.HomeActivity
import com.signal.research.utils.isValidEmail
import com.signal.research.utils.setEnabledRecursively


class RegisterFragment : Fragment() {

    var firebaseAuth = FirebaseAuth.getInstance()

    companion object {
        fun newInstance(): LoginFragment {
            return LoginFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.apply {
            sign_up.setOnClickListener {
                signUpFirebase()
            }
        }
    }

    private fun signUpFirebase() {
        val email = edit_email.text.toString()
        val password = edit_password.text.toString()
        val confirmPassword = edit_confirm_password.text.toString()

        if (!isValidEmail(email)) {
            edit_email.error = getString(R.string.enter_proper_email)
        } else if (password.isEmpty() || password.length < 6) {
            edit_password.error = getString(R.string.enter_proper_password)
        } else if (password != confirmPassword) {
            edit_confirm_password.error = getString(R.string.password_not_matching)
        } else {
            startRegistration(email, password)
        }
    }

    private fun startRegistration(email: String, password: String) {
        view?.setEnabledRecursively(false)
        sign_up_progress_bar.visibility = View.VISIBLE

        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            view?.setEnabledRecursively(true)
            sign_up_progress_bar.visibility = View.GONE
            if (task.isSuccessful) {
                Toast.makeText(context, getString(R.string.registration_successful), Toast.LENGTH_SHORT).show()
                startActivity(Intent(context, HomeActivity::class.java))
                activity?.finish()
            } else {
                task.exception?.localizedMessage?.let { it -> Toast.makeText(context, it, Toast.LENGTH_SHORT).show() };
            }
        }
    }

    private fun showSnackBar(message: String) {
        view?.let { Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show() }
    }


}