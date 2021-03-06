package io.ipoli.android.store.membership.job

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.crashlytics.android.Crashlytics
import com.evernote.android.job.DailyJob
import com.evernote.android.job.JobRequest
import io.ipoli.android.BuildConfig
import io.ipoli.android.Constants
import io.ipoli.android.common.api.Api
import io.ipoli.android.common.billing.BillingError
import io.ipoli.android.common.datetime.isBetween
import io.ipoli.android.common.di.BackgroundModule
import io.ipoli.android.MyPoliApp
import io.ipoli.android.store.membership.error.SubscriptionError
import io.ipoli.android.store.membership.usecase.RemoveMembershipUseCase
import io.ipoli.android.store.powerup.usecase.EnableAllPowerUpsUseCase
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withContext
import org.threeten.bp.LocalDate
import space.traversal.kapsule.Injects
import space.traversal.kapsule.Kapsule
import java.lang.Exception
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 03/23/2018.
 */
class CheckMembershipStatusJob : DailyJob(), Injects<BackgroundModule> {


    override fun onRunDailyJob(params: Params): DailyJobResult {
        val kap = Kapsule<BackgroundModule>()
        val playerRepository by kap.required { playerRepository }
        val removeMembershipUseCase by kap.required { removeMembershipUseCase }
        val enableAllPowerUpsUseCase by kap.required { enableAllPowerUpsUseCase }

        kap.inject(MyPoliApp.backgroundModule(context))

        val p = playerRepository.find()
        requireNotNull(p)

        runBlocking {
            val billingClient = withContext(UI) {
                try {
                    val c = BillingClient.newBuilder(context).setListener { _, _ -> }.build()
                    c.connect()
                    c
                } catch (e: BillingError) {
                    logError(e)
                    null
                }
            } ?: return@runBlocking
            checkMembershipStatus(billingClient, removeMembershipUseCase, enableAllPowerUpsUseCase)
            withContext(UI) { billingClient.endConnection() }
        }

        return DailyJobResult.SUCCESS
    }

    private suspend fun BillingClient.connect() {
        suspendCoroutine<Unit> {
            startConnection(object : BillingClientStateListener {
                override fun onBillingServiceDisconnected() {
                    it.resumeWithException(BillingError("Unable to connect"))
                }

                override fun onBillingSetupFinished(responseCode: Int) {
                    if (responseCode == BillingClient.BillingResponse.OK) {
                        it.resume(Unit)
                    } else {
                        it.resumeWithException(BillingError("Unable to establish connection $responseCode"))
                    }
                }

            })
        }
    }

    private suspend fun checkMembershipStatus(
        billingClient: BillingClient,
        removeMembershipUseCase: RemoveMembershipUseCase,
        enableAllPowerUpsUseCase: EnableAllPowerUpsUseCase
    ) {

        val purchasesResult =
            withContext(UI) { billingClient.queryPurchases(BillingClient.SkuType.SUBS) }

        if (purchasesResult.responseCode != BillingClient.BillingResponse.OK) {
            return
        }
        if (purchasesResult.purchasesList.isEmpty()) {
            removeMembershipUseCase.execute(Unit)
        } else {
            val activePurchase = purchasesResult.purchasesList.first()
            try {
                val status =
                    Api.getMembershipStatus(activePurchase.sku, activePurchase.purchaseToken)
                if (status.isAutoRenewing) {
                    updatePowerUpsExpirationDate(status, enableAllPowerUpsUseCase)
                }

            } catch (e: Api.MembershipStatusException) {
                logError(e)
            }

        }
    }

    private fun logError(e: Exception) {
        if (!BuildConfig.DEBUG) {
            Crashlytics.logException(
                SubscriptionError(
                    "Check membership status job failed", e
                )
            )
        }
    }

    private fun updatePowerUpsExpirationDate(
        status: Api.MembershipStatus,
        enableAllPowerUpsUseCase: EnableAllPowerUpsUseCase
    ) {
        val gracePeriodStart =
            status.expirationDate.minusDays((Constants.POWER_UP_GRACE_PERIOD_DAYS - 1).toLong())
        if (isInGracePeriod(gracePeriodStart, status.expirationDate)) {
            enableAllPowerUpsUseCase.execute(EnableAllPowerUpsUseCase.Params(status.expirationDate))
        } else {
            enableAllPowerUpsUseCase.execute(
                EnableAllPowerUpsUseCase.Params(
                    status.expirationDate.minusDays(
                        Constants.POWER_UP_GRACE_PERIOD_DAYS.toLong()
                    )
                )
            )
        }
    }

    private fun isInGracePeriod(
        gracePeriodStart: LocalDate,
        expirationDate: LocalDate
    ) = LocalDate.now().isBetween(gracePeriodStart, expirationDate)

    companion object {
        const val TAG = "check_membership_status_tag"
    }
}

class AndroidCheckMembershipStatusScheduler : CheckMembershipStatusScheduler {
    override fun schedule() {
        DailyJob.schedule(
            JobRequest.Builder(CheckMembershipStatusJob.TAG)
                .setUpdateCurrent(true)
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                .setRequirementsEnforced(true),
            0,
            TimeUnit.HOURS.toMillis(8)
        )
    }
}

interface CheckMembershipStatusScheduler {
    fun schedule()
}