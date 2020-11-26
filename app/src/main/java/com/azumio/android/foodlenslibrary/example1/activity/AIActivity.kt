package com.azumio.android.foodlenslibrary.example1.activity

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.azumio.android.foodlenslibrary.FoodLens
import com.azumio.android.foodlenslibrary.example1.R
import com.azumio.android.foodlenslibrary.model.FoodSearchData
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import okhttp3.FormBody
import okio.IOException
import java.util.*


class AIActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FoodLens.init(this)

        btn_ai_camera.setOnClickListener { view ->
            view.isEnabled = false
            FoodLens.lastAuthorizedInstance?.let {
                it.launchCameraActivityForResult(this)
                view.isEnabled = true
            } ?: kotlin.run {
                getAccessToken()
            }

        }
    }


    /*
    "To protect the the client secret please issues token on your server.
    The user_id that uniquely identified the user needs to be provided for personalization.
    If you can't provide the user_id it can be randomly generated "
     */

    private fun getAccessToken()
    {
        val url = "https://api.foodlens.com/api2/token"
        val clientId = "GET IT AT https://dev.caloriemama.ai/"
        val clientSecret = "GET IT AT https://dev.caloriemama.ai/"
        val userId =  UUID.randomUUID().toString()

        val okHttpClient = OkHttpClient()
        val formBody: RequestBody = FormBody.Builder()
            .add("grant_type", "foodapi")
            .add("client_id",clientId)
            .add("client_secret",clientSecret)
            .add("user_id",userId)
            .build()
        val request: Request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        progress.show()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("AI",e.localizedMessage)
                btn_ai_camera.isEnabled = true
                runOnUiThread {
                    progress.hide()
                }

            }

            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string() ?: "{}"
                val gson = Gson()
                val gsonType =
                    object : TypeToken<TokenResponse>() {}.type
                val tokenResponse = gson.fromJson<TokenResponse>(json,gsonType)
                runOnUiThread {
                    tokenResponse.error?.let {
                        Toast.makeText(this@AIActivity,"token error",Toast.LENGTH_LONG).show()
                    } ?: kotlin.run {
                        FoodLens.authorizedInstance(
                            tokenResponse.accessToken,
                            onAuthorized = { foodLens: FoodLens?, exception: Exception? ->
                                foodLens?.launchCameraActivityForResult(this@AIActivity)
                                btn_ai_camera.isEnabled = true

                            })
                    }
                    progress.hide()
                }

            }
        })
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FoodLens.FOODLENS_CAMERA_ACTIVITY_RESULT_CODE) {
            data?.getStringExtra(FoodLens.FOODLENS_FOOD_CHECKIN)?.let {

                val dialogBuilder = AlertDialog.Builder(this)
                dialogBuilder.setMessage(it)
                    .setCancelable(false)
                    .setPositiveButton("OK", DialogInterface.OnClickListener { dialog, id ->
                        dialog.cancel()
                    })

                val alert = dialogBuilder.create()
                alert.setTitle("")
                alert.show()


            }

        }

    }
    data class TokenResponse(
        @SerializedName("access_token")
        val accessToken: String,
        @SerializedName("error")
     val error: TokenError?
    )
    data class TokenError(
        @SerializedName("code")
        val errorCode: Int,
        @SerializedName("errorDetail")
        val errorDetail: String

    )
}




