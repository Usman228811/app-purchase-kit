package com.example.inapp

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val mainState by  viewModel.mainState.collectAsState()
    val activity = LocalActivity.current as Activity

    LaunchedEffect(Unit) {
        viewModel.loadSubscriptionProducts(activity, listOf("monthly", "yearly"))
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = 50.dp, start = 20.dp, end = 20.dp)) {


        Text(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            text = "One Time Purchase",
            fontSize = 20.sp
        )


        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Is Purchased: ${mainState.isPurchased}"
        )

        Text(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            text = "Price: ${mainState.price}",
            fontSize = 26.sp
        )

        Button(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), onClick = {

            viewModel.purchaseLifeTime(activity)
        }) {

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Purchase",
                textAlign = TextAlign.Center
            )
        }

        HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), thickness = 2.dp)

        Text(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            text = "Subscription Purchase",
            fontSize = 20.sp
        )


        SubscriptionOption(
            title = "Monthly",
            price = mainState.monthlyPrice,
            isSelected = mainState.selectedButtonPos == 0,
            onClick = {
                viewModel.updateSelectedButtonPos(activity, 0)

            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        SubscriptionOption(
            title = "Yearly",
            price = mainState.yearlyPrice,
            isSelected = mainState.selectedButtonPos == 1,
            onClick = {
                viewModel.updateSelectedButtonPos(activity, 1)

            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            viewModel.purchaseSubscription(activity)
        }) {

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = mainState.buttonText,
                textAlign = TextAlign.Center
            )
        }

        HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), thickness = 2.dp)
    }

}



@Composable
fun SubscriptionOption(
    title: String,
    price: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 20.dp)
            .background(
//                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.White,
                color = if (isSelected) Color.Green else Color.LightGray,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black
            )
            Text(
                text = price,
                fontSize = 16.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }
    }
}