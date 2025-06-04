package com.example.closet;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

import java.util.List;

/**
 * Model class representing a clothing item.
 * Used with Firebase Firestore for data persistence.
 */
public class ClothingItem {

    private String id;
    private String name;
    private String category;
    private String fabric;
    private String fit;
    private String care;
    private List<String> images;
    private List<String> likedUsers;
    private List<String> sizes;

    private long views;
    private int likes;
    private Timestamp dateAdded;

    // This field is not stored in Firestore—it’s set locally based on the current user.
    private boolean likedByCurrentUser = false;

    /** Default constructor required for Firestore deserialization */
    public ClothingItem() { }

    /**
     * Convenience constructor (you can add/remove parameters as needed).
     */
    public ClothingItem(String name,
                        String category,
                        String fabric,
                        String fit,
                        String care,
                        List<String> images) {
        this.name = name;
        this.category = category;
        this.fabric = fabric;
        this.fit = fit;
        this.care = care;
        this.images = images;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @PropertyName("Name")
    public String getName() {
        return name;
    }

    @PropertyName("Name")
    public void setName(String name) {
        this.name = name;
    }

    @PropertyName("Category")
    public String getCategory() {
        return category;
    }

    @PropertyName("Category")
    public void setCategory(String category) {
        this.category = category;
    }

    @PropertyName("Fabric")
    public String getFabric() {
        return fabric;
    }

    @PropertyName("Fabric")
    public void setFabric(String fabric) {
        this.fabric = fabric;
    }

    @PropertyName("Fit")
    public String getFit() {
        return fit;
    }

    @PropertyName("Fit")
    public void setFit(String fit) {
        this.fit = fit;
    }

    @PropertyName("Care")
    public String getCare() {
        return care;
    }

    @PropertyName("Care")
    public void setCare(String care) {
        this.care = care;
    }

    @PropertyName("Images")
    public List<String> getImages() {
        return images;
    }

    @PropertyName("Images")
    public void setImages(List<String> images) {
        this.images = images;
    }

    @PropertyName("likedUsers")
    public List<String> getLikedUsers() {
        return likedUsers;
    }

    @PropertyName("likedUsers")
    public void setLikedUsers(List<String> likedUsers) {
        this.likedUsers = likedUsers;
    }

    @PropertyName("Sizes")
    public List<String> getSizes() {
        return sizes;
    }

    @PropertyName("Sizes")
    public void setSizes(List<String> sizes) {
        this.sizes = sizes;
    }

    @PropertyName("Views")
    public long getViews() {
        return views;
    }

    @PropertyName("Views")
    public void setViews(long views) {
        this.views = views;
    }

    @PropertyName("Likes")
    public int getLikes() {
        return likes;
    }

    @PropertyName("Likes")
    public void setLikes(int likes) {
        this.likes = likes;
    }

    @PropertyName("dateAdded")
    public Timestamp getDateAdded() {
        return dateAdded;
    }

    @PropertyName("dateAdded")
    public void setDateAdded(Timestamp dateAdded) {
        this.dateAdded = dateAdded;
    }

    public boolean isLikedByCurrentUser() {
        return likedByCurrentUser;
    }

    public void setLikedByCurrentUser(boolean likedByCurrentUser) {
        this.likedByCurrentUser = likedByCurrentUser;
    }

    @Override
    public String toString() {
        return "ClothingItem{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", fabric='" + fabric + '\'' +
                ", fit='" + fit + '\'' +
                ", care='" + care + '\'' +
                ", images=" + images +
                ", likedUsers=" + likedUsers +
                ", sizes=" + sizes +
                ", views=" + views +
                ", likes=" + likes +
                ", dateAdded=" + dateAdded +
                ", likedByCurrentUser=" + likedByCurrentUser +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ClothingItem that = (ClothingItem) obj;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
