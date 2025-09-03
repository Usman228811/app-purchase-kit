package com.example.inapp

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            MainScreen()

        }
    }
}

@Composable
fun MainScreen() {



    val factory = remember { MainViewModelFactory() }
    val viewModel: MainViewModel = viewModel(factory = factory)
    val price by  viewModel.price.collectAsState()
    val isPurchased by  viewModel.isPurchased.collectAsState()
    val monthlyPrice by  viewModel.monthly.collectAsState()
    val yearlyPrice by  viewModel.yearly.collectAsState()
    val buttonText by  viewModel.buttonText.collectAsState()
    val activity = LocalActivity.current as Activity

    LaunchedEffect(Unit) {
        viewModel.loadSubscriptionProducts(activity, listOf("monthly", "yearly"))
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = 50.dp, start = 20.dp, end = 20.dp)) {


        Text(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            text = "One Time Purchase"
        )


        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Is Purchased: $isPurchased"
        )

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Price: $price"
        )

        HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), thickness = 2.dp)

        Text(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            text = "Subscription Purchase"
        )


        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Monthly Price: $monthlyPrice"
        )

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Price: $yearlyPrice"
        )

        Button(modifier = Modifier.fillMaxWidth(), onClick = {

        }) {

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = buttonText
            )
        }

        HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), thickness = 2.dp)
    }

}