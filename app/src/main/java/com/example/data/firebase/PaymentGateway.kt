package com.example.data.firebase

import com.example.data.model.PaymentMethod

/**
 * Integration-ready Online Payment Gateway abstraction.
 *
 * Configure your payment provider SDK (e.g., Stripe PaymentSheet or Razorpay Checkout)
 * by supplying your publishable API key in the Secrets panel / BuildConfig and wiring
 * a concrete PaymentGateway implementation.
 */
data class OnlinePaymentRequest(
    val orderNumber: String,
    val amount: Double,
    val currency: String = "USD",
    val customerEmail: String
)

sealed interface OnlinePaymentResult {
    data class ReadyForOrder(val paymentStatus: String, val note: String) : OnlinePaymentResult
    data class Failed(val message: String) : OnlinePaymentResult
}

interface PaymentGateway {
    val isLiveGatewayConfigured: Boolean
    val providerName: String
    suspend fun preparePayment(
        method: PaymentMethod,
        request: OnlinePaymentRequest
    ): OnlinePaymentResult
}

class DefaultPaymentGateway : PaymentGateway {
    override val isLiveGatewayConfigured: Boolean = false
    override val providerName: String = "TFC Payment Gateway Adapter"

    override suspend fun preparePayment(
        method: PaymentMethod,
        request: OnlinePaymentRequest
    ): OnlinePaymentResult {
        return when (method) {
            PaymentMethod.CASH_ON_DELIVERY ->
                OnlinePaymentResult.ReadyForOrder("Pending", "Pay cash on delivery")
            PaymentMethod.CASH_AT_PICKUP ->
                OnlinePaymentResult.ReadyForOrder("Pending", "Pay cash at pickup counter")
            PaymentMethod.ONLINE_PAYMENT ->
                OnlinePaymentResult.ReadyForOrder(
                    paymentStatus = "Ready for Integration",
                    note = "Order recorded with Online Payment adapter. Configure Stripe/Razorpay publishable key in Secrets to collect live card charges."
                )
        }
    }
}
