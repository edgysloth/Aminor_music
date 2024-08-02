package com.example.aminor_music

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.aminor_music.ui.theme.Aminor_musicTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class MainActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var isPaused = false
    private var currentTrackIndex = -1
    private var currentTrackList: List<Data> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Aminor_musicTheme {
                // Set background color to black
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    val data = remember { mutableStateOf<MyData?>(null) }

                    // Make the network call
                    fetchData { fetchedData ->
                        data.value = fetchedData
                        currentTrackList = fetchedData?.data ?: emptyList()
                    }

                    // Pass the data to the Composable function
                    data.value?.let {
                        DisplayData(
                            it,
                            onPlayPauseButtonClick = { index -> playPauseMusic(index) },
                            onNextButtonClick = { nextTrack() },
                            onPreviousButtonClick = { previousTrack() }
                        )
                    } ?: Text("Loading...", color = Color.White)
                }
            }
        }
    }

    private fun fetchData(callback: (MyData?) -> Unit) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://deezerdevs-deezer.p.rapidapi.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ApiInterface::class.java)

        service.getData("eminem").enqueue(object : Callback<MyData?> {
            override fun onResponse(call: Call<MyData?>, response: Response<MyData?>) {
                if (response.isSuccessful) {
                    callback(response.body())
                } else {
                    callback(null)
                }
            }

            override fun onFailure(call: Call<MyData?>, t: Throwable) {
                callback(null)
            }
        })
    }

    private fun playPauseMusic(index: Int) {
        if (currentTrackIndex == index && mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            isPaused = true
        } else {
            if (currentTrackIndex != index) {
                currentTrackIndex = index
                isPaused = false
                playMusic(currentTrackList[currentTrackIndex].preview)
            } else {
                mediaPlayer?.start()
                isPaused = false
            }
        }
    }

    private fun playMusic(url: String) {
        mediaPlayer?.release() // Release any previously playing MediaPlayer
        val myUri: Uri = Uri.parse(url)
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(applicationContext, myUri)
            prepare()
            start()
            setOnCompletionListener {
                nextTrack()
            }
        }
    }

    private fun nextTrack() {
        if (currentTrackList.isNotEmpty()) {
            currentTrackIndex = (currentTrackIndex + 1) % currentTrackList.size
            playMusic(currentTrackList[currentTrackIndex].preview)
        }
    }

    private fun previousTrack() {
        if (currentTrackList.isNotEmpty()) {
            currentTrackIndex = if (currentTrackIndex > 0) currentTrackIndex - 1 else currentTrackList.size - 1
            playMusic(currentTrackList[currentTrackIndex].preview)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}

@Composable
fun DisplayData(
    data: MyData,
    onPlayPauseButtonClick: (Int) -> Unit,
    onNextButtonClick: () -> Unit,
    onPreviousButtonClick: () -> Unit
) {
    LazyColumn {
        item {
            Text(
                text = "Library",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        items(data.data) { item ->
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                border = BorderStroke(1.dp, Color.White) // White outline for the card
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(item.album.cover_big),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Title: ${item.title}", color = Color.White)
                    Text("Album: ${item.album.title}", color = Color.White)
                    Text("Artist: ${item.artist.name}", color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(
                            onClick = onPreviousButtonClick,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_previous),
                                contentDescription = "Previous",
                                tint = Color.Gray
                            )
                        }
                        val isPlaying = remember { mutableStateOf(false) }
                        val animatedIconAlpha by animateFloatAsState(
                            targetValue = if (isPlaying.value) 1f else 0.5f,
                            animationSpec = tween(durationMillis = 300)
                        )
                        IconButton(
                            onClick = {
                                isPlaying.value = !isPlaying.value
                                onPlayPauseButtonClick(data.data.indexOf(item))
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying.value) ImageVector.vectorResource(id = R.drawable.ic_pause) else ImageVector.vectorResource(id = R.drawable.ic_play_pause),
                                contentDescription = if (isPlaying.value) "Pause" else "Play",
                                tint = Color.Green.copy(alpha = animatedIconAlpha)
                            )
                        }
                        IconButton(
                            onClick = onNextButtonClick,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_next),
                                contentDescription = "Next",
                                tint = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}
