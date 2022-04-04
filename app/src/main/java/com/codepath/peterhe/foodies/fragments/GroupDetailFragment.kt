package com.codepath.peterhe.foodies.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codepath.peterhe.foodies.Group
import com.codepath.peterhe.foodies.MemberAdapter
import com.codepath.peterhe.foodies.R
import com.parse.FindCallback
import com.parse.ParseException
import com.parse.ParseQuery
import com.parse.ParseUser
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator

class GroupDetailFragment : Fragment() {
    private lateinit var group: Group
    private lateinit var Founder:ParseUser
    //var memberIds:MutableList<String> = mutableListOf()
    var allMembers: MutableList<ParseUser> = mutableListOf()
    private lateinit var memberAdapter: MemberAdapter
    lateinit var membersRecyclerView: RecyclerView
    lateinit var layoutManager: LinearLayoutManager
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_group_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(getResources().getColor(R.color.white))
        val bundle = this.arguments
        if(bundle != null){
            group = bundle.getParcelable<Group>("GroupDetail")!!
        }
        membersRecyclerView = view.findViewById(R.id.rv_Members)
        memberAdapter = MemberAdapter(requireContext(),allMembers)
        membersRecyclerView.adapter = memberAdapter
        layoutManager = LinearLayoutManager(requireContext())
        membersRecyclerView.layoutManager = layoutManager
        membersRecyclerView.itemAnimator = SlideInUpAnimator()
        val itemDecoration: RecyclerView.ItemDecoration =
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        membersRecyclerView.addItemDecoration(itemDecoration)
        val ft:FragmentTransaction? = getFragmentManager()?.beginTransaction()
        memberAdapter.setOnItemClickListner(object: MemberAdapter.onItemClickListner{
            override fun onItemClick(position: Int) {
                //final FragmentTransaction ft = getFragmentManager().beginTransaction();
                val bundle = Bundle()
                bundle.putParcelable("MemberDetail", allMembers[position])
                /*val DetailFragment = GroupDetailFragment()
                DetailFragment.setArguments(bundle)
                //Log.i(RestaurantFragment.TAG, "Restaurant ${allGroups[position]}")
                ft?.replace(R.id.flContainer, DetailFragment)?.commit()
                requireActivity().setTitle("${allMembers[position].getName()}")
                ft?.addToBackStack(null)*/
            }
        })

        queryFounder()
        view.findViewById<TextView>(R.id.tv_GroupName_Detail).text = group.getName()
        view.findViewById<TextView>(R.id.tv_GroupDescription_detail).text = group.getDescription()
        val num = "${group.getCurrent()}/${group.getMax()}"
        view.findViewById<TextView>(R.id.tv_GroupNumber_detail).text = num
        val time = "${group.getTime()} ${group.getDate()}"
        view.findViewById<TextView>(R.id.tv_GroupTime_detail).text = time
        view.findViewById<ImageButton>(R.id.btn_cancel_groupDetail).setOnClickListener {
            val fm = getFragmentManager()
            fm?.popBackStack()
        }
        view.findViewById<ImageButton>(R.id.btn_join_GroupDetail).setOnClickListener {
            submitGroupUpdate(ParseUser.getCurrentUser().objectId)
        }

        var Userjoined = false
        for (i in 0 until group.getMemberList()!!.length()) {
            val item:String = group.getMemberList()?.getString(i)!!
            if (item.trim() == (ParseUser.getCurrentUser().objectId.trim())) {
                Userjoined =true
            }
            queryMember(item)
        }
        requireActivity().setTitle("${group.getName()}")
        if (Userjoined == true) {
            view?.findViewById<ImageButton>(R.id.btn_join_GroupDetail)?.setEnabled(false)
            view?.findViewById<ImageButton>(R.id.btn_join_GroupDetail)?.setVisibility(View.GONE)
            view?.findViewById<ImageButton>(R.id.btn_cancel_groupDetail)?.setEnabled(false)
            view?.findViewById<ImageButton>(R.id.btn_cancel_groupDetail)?.setVisibility(View.GONE)
            requireActivity().setTitle("${group.getName()} (Already Joined)")
        }


    }

    private fun submitGroupUpdate(
        userId: String,
    ) {
        var memberList = group.getMemberList()
        memberList!!.put(userId)
        group.setMemberList(memberList)
        var cur = group.getCurrent()
        cur = cur!! + 1
        group.setCurrent(cur)
        var full: Boolean = false
        if (cur == group.getMax()) {
            full = true
        }
        group.setFull(full)
        val ft: FragmentTransaction? = getFragmentManager()?.beginTransaction()
        group.saveInBackground { exception ->
            if (exception != null) {
                //Log.e(TAG, "ERROR submitting a post")
                exception.printStackTrace()
                Toast.makeText(requireContext(), "Error joining a group", Toast.LENGTH_SHORT).show()
            } else {
                //Log.i(TAG, "Successfully submitted a post")
                Toast.makeText(requireContext(), "Successfully joined a group", Toast.LENGTH_SHORT).show()
                view?.findViewById<ImageButton>(R.id.btn_join_GroupDetail)?.setEnabled(false)
                view?.findViewById<ImageButton>(R.id.btn_join_GroupDetail)?.setVisibility(View.GONE)
                view?.findViewById<ImageButton>(R.id.btn_cancel_groupDetail)?.setEnabled(false)
                view?.findViewById<ImageButton>(R.id.btn_cancel_groupDetail)?.setVisibility(View.GONE)
                requireActivity().setTitle("${group.getName()} (Already Joined)")
                allMembers.add(ParseUser.getCurrentUser())
                memberAdapter.notifyDataSetChanged()
                //ft.detach(this).attach(this).commit()

                //val fm = getFragmentManager()
                // fm?.popBackStack()
                /*val bundle = Bundle()
                bundle.putParcelable("RestaurantDetail", restaurant)
                val DetailFragment = RestaurantDetailFragment()
                DetailFragment.setArguments(bundle)
                // Log.i(RestaurantFragment.TAG, "Restaurant ${restaurants[position]}")
                ft?.replace(R.id.flContainer, DetailFragment)?.commit()
                requireActivity().setTitle("${restaurant.name}")
                ft?.addToBackStack(null)*/
            }
        }
    }

    fun queryFounder() {
        val query: ParseQuery<ParseUser> = ParseQuery.getQuery(ParseUser::class.java)
        query.include(ParseUser.KEY_OBJECT_ID)
        query.addDescendingOrder("createdAt")
        query.limit = 1
        //query.skip = offset * 20
        query.whereEqualTo(ParseUser.KEY_OBJECT_ID, group.getFounder()?.objectId)
        query.findInBackground(object : FindCallback<ParseUser> {
            override fun done(founders: MutableList<ParseUser>?, e: ParseException?) {
                if (e != null) {
                    //Log.e(TAG, "Error getting posts")
                    Toast.makeText(requireContext(), "Error getting groups", Toast.LENGTH_SHORT).show()
                } else {
                    if (founders != null) {
                        Founder = founders[0]
                        view?.findViewById<TextView>(R.id.tv_FounderName_detail)?.text = Founder.get("username").toString()
                        view?.findViewById<TextView>(R.id.tv_FounderDescription_detail)?.text = Founder.get("description").toString()
                        //TODO view?.findViewById<ImageView>(R.id.tv_FounderProfile_detail)?. = Founder.get("username").toString()
                        val time = "${Founder.getCreatedAt()}".substring(4,10)
                        view?.findViewById<TextView>(R.id.tv_FounderJoined_GroupDetail)?.text = time
                    }
                }
            }

        })
    }

    fun queryMember(memberId:String) {
        val query: ParseQuery<ParseUser> = ParseQuery.getQuery(ParseUser::class.java)
        query.include(ParseUser.KEY_OBJECT_ID)
        //query.addDescendingOrder("createdAt")
        query.limit = 1
        //query.skip = offset * 20
        query.whereEqualTo(ParseUser.KEY_OBJECT_ID, memberId)
        query.findInBackground(object : FindCallback<ParseUser> {
            override fun done(members: MutableList<ParseUser>?, e: ParseException?) {
                if (e != null) {
                    //Log.e(TAG, "Error getting posts")
                    Toast.makeText(requireContext(), "Error getting members", Toast.LENGTH_SHORT).show()
                } else {
                    if (members != null) {
                        allMembers.addAll(members)
                        memberAdapter.notifyDataSetChanged()
                    }
                }
            }

        })
    }
}