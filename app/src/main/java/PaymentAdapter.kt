package vcmsa.projects.crechemanagementapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class PaymentAdapter(private val paymentList: List<Payment>) :
    RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder>() {

    class PaymentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPaymentDescription: TextView = itemView.findViewById(R.id.tvPaymentDescription)
        val tvPaymentAmount: TextView = itemView.findViewById(R.id.tvPaymentAmount)
        val tvPaymentDate: TextView = itemView.findViewById(R.id.tvPaymentDate)
        val tvPaymentStatus: TextView = itemView.findViewById(R.id.tvPaymentStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_history, parent, false)
        return PaymentViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        val payment = paymentList[position]
        holder.tvPaymentDescription.text = payment.description
        holder.tvPaymentAmount.text = NumberFormat.getCurrencyInstance(Locale("en", "ZA")).format(payment.amount)
        holder.tvPaymentDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(payment.date))
        holder.tvPaymentStatus.text = payment.status

        val context = holder.itemView.context
        when (payment.status) {
            "Paid" -> holder.tvPaymentStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            "Pending" -> holder.tvPaymentStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark))
            else -> holder.tvPaymentStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
        }
    }

    override fun getItemCount(): Int = paymentList.size
}