package io.github.aedev.flow.ui.components.donation

import androidx.annotation.StringRes

internal const val OPEN_COLLECTIVE_URL = ""
internal const val PATREON_URL = ""

internal class CryptoWallet(
    @StringRes val name: Int,
    val ticker: String,
    val address: String,
)

internal val DonationWallets = emptyList<CryptoWallet>()
internal val DonationMethodsAvailable: Boolean
    get() = OPEN_COLLECTIVE_URL.isNotBlank() || PATREON_URL.isNotBlank() || DonationWallets.any { it.address.isNotBlank() }
