package th.ac.mfu.su.wbw.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import th.ac.mfu.su.wbw.WbwApplication
import th.ac.mfu.su.wbw.di.AppContainer

/** Pulls the [AppContainer] out of CreationExtras inside a viewModelFactory block. */
val CreationExtras.appContainer: AppContainer
    get() = (this[APPLICATION_KEY] as WbwApplication).container
