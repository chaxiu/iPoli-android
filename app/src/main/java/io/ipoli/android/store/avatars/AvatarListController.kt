package io.ipoli.android.store.avatars

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import com.couchbase.lite.Database
import com.couchbase.lite.DatabaseConfiguration
import com.hannesdorfmann.adapterdelegates3.AdapterDelegate
import com.hannesdorfmann.adapterdelegates3.AdapterDelegatesManager
import com.hannesdorfmann.adapterdelegates3.ListDelegationAdapter
import com.jakewharton.rxbinding2.view.RxView
import io.ipoli.android.R
import io.ipoli.android.common.view.BaseController
import io.ipoli.android.player.persistence.CouchbasePlayerRepository
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.controller_avatar_list.view.*
import kotlinx.android.synthetic.main.item_avatar_store.view.*
import kotlinx.android.synthetic.main.view_error.view.*
import kotlinx.android.synthetic.main.view_loading.view.*

/**
 * Created by Polina Zhelyazkova <polina@ipoli.io>
 * on 8/20/17.
 */
class AvatarListController : BaseController<AvatarListController, AvatarListPresenter>() {

    lateinit private var avatarList: RecyclerView

    private var buySubject = PublishSubject.create<AvatarViewModel>()
    private var useSubject = PublishSubject.create<AvatarViewModel>()

    private lateinit var adapter: AvatarListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        val view = inflater.inflate(R.layout.controller_avatar_list, container, false)
        avatarList = view.avatarList
        avatarList.setHasFixedSize(true)
        avatarList.layoutManager = GridLayoutManager(view.context, 2)

        val delegatesManager = AdapterDelegatesManager<List<AvatarViewModel>>()
            .addDelegate(AvatarListController.AvatarAdapterDelegate(LayoutInflater.from(activity), buySubject, useSubject))
        adapter = AvatarListAdapter(delegatesManager)
        avatarList.adapter = adapter

        return view
    }

    override fun createPresenter(): AvatarListPresenter {
        return AvatarListPresenter(
            DisplayAvatarListUseCase(CouchbasePlayerRepository(Database("iPoli", DatabaseConfiguration(applicationContext)), job)),
            BuyAvatarUseCase(CouchbasePlayerRepository(Database("iPoli", DatabaseConfiguration(applicationContext)), job)),
            UseAvatarUseCase(CouchbasePlayerRepository(Database("iPoli", DatabaseConfiguration(applicationContext)), job))
        )
    }

    fun displayAvatarListIntent(): Observable<Boolean> =
        Observable.just(creatingState).filter { _ -> true }

    fun buyAvatarIntent(): Observable<AvatarViewModel> {
        return buySubject
    }

    fun useAvatarIntent(): Observable<AvatarViewModel> {
        return useSubject
    }

    fun render(state: AvatarListViewState) {
        val contentView = view!!

        contentView.loadingView.visibility = if (state.loading) View.VISIBLE else View.GONE
        contentView.errorView.visibility = if (state.error != null) View.VISIBLE else View.GONE
        contentView.avatarList.visibility = if (state.avatars != null) View.VISIBLE else View.GONE

        if (state.isDataNew) {
            adapter.items = state.avatars
            adapter.notifyDataSetChanged()
        }

        if (state.boughtAvatar != null) {
            val name = activity?.getString(state.boughtAvatar.name)
            Toast.makeText(activity, name + " successfully bought", Toast.LENGTH_SHORT).show()
        }

        if (state.usedAvatar != null) {
            val name = activity?.getString(state.usedAvatar.name)
            Toast.makeText(activity, name + " successfully used", Toast.LENGTH_SHORT).show()
        }

        if (state.avatarTooExpensive != null) {
            val name = activity?.getString(state.avatarTooExpensive.name)
            Toast.makeText(activity, name + " is too expensive", Toast.LENGTH_SHORT).show()
        }
    }

    class AvatarListAdapter(manager: AdapterDelegatesManager<List<AvatarViewModel>>) : ListDelegationAdapter<List<AvatarViewModel>>(
        manager)

    class AvatarAdapterDelegate(private val inflater: LayoutInflater,
                                private val buySubject: PublishSubject<AvatarViewModel>,
                                private val useSubject: PublishSubject<AvatarViewModel>) : AdapterDelegate<List<AvatarViewModel>>() {

        private var lastAnimatedPosition = -1

        private val colors = intArrayOf(R.color.md_green_300,
            R.color.md_indigo_300,
            R.color.md_blue_300,
            R.color.md_red_300,
            R.color.md_deep_orange_300,
            R.color.md_purple_300,
            R.color.md_orange_300,
            R.color.md_pink_300)

        override fun isForViewType(items: List<AvatarViewModel>, position: Int): Boolean = true

        override fun onBindViewHolder(items: List<AvatarViewModel>, position: Int, holder: RecyclerView.ViewHolder, payloads: MutableList<Any>) {
            val vh = holder as AvatarViewHolder
            val avatar = items[position]
            vh.bindAvatar(avatar, colors[position % colors.size])
            playEnterAnimation(holder.itemView, holder.getAdapterPosition())
        }

        override fun onCreateViewHolder(parent: ViewGroup?): RecyclerView.ViewHolder =
            AvatarViewHolder(inflater.inflate(R.layout.item_avatar_store, parent, false))

        private fun playEnterAnimation(viewToAnimate: View, position: Int) {
            if (position > lastAnimatedPosition) {
                val anim = AnimationUtils.loadAnimation(viewToAnimate.context, R.anim.fade_in)
                anim.startOffset = (position * 50).toLong()
                viewToAnimate.startAnimation(anim)
                lastAnimatedPosition = position
            }
        }

        inner class AvatarViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            fun bindAvatar(vm: AvatarViewModel, @ColorRes backgroundColor: Int) {
                with(vm) {
                    val context = itemView.context
                    val observable = RxView.clicks(itemView.avatarPrice).map { vm }
                    if (isBought) {
                        itemView.avatarPrice.setText(context.getString(R.string.avatar_store_use_avatar).toUpperCase())
                        itemView.avatarPrice.setIconResource(null as Drawable?)
                        observable.subscribe(useSubject)
                    } else {
                        itemView.avatarPrice.setText(price.toString())
                        itemView.avatarPrice.setIconResource(context.getDrawable(R.drawable.ic_life_coin_white_24dp))
                        observable.subscribe(buySubject)
                    }
                    itemView.avatarName.text = context.getString(name)
                    itemView.avatarPicture.setImageResource(picture)
                    itemView.setBackgroundColor(ContextCompat.getColor(context, backgroundColor))
                }
            }
        }

    }
}