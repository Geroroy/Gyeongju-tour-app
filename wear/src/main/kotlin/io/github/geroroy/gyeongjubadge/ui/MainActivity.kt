package io.github.geroroy.gyeongjubadge.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import io.github.geroroy.gyeongjubadge.BadgeApplication
import io.github.geroroy.gyeongjubadge.ui.checkin.CheckInViewModel
import io.github.geroroy.gyeongjubadge.ui.checkin.PlaceCheckInRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as BadgeApplication).container

        setContent {
            MaterialTheme {
                AppScaffold {
                    val viewModel: CheckInViewModel = viewModel(
                        factory = CheckInViewModel.factory(
                            place = container.place,
                            checkInService = container.checkInService,
                            repository = container.badgeRepository,
                        ),
                    )
                    PlaceCheckInRoute(viewModel = viewModel)
                }
            }
        }
    }
}
