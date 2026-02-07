package com.example.blueprinttracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blueprinttracker.data.StockDetail
import com.example.blueprinttracker.databinding.DialogAddRemoveFundsBinding
import com.example.blueprinttracker.databinding.DialogChangeTargetAllocationBinding
import com.example.blueprinttracker.databinding.DialogUpdateStockBinding
import com.example.blueprinttracker.databinding.FragmentBucketDetailsBinding
import com.example.blueprinttracker.ui.adapter.StockAdapter
import com.example.blueprinttracker.ui.viewmodel.BucketDetailsUiState
import com.example.blueprinttracker.ui.viewmodel.BucketDetailsViewModel
import com.example.blueprinttracker.ui.viewmodel.BucketDetailsViewModelFactory
import kotlinx.coroutines.launch

class BucketDetailsFragment : Fragment() {

    private var _binding: FragmentBucketDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: BucketDetailsFragmentArgs by navArgs()

    private val viewModel: BucketDetailsViewModel by viewModels {
        BucketDetailsViewModelFactory(
            (requireActivity().application as BlueprintTrackerApplication).repository,
            args.bucketId
        )
    }

    private lateinit var stockAdapter: StockAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBucketDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeUiState()

        binding.buttonUpdateStock.setOnClickListener {
            showUpdateStockDialog()
        }
    }

    private fun setupRecyclerView() {
        stockAdapter = StockAdapter { stockDetail ->
            showStockOptionsDialog(stockDetail)
        }
        binding.recyclerStocks.apply {
            adapter = stockAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is BucketDetailsUiState.Loading -> {
                            // Handle loading
                        }
                        is BucketDetailsUiState.Success -> {
                            binding.textBucketName.text = state.summary.bucket.name
                            stockAdapter.submitList(state.summary.stocks)
                        }
                        is BucketDetailsUiState.Error -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun showStockOptionsDialog(stockDetail: StockDetail) {
        val options = arrayOf("Add/Remove funds", "Change target allocation")
        AlertDialog.Builder(requireContext())
            .setTitle(stockDetail.stock.symbol)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddRemoveFundsDialog(stockDetail)
                    1 -> showChangeTargetAllocationDialog(stockDetail)
                }
            }
            .show()
    }

    private fun showAddRemoveFundsDialog(stockDetail: StockDetail) {
        val dialogBinding = DialogAddRemoveFundsBinding.inflate(layoutInflater)
        AlertDialog.Builder(requireContext())
            .setTitle("Add/Remove Funds: ${stockDetail.stock.symbol}")
            .setView(dialogBinding.root)
            .setPositiveButton("Update") { _, _ ->
                val amountStr = dialogBinding.editAmount.text.toString()
                if (amountStr.isNotBlank()) {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    viewModel.addRemoveFunds(stockDetail.stock.stockId, amount)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showChangeTargetAllocationDialog(stockDetail: StockDetail) {
        val dialogBinding = DialogChangeTargetAllocationBinding.inflate(layoutInflater)
        dialogBinding.editTargetPercentage.setText(stockDetail.stock.targetPercentage.toString())
        AlertDialog.Builder(requireContext())
            .setTitle("Change Target: ${stockDetail.stock.symbol}")
            .setView(dialogBinding.root)
            .setPositiveButton("Update") { _, _ ->
                val targetStr = dialogBinding.editTargetPercentage.text.toString()
                if (targetStr.isNotBlank()) {
                    val target = targetStr.toDoubleOrNull() ?: 0.0
                    viewModel.updateTargetAllocation(stockDetail.stock.stockId, target)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showUpdateStockDialog() {
        val dialogBinding = DialogUpdateStockBinding.inflate(layoutInflater)
        val actions = arrayOf("Buy", "Sell")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, actions)
        dialogBinding.dropdownAction.setAdapter(adapter)

        AlertDialog.Builder(requireContext())
            .setTitle("Update Stock")
            .setView(dialogBinding.root)
            .setPositiveButton("Update") { _, _ ->
                val ticker = dialogBinding.editStockTicker.text.toString()
                val deltaStr = dialogBinding.editDelta.text.toString()
                val action = dialogBinding.dropdownAction.text.toString()

                if (ticker.isNotBlank() && deltaStr.isNotBlank()) {
                    val delta = deltaStr.toDoubleOrNull() ?: 0.0
                    val isBuy = action == "Buy"
                    viewModel.updateStock(ticker, delta, isBuy)
                } else {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
