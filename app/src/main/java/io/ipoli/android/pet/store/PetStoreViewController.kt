package io.ipoli.android.pet.store

import android.os.Bundle
import android.support.v4.view.PagerAdapter
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import io.ipoli.android.R
import io.ipoli.android.common.ViewUtils
import io.ipoli.android.common.mvi.MviViewController
import io.ipoli.android.common.view.showBackButton
import io.ipoli.android.common.view.stringRes
import io.ipoli.android.common.view.visible
import io.ipoli.android.pet.AndroidPetAvatar
import io.ipoli.android.pet.PetAvatar
import io.ipoli.android.pet.PetMood
import io.ipoli.android.pet.store.PetStoreViewState.StateType.*
import kotlinx.android.synthetic.main.controller_pet_store.view.*
import kotlinx.android.synthetic.main.controller_store_toolbar.view.*
import kotlinx.android.synthetic.main.item_pet_store.view.*
import space.traversal.kapsule.required

/**
 * Created by Polina Zhelyazkova <polina@ipoli.io>
 * on 12/4/17.
 */
class PetStoreViewController(args: Bundle? = null) : MviViewController<PetStoreViewState, PetStoreViewController, PetStorePresenter, PetStoreIntent>(args) {
    private val presenter by required { petStorePresenter }

    override fun createPresenter() = presenter

    private lateinit var storeToolbar: ViewGroup

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        setHasOptionsMenu(true)
        val view = inflater.inflate(R.layout.controller_pet_store, container, false)

        val toolbar = activity!!.findViewById<Toolbar>(R.id.toolbar)
        storeToolbar = inflater.inflate(R.layout.controller_store_toolbar, toolbar, false) as ViewGroup
        toolbar.addView(storeToolbar)
        storeToolbar.storeTitle.setText(R.string.store)

        view.petPager.clipToPadding = false
        view.petPager.pageMargin = ViewUtils.dpToPx(32f, view.context).toInt()
        return view
    }

    override fun onAttach(view: View) {
        showBackButton()
        super.onAttach(view)
        send(LoadDataIntent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            router.popCurrentController()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun render(state: PetStoreViewState, view: View) {
        when (state.type) {
            DATA_LOADED -> {
                view.petPager.adapter = PetPagerAdapter(listOf())
            }

            PLAYER_CHANGED -> {
                storeToolbar.playerCoins.text = state.playerCoins.toString()
                (view.petPager.adapter as PetPagerAdapter).updateAll(state.petViewModels)
            }

            PET_TOO_EXPENSIVE -> {
                Toast.makeText(view.context, "Pet too expensive", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView(view: View) {
        val toolbar = activity!!.findViewById<Toolbar>(R.id.toolbar)
        toolbar.removeView(storeToolbar)

        super.onDestroyView(view)
    }

    data class PetViewModel(val avatar: AndroidPetAvatar, val isBought: Boolean = false, val isCurrent: Boolean = false)

    inner class PetPagerAdapter(private var viewModels: List<PetViewModel>) : PagerAdapter() {

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val inflater = LayoutInflater.from(container.context)
            val view = inflater.inflate(R.layout.item_pet_store, container, false)
            val vm = viewModels[position]
            val avatar = vm.avatar
            view.petName.setText(avatar.petName)
            view.pet.setImageResource(avatar.image)
            view.petPrice.text = PetAvatar.valueOf(avatar.name).price.toString()
            view.petDescription.setText(avatar.description)
            val action = view.petAction
            val current = view.currentPet
            when {
                vm.isCurrent -> {
                    action.visible = false
                    current.visible = true
                    view.petState.setImageResource(avatar.moodImage[PetMood.HAPPY]!!)
                }
                vm.isBought -> {
                    action.visible = true
                    current.visible = false
                    action.text = stringRes(R.string.store_pet_in_inventory)
                    action.setOnClickListener {
                        send(ChangePetIntent(PetAvatar.valueOf(vm.avatar.name)))
                    }
                    view.petState.setImageResource(avatar.moodImage[PetMood.GOOD]!!)
                }
                else -> {
                    action.visible = true
                    current.visible = false
                    action.text = stringRes(R.string.store_buy_pet)
                    action.setOnClickListener {
                        send(BuyPetIntent(PetAvatar.valueOf(vm.avatar.name)))
                    }
                    view.petState.setImageResource(avatar.moodImage[PetMood.GOOD]!!)
                }
            }
            container.addView(view)
            return view
        }

        override fun destroyItem(container: ViewGroup, position: Int, view: Any) {
            container.removeView(view as View)
        }

        override fun isViewFromObject(view: View, `object`: Any) = view == `object`

        override fun getCount() = viewModels.size

        override fun getItemPosition(`object`: Any) = PagerAdapter.POSITION_NONE

        fun updateAll(viewModels: List<PetViewModel>) {
            this.viewModels = viewModels
            notifyDataSetChanged()
        }

    }
}