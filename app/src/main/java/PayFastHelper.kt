package vcmsa.projects.crechemanagementapp

import java.math.BigInteger
import java.security.MessageDigest
import java.util.Locale
import java.net.URLEncoder

object PayFastHelper {

    // Sandbox endpoint for testing
    private const val PAYFAST_URL = "https://crecheconnectpayfastserver20251030090609-g5haf5fha0brhaa9.southafricanorth-01.azurewebsites.net"

    private const val MERCHANT_ID = "10037647"
    private const val MERCHANT_KEY = "2lqxb8b17dees"
    private const val PASSPHRASE = "Computer science"

    /**
     * Build the PayFast POST URL with all parameters + signature.
     */
    fun buildPayFastUrl(
        amount: String,
        itemName: String,
        itemDescription: String,
        buyerEmail: String
    ): String {
        val params = linkedMapOf(
            "merchant_id" to MERCHANT_ID,
            "merchant_key" to MERCHANT_KEY,
            "return_url" to "https://crecheconnectpayfastserver20251030090609-g5haf5fha0brhaa9.southafricanorth-01.azurewebsites.net/payment-success",
            "cancel_url" to "https://crecheconnectpayfastserver20251030090609-g5haf5fha0brhaa9.southafricanorth-01.azurewebsites.net/payment-cancel",
            "notify_url" to "https://crecheconnectpayfastserver20251030090609-g5haf5fha0brhaa9.southafricanorth-01.azurewebsites.net/payment-notify",
            "amount" to amount,
            "item_name" to itemName,
            "item_description" to itemDescription,
            "email_address" to buyerEmail,
        )

        val signature = generateSignature(params, PASSPHRASE)
        val query = params.entries.joinToString("&") {
            "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}"
        } + "&signature=$signature"

        return "$PAYFAST_URL?$query"
    }

    /**
     * Generate the MD5 signature for PayFast.
     */
    private fun generateSignature(data: Map<String, String>, passphrase: String): String {
        val payload = data.entries.joinToString("&") { "${it.key}=${it.value}" } +
                "&passphrase=$passphrase"
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(payload.toByteArray()))
            .toString(16)
            .padStart(32, '0')
            .lowercase(Locale.getDefault())
    }
}
