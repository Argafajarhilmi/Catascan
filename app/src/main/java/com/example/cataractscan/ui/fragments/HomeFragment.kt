package com.example.cataractscan.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cataractscan.R
import com.example.cataractscan.databinding.FragmentHomeBinding
import com.example.cataractscan.utils.PreferenceManager

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Set fullscreen flag
        requireActivity().window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferenceManager = PreferenceManager(requireContext())

        // Display user information
        binding.tvEmail.text = preferenceManager.getEmail()

        // Set welcome text
        binding.tvWelcome.text = "Selamat datang..."

        // Activate marquee
        binding.tvWelcome.isSelected = true
        binding.tvWelcome.requestFocus()  // Request focus for marquee to work

        // Setup card click listeners
        setupCardClickListeners()
    }

    private fun setupCardClickListeners() {
        binding.cardPengertian.setOnClickListener {
            findNavController().navigate(R.id.infoPengertianFragment)
        }

        binding.cardMature.setOnClickListener {
            findNavController().navigate(R.id.infoMatureFragment)
        }

        binding.cardImmature.setOnClickListener {
            findNavController().navigate(R.id.infoImmatureFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}