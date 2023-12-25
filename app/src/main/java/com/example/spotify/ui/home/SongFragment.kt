package com.example.spotify.ui.home

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.spotify.adapter.Contributor
import com.example.spotify.adapter.Song
import com.example.spotify.adapter.SongAdapter
import com.example.spotify.api.ApiConfig
import com.example.spotify.api.ContributorsItem
import com.example.spotify.api.TopSongsResponse
import com.example.spotify.database.FavouriteSong
import com.example.spotify.database.FavouriteSongDao

import com.example.spotify.databinding.FragmentSongBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class SongFragment : Fragment(),SongAdapter.OnFavoriteClickListener {
    // TODO: Rename and change types of parameters
    private lateinit var binding : FragmentSongBinding
    private lateinit var songRv: RecyclerView
    private lateinit var songAdapter: SongAdapter
    private lateinit var favouriteSongDao: FavouriteSongDao
    private val listSong = ArrayList<Song>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSongBinding.inflate(inflater, container, false)

        // Retrieve arguments
        val artistName = arguments?.getString("name")?:""
        val artistId = arguments?.getInt("artistId", 0) ?: 0
        val photoUrlBig = arguments?.getString("photoUrlBig") ?: ""

        loadImage(photoUrlBig)

        binding.singerName.text = artistName

        // Initialize RecyclerView
        songRv = binding.songRv
        songRv.setHasFixedSize(true)

        // Set up RecyclerView layout manager
        val layoutManager = GridLayoutManager(requireContext(), 1)

        songRv.layoutManager = layoutManager

        // Set up RecyclerView adapter
        val listSong = ArrayList<Song>()
        songAdapter = SongAdapter(listSong).apply {
            setOnFavoriteClickListener(this@SongFragment)
        }
        songAdapter = SongAdapter(listSong)

        songRv.adapter = songAdapter

        fetchDataFromApi(artistId)

        binding.backButton.setOnClickListener{
            findNavController().navigateUp()
        }

        // Add an OnBackPressedCallback to handle system back button press
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigateUp()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)


        return binding.root
    }


    // In the ImageView extension function
    private fun loadImage(url: String) {
        Glide.with(this)
            .load(url)
            .into(binding.imageView2)
    }

    private fun fetchDataFromApi(id: Int){

        binding.loadingProgressBar.visibility = View.VISIBLE

        val apiService = ApiConfig.getApiService()
        val call = apiService.getTop10Songs(id)

        call.enqueue(object : Callback<TopSongsResponse> {
            override fun onResponse(call: Call<TopSongsResponse>, response: Response<TopSongsResponse>) {
                if (response.isSuccessful) {
                    val topSongsResponse = response.body()
                    topSongsResponse?.let {
                        val dataItems = it.data

                        // Map dataItems to Song list
                        val listSong = dataItems?.map { dataItem ->
                            Song(
                                name = dataItem?.artist?.name.orEmpty(),
                                albumCover = dataItem?.album?.coverSmall.orEmpty(),
                                songName = dataItem?.title.orEmpty(),
                                contributors = mapContributors(dataItem?.contributors)
                            )
                        } ?: emptyList()
                        updateAdapter(listSong)
                    }
                } else {
                    // Handle API error
                    Log.e("SongFragment", "API call failed: ${response.message()}")
                }

                binding.loadingProgressBar.visibility = View.GONE

            }
            override fun onFailure(call: Call<TopSongsResponse>, t: Throwable) {
                // Handle network failure
                 Log.e("SongFragment", "Network call failed: ${t.message}")
                binding.loadingProgressBar.visibility = View.GONE

            }
        })

    }

    private fun updateAdapter(listSong: List<Song>) {
        Log.d("SongFragment", "Received ${listSong.size} items from API")

        songAdapter.setData(listSong)
        songAdapter.notifyDataSetChanged()
    }

    private fun mapContributors(contributors: List<ContributorsItem?>?): List<Contributor> {
        return contributors?.mapNotNull { contributor ->
            Contributor(name = contributor?.name.orEmpty())
        } ?: emptyList()
    }

    override fun onFavoriteClick(position: Int) {
        // Handle favorite button click here
        val song = listSong[position]
        song.isFavorite = !song.isFavorite
        songAdapter.notifyItemChanged(position)

        saveToFavoriteDatabase(song)

    }

    private fun saveToFavoriteDatabase(song: Song) {
        // Convert contributors List to a comma-separated string for simplicity
        val contributors = song.contributors.joinToString(", ")

        // Create a FavouriteSong entity
        val favouriteSong = FavouriteSong(
            songName = song.songName,
            albumCover = song.albumCover,
            contributors = contributors,
            favourite = song.isFavorite
        )

        // Save to the FavouriteSong database using FavouriteSongDao
        saveToFavoriteDatabase(favouriteSong)

    }

    private fun saveToFavoriteDatabase(favouriteSong: FavouriteSong) {
        // Use favouriteSongDao to perform the insert operation
        favouriteSongDao.insert(favouriteSong)
    }



}

