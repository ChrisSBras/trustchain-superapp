package nl.tudelft.trustchain.valuetransfer.ui.identity

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.flow.map
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.copyToClipboard
import nl.tudelft.trustchain.common.util.mapToJSON
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.community.IdentityCommunity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentIdentityBinding
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import nl.tudelft.trustchain.valuetransfer.dialogs.IdentityDetailsDialog
import nl.tudelft.trustchain.valuetransfer.dialogs.QRCodeDialog
import nl.tudelft.trustchain.valuetransfer.entity.Identity

class IdentityFragment : BaseFragment(R.layout.fragment_identity) {

    private val binding by viewBinding(FragmentIdentityBinding::bind)
    private val adapterPersonal = ItemAdapter()

    private val itemsPersonal: LiveData<List<Item>> by lazy {
        store.getAllPersonalIdentities().map { identities ->
            createItems(identities)
        }.asLiveData()
    }

    private val store by lazy {
        IdentityStore.getInstance(requireContext())
    }

    private fun getCommunity(): IdentityCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("IdentityCommunity is not configured")
    }

    init {
        setHasOptionsMenu(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapterPersonal.registerRenderer(
            IdentityItemRenderer(
                1,
                 { identity ->
                    val map = mapOf(
                        "public_key" to identity.publicKey.keyToBin().toHex(),
                        "message" to "TEST"
                    )
                    QRCodeDialog("Personal Public Key", "Show QR-code to other party", mapToJSON(map).toString())
                        .show(parentFragmentManager, tag)
                }, { identity ->
                    copyToClipboard(requireContext(), identity.publicKey.keyToBin().toHex(), "Public Key")
                }
            )
        )

        itemsPersonal.observe(
            this,
            Observer {
                adapterPersonal.updateItems(it)

                if (store.hasPersonalIdentity()) {
                    binding.tvNoPersonalIdentity.visibility = View.GONE
                } else {
                    binding.tvNoPersonalIdentity.visibility = View.VISIBLE
                }
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bnvIdentity)
        val navController = requireActivity().findNavController(R.id.navHostFragment)
        bottomNavigationView.setupWithNavController(navController)

//        when(store.hasPersonalIdentity()) {
//            true -> {
//                val identity = store.getPersonalIdentity()
//
//
//
//                binding.tvPublicKeyValue.setOnClickListener {
//
//                }
//
//                binding.btnPersonalQRCode.setOnClickListener {
//                    val map = mapOf(
//                        "public_key" to identity.publicKey.keyToBin().toHex(),
//                        "message" to "TEST"
//                    )
//
//                    QRCodeDialog("Personal Public Key", "Show QR-code to other party", map)
//                        .show(parentFragmentManager, tag)
//                }
//
//                binding.btnPersonalCopyPublicKey.setOnClickListener {
//                    addToClipboard(identity.publicKey.keyToBin().toHex(), "Public Key")
//                    Toast.makeText(
//                        this.context,
//                        "Public key copied to clipboard",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//        }

        binding.tvNoPersonalIdentity.setOnClickListener {
            IdentityDetailsDialog(null, getCommunity()).show(parentFragmentManager, tag)
        }

        binding.rvPersonalIdentities.adapter = adapterPersonal
        binding.rvPersonalIdentities.layoutManager = LinearLayoutManager(context)
//        binding.rvPersonalIdentities.addItemDecoration(
//            DividerItemDecoration(
//                context,
//                LinearLayout.VERTICAL
//            )
//        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.identity_options, menu)

        menu.getItem(0).isVisible = !store.hasPersonalIdentity()
        menu.getItem(1).isVisible = store.hasPersonalIdentity()
        menu.getItem(2).isVisible = store.hasPersonalIdentity()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.actionAddPersonalIdentity -> IdentityDetailsDialog(null, getCommunity()).show(parentFragmentManager, tag)
            R.id.actionEditPersonalIdentity -> {
                IdentityDetailsDialog(store.getPersonalIdentity(), getCommunity()).show(parentFragmentManager, tag)
            }
            R.id.actionRemovePersonalIdentity -> {
                Toast.makeText(this.context, "Removed personal identity", Toast.LENGTH_SHORT).show()
                store.deleteIdentity(store.getPersonalIdentity())
                activity?.invalidateOptionsMenu()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun createItems(identities: List<Identity>): List<Item> {
        return identities.mapIndexed { _, identity ->
            IdentityItem(
                identity
            )
        }
    }
}
