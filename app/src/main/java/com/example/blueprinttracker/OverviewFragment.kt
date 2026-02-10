package com.example.blueprinttracker

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blueprinttracker.data.Bucket
import com.example.blueprinttracker.data.BucketAllocation
import com.example.blueprinttracker.databinding.DialogUpdateAllBucketsBinding
import com.example.blueprinttracker.databinding.FragmentOverviewBinding
import com.example.blueprinttracker.databinding.ItemDialogBucketRowBinding
import com.example.blueprinttracker.ui.adapter.BucketAdapter
import com.example.blueprinttracker.ui.viewmodel.PortfolioUiState
import com.example.blueprinttracker.ui.viewmodel.PortfolioViewModel
import com.example.blueprinttracker.ui.viewmodel.PortfolioViewModelFactory
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class OverviewFragment : Fragment() {

    private var _binding: FragmentOverviewBinding? = null
    private val binding get() = _binding!!

    private val portfolioViewModel: PortfolioViewModel by viewModels {
        PortfolioViewModelFactory((requireActivity().application as BlueprintTrackerApplication).repository)
    }

    private lateinit var bucketAdapter: BucketAdapter
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observePortfolio()

        binding.fabAddStock.setOnClickListener {
            Toast.makeText(requireContext(), "Add Stock coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.buttonUpdateBuckets.setOnClickListener {
            val state = portfolioViewModel.uiState.value
            if (state is PortfolioUiState.Success) {
                showUpdateAllBucketsDialog(state.summary.buckets)
            }
        }
    }

    private fun setupRecyclerView() {
        bucketAdapter = BucketAdapter { allocation ->
            val action = OverviewFragmentDirections.actionOverviewFragmentToBucketDetailsFragment(allocation.bucket.bucketId)
            findNavController().navigate(action)
        }
        binding.recyclerBuckets.apply {
            adapter = bucketAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observePortfolio() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                portfolioViewModel.uiState.collect { state ->
                    when (state) {
                        is PortfolioUiState.Loading -> {
                            // Show loading state if needed
                        }
                        is PortfolioUiState.Success -> {
                            binding.textTotalPortfolioValue.text = 
                                currencyFormatter.format(state.summary.totalValue)
                            
                            if (state.summary.lastUpdated > 0) {
                                binding.textLastUpdated.text = "Last Updated: ${dateFormatter.format(Date(state.summary.lastUpdated))}"
                                binding.textLastUpdated.visibility = View.VISIBLE
                            } else {
                                binding.textLastUpdated.visibility = View.GONE
                            }

                            bucketAdapter.submitList(state.summary.buckets)
                        }
                        is PortfolioUiState.Error -> {
                            Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                portfolioViewModel.snackbarMessage.collect { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showUpdateAllBucketsDialog(allocations: List<BucketAllocation>) {
        val dialogBinding = DialogUpdateAllBucketsBinding.inflate(layoutInflater)
        val tempBuckets = allocations.map { it.bucket.copy() }.toMutableList()
        
        fun updateTotal() {
            val total = tempBuckets.sumOf { it.targetPercentage }
            dialogBinding.textTotalPercentage.text = "Total: ${total.toInt()}%"
            if (total == 100.0) {
                dialogBinding.textTotalPercentage.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            } else {
                dialogBinding.textTotalPercentage.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            }
        }

        allocations.forEachIndexed { index, allocation ->
            val rowBinding = ItemDialogBucketRowBinding.inflate(layoutInflater, dialogBinding.containerBuckets, true)
            rowBinding.textBucketName.text = allocation.bucket.name
            rowBinding.textTargetPercentage.text = "${allocation.bucket.targetPercentage.toInt()}%"

            rowBinding.buttonIncrement.setOnClickListener {
                val currentBucket = tempBuckets[index]
                val nextValue = (currentBucket.targetPercentage + 5).coerceAtMost(100.0)
                tempBuckets[index] = currentBucket.copy(targetPercentage = nextValue)
                rowBinding.textTargetPercentage.text = "${nextValue.toInt()}%"
                updateTotal()
            }

            rowBinding.buttonDecrement.setOnClickListener {
                val currentBucket = tempBuckets[index]
                val nextValue = (currentBucket.targetPercentage - 5).coerceAtLeast(0.0)
                tempBuckets[index] = currentBucket.copy(targetPercentage = nextValue)
                rowBinding.textTargetPercentage.text = "${nextValue.toInt()}%"
                updateTotal()
            }
        }

        updateTotal()

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Update Bucket Allocations")
            .setView(dialogBinding.root)
            .setPositiveButton("Update", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val total = tempBuckets.sumOf { it.targetPercentage }
            if (total == 100.0) {
                portfolioViewModel.updateAllBucketAllocations(tempBuckets)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Total allocation must be 100%", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
