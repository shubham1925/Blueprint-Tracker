package com.example.blueprinttracker

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blueprinttracker.databinding.FragmentOverviewBinding
import com.example.blueprinttracker.ui.adapter.BucketAdapter
import com.example.blueprinttracker.ui.viewmodel.PortfolioUiState
import com.example.blueprinttracker.ui.viewmodel.PortfolioViewModel
import com.example.blueprinttracker.ui.viewmodel.PortfolioViewModelFactory
import kotlinx.coroutines.launch
import java.text.NumberFormat
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
            Toast.makeText(requireContext(), "Update Buckets coming soon!", Toast.LENGTH_SHORT).show()
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
                            bucketAdapter.submitList(state.summary.buckets)
                        }
                        is PortfolioUiState.Error -> {
                            Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
