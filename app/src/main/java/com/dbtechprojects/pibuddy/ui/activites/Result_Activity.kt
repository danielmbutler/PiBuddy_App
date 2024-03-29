package com.dbtechprojects.pibuddy.ui.activites

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.dbtechprojects.pibuddy.dialogs.CustomCommand
import com.dbtechprojects.pibuddy.R
import com.dbtechprojects.pibuddy.adapters.CustomButtonAdapter
import com.dbtechprojects.pibuddy.databinding.ActivityResultBinding
import com.dbtechprojects.pibuddy.dialogs.CustomButtonDialog
import com.dbtechprojects.pibuddy.dialogs.CustomButtonListener
import com.dbtechprojects.pibuddy.models.*
import com.dbtechprojects.pibuddy.ui.viewmodels.ResultViewModel
import com.dbtechprojects.pibuddy.utilities.SharedPref
import org.json.JSONObject


class Result_Activity : AppCompatActivity(), CustomButtonListener,
    CustomButtonAdapter.OnCustomButtonClick, CustomButtonAdapter.OnCustomButtonDelete {

    companion object {
        val TAG = "Result_Activity"
    }

    private lateinit var pref: SharedPreferences
    private lateinit var binding: ActivityResultBinding
    private lateinit var IPAddress: String
    private lateinit var username: String
    private lateinit var password: String
    private var port: Int? = null
    private val viewModel: ResultViewModel by viewModels()


    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        pref = SharedPref.getSharedPref(applicationContext)
        port = pref.getInt("port", 22)

        // check for results
        intent.getParcelableExtra<CommandResults>("results")?.let { results ->
            setupObservers()
            populateResultView(results)
            setupClicks()
        }

        addCustomButtons()


    }

    private fun setupObservers() {
        viewModel.restartAttemptMessage.observe(this, Observer { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })
        viewModel.powerOffAttemptMessage.observe(this, Observer { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })
    }

    private fun setupClicks() {
        binding.AddCustomCommandButton.setOnClickListener {

            val dialog = CustomCommand(IPAddress)
            dialog.show(supportFragmentManager, "CustomCommand")

        }

        binding.ResultViewAddCustomBtn.setOnClickListener {
            val dialog = CustomButtonDialog(this)
            dialog.show(supportFragmentManager, "CustomButton")
        }

        binding.ResultViewRestartButton.setOnClickListener {

            // Build Confirmation alert

            val builder = AlertDialog.Builder(this@Result_Activity)
            builder.setMessage("Are you sure you want to restart this device?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, id ->
                    viewModel.restartButtonClick(
                        username = username,
                        password = password,
                        ipaddress = IPAddress,
                        port = port!!
                    )

                }
                .setNegativeButton("No") { dialog, id ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        }


        binding.ResultViewPowerOffButton.setOnClickListener {

            // Build Confirmation alert

            val builder = AlertDialog.Builder(this@Result_Activity)
            builder.setMessage("Are you sure you want to Power OFF this device?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, id ->
                    viewModel.powerOffButtonClicked(username, password, IPAddress, port!!)
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, id ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        }
    }

    private fun populateResultView(results: CommandResults) {
        // setup values to be used for power off and custom command
        IPAddress = results.ipAddress.toString()
        username = results.username.toString()
        password = results.password.toString()
        showProgressBar(true)


        setupActionBar(IPAddress)


        binding.LoggedInResultView.text = results.loggedInUsers
        binding.DiskSpaceResultView.text = (results.diskSpace?.replace(
            "[^0-9a-zA-Z:,]+".toRegex(),
            ""
        ) + "%" + " used") //replace all special charaters due to phantom space
        binding.CPUResultView.text = results.cpuUsage?.replace(
            "[^.,a-zA-Z0-9]+".toRegex(),
            ""
        ) + "%" //replace all special charaters due to phantom space but keep '.'
        binding.MemResultView.text = results.memUsage
        binding.CustomCommandResultView.text = results.customCommand
        binding.DiskSpaceResultView.movementMethod = ScrollingMovementMethod()

        if (!results.customCommand.isNullOrEmpty()) {
            //Log.d(TAG, customCommandOutput)
            binding.CustomCommandResultView.visibility = VISIBLE
        }

        showProgressBar(false)
    }

    private fun setupActionBar(IP: String) {

        setSupportActionBar(binding.toolbar)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_24_white)
            actionBar.title = IP
        }

        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun showProgressBar(isVisible: Boolean) {
        binding.MainActivityTextDotLoader.isVisible = isVisible
    }

    override fun onBackPressed() {

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    // set up right help icon on toolbar

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.pi_buddy_result_toolbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.getItemId()) {
            R.id.toolbar_menu_result -> {
                val intent = Intent(this, Shell_Activity::class.java)
                intent.putExtra("Connection", Connection(IPAddress, username, password, port!!))
                startActivity(intent)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCustomButtonAdded() {
        addCustomButtons()
    }

    private fun addCustomButtons() {
        val buttons = pref.getString("buttons", null)
        if (buttons != null) {

            findViewById<RecyclerView>(R.id.customBtnRv).apply {
                visibility = View.VISIBLE
                adapter = CustomButtonAdapter(this@Result_Activity,this@Result_Activity, buttons.toCustomButtonList()?.list,  )
            }
        }
    }

    private fun runCustomCommand(command : String){
        val builder = AlertDialog.Builder(this@Result_Activity)
        builder.setMessage("Are you sure you want to Run this command")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                viewModel.runCustomCommand(username, password, IPAddress, port!!, command )
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, id ->
                // Dismiss the dialog
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }

    override fun onClick(item: CustomButton) {
        runCustomCommand(item.command)
    }

    override fun onDelete(item: CustomButton) {
        val buttons = pref.getString("buttons", null)?.toCustomButtonList()
        buttons?.list?.remove(item)
        val editor = pref?.edit()

        editor.putString("buttons", buttons?.toJsonString())
        editor.commit()
        addCustomButtons()
    }
}