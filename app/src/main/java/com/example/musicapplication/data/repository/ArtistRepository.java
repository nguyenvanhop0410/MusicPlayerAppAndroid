package com.example.musicapplication.data.repository;

import android.content.Context;

import com.example.musicapplication.model.Artist;
import com.example.musicapplication.utils.Logger;
import com.example.musicapplication.utils.NetworkUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ArtistRepository {
    private FirebaseFirestore firestore;
    private Context context;

    public ArtistRepository(Context context) {
        this.context = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public interface OnArtistsLoadedListener {
        void onSuccess(List<Artist> artists);
        void onError(Exception error);
    }

    public interface OnArtistLoadedListener {
        void onSuccess(Artist artist);
        void onError(Exception error);
    }

    // Get popular artists (sorted by followers)
    public void getPopularArtists(int limit, OnArtistsLoadedListener listener) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            listener.onError(new Exception("Không có kết nối mạng"));
            return;
        }
        firestore.collection("artists")
                .orderBy("followers", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Artist> artists = new ArrayList<>();
                    int totalArtists = querySnapshot.size();
                    
                    if (totalArtists == 0) {
                        listener.onSuccess(artists);
                        return;
                    }
                    
                    // Counter to track completion
                    final int[] completed = {0};
                    final boolean[] hasReturned = {false};
                    
                    // Timeout protection: return results after 10 seconds even if not all counts completed
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (!hasReturned[0]) {
                            hasReturned[0] = true;
                            Logger.w("Timeout waiting for artist song counts, returning " + artists.size() + " artists");
                            listener.onSuccess(artists);
                        }
                    }, 10000);
                    
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        try {
                            Artist artist = document.toObject(Artist.class);
                            artist.setId(document.getId());
                            
                            // Validate artist has required fields
                            if (artist.getName() == null || artist.getName().isEmpty()) {
                                Logger.w("Artist document missing name field: " + document.getId());
                                completed[0]++;
                                if (completed[0] == totalArtists && !hasReturned[0]) {
                                    hasReturned[0] = true;
                                    listener.onSuccess(artists);
                                }
                                continue;
                            }
                            
                            // Count songs for this artist
                            countSongsForArtist(artist.getName(), songCount -> {
                                if (!hasReturned[0]) {
                                    artist.setSongCount(songCount);
                                    artists.add(artist);
                                    completed[0]++;
                                    
                                    // When all artists are processed, return the list
                                    if (completed[0] == totalArtists) {
                                        hasReturned[0] = true;
                                        Logger.d("Loaded " + artists.size() + " popular artists with song counts");
                                        listener.onSuccess(artists);
                                    }
                                }
                            });
                        } catch (Exception e) {
                            Logger.e("Error parsing artist document: " + document.getId(), e);
                            completed[0]++;
                            if (completed[0] == totalArtists && !hasReturned[0]) {
                                hasReturned[0] = true;
                                listener.onSuccess(artists);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Logger.e("Error loading popular artists", e);
                    listener.onError(e);
                });
    }

    // Get all artists
    public void getAllArtists(OnArtistsLoadedListener listener) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            listener.onError(new Exception("Không có kết nối mạng"));
            return;
        }
        firestore.collection("artists")
                .orderBy("name")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Artist> artists = new ArrayList<>();
                    int totalArtists = querySnapshot.size();
                    
                    if (totalArtists == 0) {
                        listener.onSuccess(artists);
                        return;
                    }
                    
                    // Counter to track completion
                    final int[] completed = {0};
                    final boolean[] hasReturned = {false};
                    
                    // Timeout protection
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (!hasReturned[0]) {
                            hasReturned[0] = true;
                            Logger.w("Timeout waiting for artist song counts, returning " + artists.size() + " artists");
                            listener.onSuccess(artists);
                        }
                    }, 10000);
                    
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        try {
                            Artist artist = document.toObject(Artist.class);
                            artist.setId(document.getId());
                            
                            // Validate artist has required fields
                            if (artist.getName() == null || artist.getName().isEmpty()) {
                                Logger.w("Artist document missing name field: " + document.getId());
                                completed[0]++;
                                if (completed[0] == totalArtists && !hasReturned[0]) {
                                    hasReturned[0] = true;
                                    listener.onSuccess(artists);
                                }
                                continue;
                            }
                            
                            // Count songs for this artist
                            countSongsForArtist(artist.getName(), songCount -> {
                                if (!hasReturned[0]) {
                                    artist.setSongCount(songCount);
                                    artists.add(artist);
                                    completed[0]++;
                                    
                                    // When all artists are processed, return the list
                                    if (completed[0] == totalArtists) {
                                        hasReturned[0] = true;
                                        Logger.d("Loaded " + artists.size() + " artists with song counts");
                                        listener.onSuccess(artists);
                                    }
                                }
                            });
                        } catch (Exception e) {
                            Logger.e("Error parsing artist document: " + document.getId(), e);
                            completed[0]++;
                            if (completed[0] == totalArtists && !hasReturned[0]) {
                                hasReturned[0] = true;
                                listener.onSuccess(artists);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Logger.e("Error loading artists", e);
                    listener.onError(e);
                });
    }

    // Get artist by ID
    public void getArtistById(String artistId, OnArtistLoadedListener listener) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            listener.onError(new Exception("Không có kết nối mạng"));
            return;
        }
        firestore.collection("artists")
                .document(artistId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Artist artist = documentSnapshot.toObject(Artist.class);
                        if (artist != null) {
                            artist.setId(documentSnapshot.getId());
                            
                            // Count songs for this artist
                            countSongsForArtist(artist.getName(), songCount -> {
                                artist.setSongCount(songCount);
                                listener.onSuccess(artist);
                            });
                        } else {
                            listener.onError(new Exception("Artist data is null"));
                        }
                    } else {
                        listener.onError(new Exception("Artist not found"));
                    }
                })
                .addOnFailureListener(e -> {
                    Logger.e("Error loading artist", e);
                    listener.onError(e);
                });
    }

    // Search artists by name
    public void searchArtists(String query, OnArtistsLoadedListener listener) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            listener.onError(new Exception("Không có kết nối mạng"));
            return;
        }
        firestore.collection("artists")
                .orderBy("name")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Artist> artists = new ArrayList<>();
                    int totalArtists = querySnapshot.size();
                    
                    if (totalArtists == 0) {
                        listener.onSuccess(artists);
                        return;
                    }
                    
                    // Counter to track completion
                    final int[] completed = {0};
                    
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Artist artist = document.toObject(Artist.class);
                        artist.setId(document.getId());
                        
                        // Count songs for this artist
                        countSongsForArtist(artist.getName(), songCount -> {
                            artist.setSongCount(songCount);
                            artists.add(artist);
                            completed[0]++;
                            
                            // When all artists are processed, return the list
                            if (completed[0] == totalArtists) {
                                Logger.d("Found " + artists.size() + " artists matching: " + query);
                                listener.onSuccess(artists);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Logger.e("Error searching artists", e);
                    listener.onError(e);
                });
    }

    // Helper method to count songs for a specific artist
    private void countSongsForArtist(String artistName, OnSongCountListener listener) {
        if (artistName == null || artistName.isEmpty()) {
            Logger.w("countSongsForArtist called with null/empty artist name");
            listener.onCountComplete(0);
            return;
        }
        
        firestore.collection("songs")
                .whereEqualTo("artist", artistName)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();
                    Logger.d("Artist '" + artistName + "' has " + count + " songs");
                    listener.onCountComplete(count);
                })
                .addOnFailureListener(e -> {
                    Logger.e("Error counting songs for artist: " + artistName, e);
                    // Return 0 on error instead of failing the entire operation
                    listener.onCountComplete(0);
                });
    }

    // Callback interface for song count
    private interface OnSongCountListener {
        void onCountComplete(int count);
    }
}
